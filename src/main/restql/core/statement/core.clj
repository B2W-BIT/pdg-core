(ns restql.core.statement.core
  (:require [restql.core.statement [expand :refer [expand]]
             [resolve-chained-values :refer [resolve-chained-values]]
             [apply-encoders :refer [apply-encoders]]]
            [restql.core.util.assoc? :refer [assoc?]]
            [restql.core.statement.url-utils :as url-utils]))

(defn- add-default-method [request]
  (conj {:method :get} request))

(defn- add-url [mappings statement request]
  (-> (url-utils/from-mappings mappings statement)
      (url-utils/interpolate (:with statement))
      (as-> url (assoc {} :url url))
      (conj request)))

(defn- is-post-or-put? [statement]
  (or (= :post (:method statement)) (= :put (:method statement))))

(defn- add-query-params [mappings statement request]
  (-> statement
      (get :with)
      (as-> params (if (is-post-or-put? statement)
                     (url-utils/filter-explicit-query-params (url-utils/from-mappings mappings statement) params)
                     (url-utils/dissoc-path-params (url-utils/from-mappings mappings statement) params)))
      (as-> params (if-not (empty? params) (assoc? {} :query-params params) {}))
      (conj request)))

(defn- add-body-params [mappings statement request]
  (-> statement
      (get :with)
      (as-> params (if (is-post-or-put? statement)
                     (url-utils/dissoc-params (url-utils/from-mappings mappings statement) params)
                     (identity {})))
      (as-> params (if-not (empty? params) (assoc? {} :body params) {}))
      (conj request)))

(defn- add-metadata-from-statement-meta [statement]
  (if-not (empty? (meta statement))
    (assoc statement :metadata (meta statement))
    statement))

(def params-ignored-from-request [:with])

(defn- statement->request [mappings statement]
  (->> (apply dissoc statement params-ignored-from-request)
       (add-metadata-from-statement-meta)
       (add-default-method)
       (add-url mappings statement)
       (add-query-params mappings statement)
       (add-body-params mappings statement)))

(defn from-statements [mappings statements]
  (if (sequential? (first statements))
    (map #(from-statements mappings %) statements)
    (map (partial statement->request mappings) statements)))

(defn build [mappings statement done-requests encoders]
  (->> (resolve-chained-values statement done-requests)
       (expand)
       (apply-encoders encoders)
       (from-statements mappings)))
