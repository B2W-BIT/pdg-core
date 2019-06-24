(ns restql.core.runner.request
  (:require [clojure.core.async :refer [chan go >! close!]]
            [restql.log :as log]
            [restql.parser.json :as json]
            [httpurr.client :as http]
            [httpurr.client.node :refer [client]]
            [environ.core :refer [env]]
            [restql.hooks.core :as hook]
            [promesa.core :as p]))

(defn- fmap [f m]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn- decode-url [string]
  (try
    (js/decodeURI string)
    (catch Exception e
      string)))

(defn- parse-query-params
  "this function takes a request object (with :url and :query-params)
  and transforms query params that are sets into vectors"
  [request]
  (update-in request [:query-params]
             #(fmap (fn [query-param-value]
                      (if (or (sequential? query-param-value) (set? query-param-value))
                        (->> query-param-value (map decode-url) (into []))
                        (decode-url query-param-value))) %)))

(defn- get-forward-params [query-opts]
  (-> query-opts
      (some-> :forward-params)
      (as-> forward-params (if (nil? forward-params) {} forward-params))))

(defn- get-query-params [request]
  (-> request
      (some-> :query-params)
      (as-> query-params (if (nil? query-params) {} query-params))))

(defn- valid-query-params [request query-opts]
  (->> (get-query-params request)
       (merge (get-forward-params query-opts))
       (filter (fn [[_ v]] (some? v)))
       (into {})))

(defn- convert-response [{:keys [status body headers]} {:keys [_debugging metadata response-time url params timeout resource]}]
  (let [parsed body
        base {:status        status
              :headers       headers
              :url           url
              :metadata      metadata
              :timeout       timeout
              :params        params
              :resource      resource
              :response-time response-time}]
    (try
      (assoc base
             :body parsed)
      (catch :default e
        (log/error {:message (.getMessage e)}
                   "error parsing request")
        (assoc base
               :parse-error true
               :body parsed)))))

(defn- get-error-status [exception]
  (cond
    (instance? java.lang.IllegalArgumentException exception) 400
    (instance? clojure.lang.ExceptionInfo exception) 408
    :else 0))

(defn- get-error-message [exception]
  (cond
    (instance? clojure.lang.ExceptionInfo exception) (str "Error: " (.getMessage exception))
    (instance? java.lang.IllegalArgumentException exception) (str "Error: " (.getMessage exception))
    :else "Internal error"))

(defn- get-after-ctx [{:keys [ctx status response-time request result]}]
  (merge {} ctx request result {:status status
                                :response-time response-time}))

(defn- mount-url [url params]
  (str url (if (empty? params) "" (str "?" (js/encodeURI params)))))

(defn- build-debug-map [response request-map query-opts]
  {:debug {:url (mount-url (:url request-map) (merge (:query-params request-map) (:forward-params query-opts)))
           :timeout (:request-timeout request-map)
           :response-time (:response-time response)
           :request-headers (:headers request-map)
           :response-haders (:headers response)
           :params (merge (:query-params request-map) (:forward-params query-opts))}})

(defn- response-with-debug [response request-map query-opts]
  (into response (build-debug-map response request-map query-opts)))

(defn- request-respond-callback [result & {:keys [request
                                                  request-timeout
                                                  time-before
                                                  query-opts
                                                  output-ch
                                                  request-map
                                                  before-hook-ctx]}]
  (let [log-data {:resource (:from request)
                  :timeout  request-timeout
                  :success  true}]
    (log/debug (assoc log-data :success true
                      :status (:status result)
                      :time (- (.getTime (js/Date.)) time-before))
               "Request successful")
    (let [response (convert-response result {:debugging (:debugging query-opts)
                                             :metadata  (:metadata request)
                                             :resource  (:from request)
                                             :url       (:url request)
                                             :params    (:query-params request)
                                             :timeout   request-timeout
                                             :response-time  (- (.getTime (js/Date.)) time-before)})
          ; After Request hook
          _ (hook/execute-hook :after-request (get-after-ctx {:ctx before-hook-ctx
                                                              :status (:status result)
                                                              :response-time (- (.getTime (js/Date.)) time-before)
                                                              :request request
                                                              :result result}))]
      ; Send response to channel
      (go (->> (if (:debugging query-opts)
                 (response-with-debug response request-map query-opts)
                 response)
               (>! output-ch))
          (close! output-ch)))))

(defn- build-error-response [error-data exception]
  (merge (select-keys error-data [:success :status :metadata :url :params :timeout :response-time])
         {:body {:message (get-error-message exception)}}))

