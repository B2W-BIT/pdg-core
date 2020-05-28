(ns restql.test-util
  (:require [restql.parser.json :as json]))

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
   {:status status :content-type "application/json" :body (json/generate-string body)})
  ([status body headers]
   {:status status :content-type "application/json" :body (json/generate-string body) :headers headers}))

(defn- verify-header [request [k v]]
  (= (get-in request [:headers (keyword k)]) v))

(defn route-header
  [path headers]
  (fn [request]
    (and (= (get-in request [:path]) path)
         (every? true? (map (partial verify-header request) headers)))))

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