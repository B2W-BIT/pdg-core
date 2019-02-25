(ns restql.core.runner.request
  (:require [clojure.core.async :refer [chan go >!]]
            [aleph.http :as http]
            [manifold.deferred :as d]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [restql.hooks.core :as hook])
  (:import [java.net URLDecoder]))

(def default-values {:pool-connections-per-host 100
                     :pool-total-connections 10000
                     :pool-max-queue-size 65536
                     :pool-control-period 60000})

(defn- get-default
  ([key] (if (contains? env key) (read-string (env key)) (default-values key)))
  ([key default] (if (contains? env key) (read-string (env key)) default)))

(defonce client-connection-pool
         (http/connection-pool {:connections-per-host (get-default :pool-connections-per-host)
                                :total-connections    (get-default :pool-total-connections)
                                :max-queue-size       (get-default :pool-max-queue-size)
                                :control-period       (get-default :pool-control-period)
                                :stats-callback       #(hook/execute-hook :stats-conn-pool (assoc {} :stats %))}))

(defn- fmap [f m]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn- decode-url [string]
  (try
    (URLDecoder/decode string "utf-8")
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

(defn- convert-response [{:keys [status body headers]} {:keys [_debugging metadata time url params timeout resource]}]
  (let [parsed (if (string? body) body (slurp body))
        base {:status        status
              :headers       headers
              :url           url
              :metadata      metadata
              :timeout       timeout
              :params        params
              :resource      resource
              :response-time time}]
    (try
      (assoc base
        :body (json/parse-string parsed true))
      (catch Exception e
        (log/error {:message (.getMessage e)}
                   "error parsing request")
        (assoc base
          :parse-error true
          :body parsed)))))

(defn- get-error-status [exception]
  (cond
    (instance? java.lang.IllegalArgumentException exception) 400
    (instance? clojure.lang.ExceptionInfo exception) 408
    (instance? aleph.utils.RequestTimeoutException exception) 408
    (instance? aleph.utils.ConnectionTimeoutException exception) 408
    (instance? aleph.utils.ReadTimeoutException exception) 408
    (instance? aleph.utils.ProxyConnectionTimeoutException exception) 408
    (instance? aleph.utils.PoolTimeoutException exception) 0
    :else 0))

(defn- get-error-message [exception]
  (cond
    (instance? clojure.lang.ExceptionInfo exception) (str "Error: " (.getMessage exception))
    (instance? java.lang.IllegalArgumentException exception) (str "Error: "(.getMessage exception))
    (instance? aleph.utils.RequestTimeoutException exception) "RequestTimeoutException"
    (instance? aleph.utils.ConnectionTimeoutException exception) "ConnectionTimeoutException"
    (instance? aleph.utils.PoolTimeoutException exception) "PoolTimeoutException"
    (instance? aleph.utils.ReadTimeoutException exception) "ReadTimeoutException"
    (instance? aleph.utils.ProxyConnectionTimeoutException exception) "ProxyConnectionTimeoutException"
    :else "Internal error"))

(defn- get-after-ctx [{:keys [ctx status response-time request result]}]
  (merge {} ctx request result {:status status
                                :response-time response-time}))

(defn- request-respond-callback [result & {:keys [request
                                                  request-timeout
                                                  time-before
                                                  query-opts
                                                  output-ch
                                                  before-hook-ctx]}]
  (let [log-data {:resource (:from request)
                  :timeout  request-timeout
                  :success  true}]
    (log/debug (assoc log-data :success true
                               :status (:status result)
                               :time (- (System/currentTimeMillis) time-before))
               "Request successful")
    (let [response (convert-response result {:debugging (:debugging query-opts)
                                             :metadata  (:metadata request)
                                             :resource  (:from request)
                                             :url       (:url request)
                                             :params    (:query-params request)
                                             :timeout   request-timeout
                                             :time      (- (System/currentTimeMillis) time-before)})
          ; After Request hook
          _ (hook/execute-hook :after-request (get-after-ctx {:ctx before-hook-ctx
                                                              :status (:status result)
                                                              :response-time (- (System/currentTimeMillis) time-before)
                                                              :request request
                                                              :result result}))]
      ; Send response to channel
      (go (>! output-ch response)))))

(defn- request-error-callback [exception & {:keys [request
                                                   request-timeout
                                                   time-before
                                                   query-opts
                                                   output-ch
                                                   before-hook-ctx]}]
  (if (and (instance? clojure.lang.ExceptionInfo exception) (:body (.getData exception)))
    (request-respond-callback (.getData exception)
                              :request         request
                              :request-timeout request-timeout
                              :query-opts      query-opts
                              :time-before     time-before
                              :output-ch       output-ch
                              :before-hook-ctx before-hook-ctx)
    (let [error-status (get-error-status exception)
          log-data {:resource (:from request)
                    :timeout  request-timeout
                    :success  false}]
      (log/debug (assoc log-data :success false
                                 :status error-status
                                 :time (- (System/currentTimeMillis) time-before)))
      (let [error-data (assoc log-data :success false
                                       :status error-status
                                       :metadata (some-> request :metadata)
                                       :method (:method request)
                                       :url (some-> request :url)
                                       :params    (:query-params request)
                                       :response-time (- (System/currentTimeMillis) time-before)
                                       :errordetail (pr-str (some-> exception :error)))
            ; After Request hook
            _ (hook/execute-hook :after-request (get-after-ctx {:ctx before-hook-ctx
                                                                :status error-status
                                                                :response-time (- (System/currentTimeMillis) time-before)
                                                                :request request
                                                                :result error-data}))]
        (log/warn error-data "Request failed")
        ; Send error response to channel
        (go (>! output-ch (merge (select-keys error-data [:success :status :metadata :url :params :timeout :response-time])
                                 {:body {:message (get-error-message exception)}})))))))

(defn- build-request-map [request request-timeout valid-query-params time body-encoded poll-timeout]
  {:url                (:url request)
   :request-method     (:method request)
   :content-type       "application/json"
   :resource           (:from request)
   :connection-timeout request-timeout
   :request-timeout    request-timeout
   :read-timeout       request-timeout
   :query-params       valid-query-params
   :headers            (:with-headers request)
   :time               time
   :body               body-encoded
   :pool               client-connection-pool
   :pool-timeout       poll-timeout})

(defn make-request
  ([request query-opts]
   (let [output-ch (chan)]
     (make-request request query-opts output-ch)
     output-ch))
  ([request query-opts output-ch]
   (let [request         (parse-query-params request)
         time-before     (System/currentTimeMillis)
         request-timeout (if (nil? (:timeout request)) (:timeout query-opts) (:timeout request))
         request-map (build-request-map
                       request request-timeout
                       (valid-query-params request query-opts)
                       time-before
                       (some-> request :body json/encode)
                       (get-default :pool-timeout request-timeout))
         ; Before Request hook
         before-hook-ctx (hook/execute-hook :before-request request-map)]
     (log/debug request-map "Preparing request")
     (-> (http/request request-map)
         (d/chain #(request-respond-callback %
                                             :request request
                                             :request-timeout request-timeout
                                             :query-opts query-opts
                                             :time-before time-before
                                             :output-ch output-ch
                                             :before-hook-ctx before-hook-ctx))
         (d/catch Exception #(request-error-callback %
                                                     :request request
                                                     :request-timeout request-timeout
                                                     :query-opts query-opts
                                                     :time-before time-before
                                                     :output-ch output-ch
                                                     :before-hook-ctx before-hook-ctx))
         (d/success! 1)))))