(defn- request-error-callback [exception & {:keys [request
                                                   request-timeout
                                                   time-before
                                                   query-opts
                                                   output-ch
                                                   request-map
                                                   before-hook-ctx]}]
  (if (and (instance? clojure.lang.ExceptionInfo exception) (:body (.getData exception)))
    (request-respond-callback (.getData exception)
                              :request         request
                              :request-timeout request-timeout
                              :query-opts      query-opts
                              :time-before     time-before
                              :request-map     request-map
                              :output-ch       output-ch
                              :before-hook-ctx before-hook-ctx)
    (let [error-status (get-error-status exception)
          log-data {:resource (:from request)
                    :timeout  request-timeout
                    :success  false}]
      (log/debug (assoc log-data :success false
                        :status error-status
                        :response-time (- (.getTime (js/Date.)) time-before)))
      (let [error-data (assoc log-data :success false
                              :status error-status
                              :metadata (some-> request :metadata)
                              :method (:method request)
                              :url (some-> request :url)
                              :params    (:query-params request)
                              :response-time (- (.getTime (js/Date.)) time-before)
                              :errordetail (pr-str (some-> exception :error)))
            ; After Request hook
            _ (hook/execute-hook :after-request (get-after-ctx {:ctx before-hook-ctx
                                                                :status error-status
                                                                :response-time (- (.getTime (js/Date.)) time-before)
                                                                :request request
                                                                :result error-data}))
            error-response (build-error-response error-data exception)]
        (log/warn error-data "Request failed")
        ; Send error response to channel
        (go (->> (if (:debugging query-opts)
                   (response-with-debug error-response request-map query-opts)
                   error-response)
                 (>! output-ch))
            (close! output-ch))))))

(defn- lower-case-keys [kv]
  (into {} (map (fn [[k v]]
                  {(clojure.string/lower-case k) v}) kv)))

(defn- append-request-headers-to-query-opts [request query-opts]
  (let [forward-headers (lower-case-keys (:forward-headers query-opts))
        with-headers   (lower-case-keys (:with-headers request))]

    (merge forward-headers with-headers)))

(defn- empty-header-to-empty-string [headers]
  (into {} (map (fn [[k v]]
                  [k (if (= :empty v) "" v)]) headers)))

(defn decode
  [response]
  ;; NOTE: It is not optimized for speed and the transit.cljs library is recommended for parsing large amounts of JSON data.  
  ;; https://cljs.github.io/api/cljs.core/js-GTclj
  (update response :body #(-> (.parse js/JSON %)
                              (js->clj :keywordize-keys true))))

(defn- make-request [request query-opts output-ch]
  (let [request         (parse-query-params request)
        time-before     (.getTime (js/Date.))
        request-timeout (if (nil? (:timeout request)) (:timeout query-opts) (:timeout request))
        request-map        {:url                (:url request)
                            :request-method     (:method request)
                            :content-type       "application/json"
                            :resource           (:from request)
                            :query-params       (valid-query-params request query-opts)
                            :headers            (-> request (append-request-headers-to-query-opts query-opts) (empty-header-to-empty-string))
                            :time               time-before
                            :body               (some-> request :body json/encode)}
         ; Before Request hook
        before-hook-ctx (hook/execute-hook :before-request request-map)]
    (log/debug request-map "Preparing request")
    (-> (http/send! client request-map {:timeout request-timeout})
        (p/then decode)
        (p/then  #(request-respond-callback %
                                            :request request
                                            :request-timeout request-timeout
                                            :query-opts query-opts
                                            :time-before time-before
                                            :output-ch output-ch
                                            :request-map request-map
                                            :before-hook-ctx before-hook-ctx))
        (p/catch #(request-error-callback %
                                          :request request
                                          :request-timeout request-timeout
                                          :query-opts query-opts
                                          :time-before time-before
                                          :output-ch output-ch
                                          :request-map request-map
                                          :before-hook-ctx before-hook-ctx)))
    output-ch))

(defn- create-skip-message [params]
  (str "The request was skipped due to missing {" (clojure.string/join ", " params) "} param value"))

(defn- get-empty-params [request]
  (->> request
       (:query-params)
       (keep (fn [[k v]] (when (= :empty v) k)))))

(defn verify-and-make-request
  [request query-opts]
  (let [output-ch    (chan)
        empty-params (get-empty-params request)]
    (if (empty? empty-params)
      (make-request request query-opts output-ch)
      (do
        (go (>! output-ch {:status 400 :body (create-skip-message empty-params)}))
        output-ch))))