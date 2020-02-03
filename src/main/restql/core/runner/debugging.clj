(ns restql.core.runner.debugging
  (:require [ring.util.codec :refer [form-encode]]))

(defn- mount-url [url params]
  (str url (if (empty? params) "" (str "?" (form-encode params)))))

(defn- post-put-patch? [request]
  (some #(= % (:method request)) [:post :put :patch]))

(defn- add-body-if-post-put-patch [request debug-map]
  (if (post-put-patch? request)
    (assoc debug-map :request-body (:body request))
    debug-map))

(defn build-map [response request-map query-opts]
  (->> {:url             (mount-url (:url request-map) (merge (:query-params request-map) (:forward-params query-opts)))
        :timeout         (:request-timeout request-map)
        :response-time   (:response-time response)
        :request-headers (:headers request-map)
        :response-haders (:headers response)
        :params          (merge (:query-params request-map) (:forward-params query-opts))}
       (add-body-if-post-put-patch request-map)
       (assoc {} :debug)))
