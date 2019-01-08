(ns restql.core.request
  (:require [restql.core.util.assoc? :refer [assoc?]]
            [restql.core.url :as url])
)

(defn- add-default-method [request]
  (conj {:method :get} request)
)

(defn- add-url [mappings statement request]
  (-> (url/from-mappings mappings statement)
      (url/interpolate (:with statement))
      (as-> url (assoc {} :url url))
      (conj request)
  )
)

(defn- add-query-params [mappings statement request]
  (-> statement
      (get :with)
      (as-> params (url/dissoc-params (url/from-mappings mappings statement) params))
      (as-> params (if-not (empty? params) (assoc? {} :query-params params) {}))
      (conj request)
  )
)

(def params-ignored-from-request [:with])

(defn- statement->request [mappings statement]
  (->> (apply dissoc statement params-ignored-from-request)
       (add-default-method)
       (add-url mappings statement)
       (add-query-params mappings statement)
  )
)

(defn from-statements [mappings statements]
  (if (sequential? (first statements))
      (map #(from-statements mappings %) statements)
      (map (partial statement->request mappings) statements)))