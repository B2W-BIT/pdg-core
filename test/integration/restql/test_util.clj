(ns restql.test-util
  (:require [cheshire.core :as json]))

(defn get-stub-body [request]
  (if-not (nil? (:body request))
    (val (first (:body request)))))

(defn stub-post-route
  ([path]
   (stub-post-route path nil nil))
  ([path body]
   (stub-post-route path body nil))
  ([path body query-params]
   (fn [request]
     (and (= (get request :path) path)
          (= (get request :method) "POST")
          (= (get request :query-params) query-params)
          (= (get-stub-body request) (if-not (nil? body) (json/generate-string body)))))))

(defn route-response
  ([body]
   {:status 200 :content-type "application/json" :body (json/generate-string body)})
  ([status body]
   {:status status :content-type "application/json" :body (json/generate-string body)}))

(defn- encode [value]
  (->
   value
   (as-> value (if (string? value)
                 value
                 (json/generate-string value)))
   (java.net.URLEncoder/encode)))

(defn route-request
  ([path]
   path)
  ([path query-params]
   {:path path
    :query-params (->> query-params (map (fn [[k v]] {k (encode v)})) (into {}))}))