(ns restql.core.transformations.select
  (:require [restql.core.transformations.filters :as filters]))

(defn- select-keyword [select-param entity-value]
  (get entity-value select-param))

(declare do-selection)

(defn- apply-filters [filter-data entity-value]
  (if (seq filter-data)
    (filters/apply-filters entity-value filter-data)
    entity-value))

(defn- select-expression [[select-key & select-params] entity-value]
  (let [entity-item (get entity-value select-key)
        subselect (->> select-params (filter set?) first)
        filters (filter map? select-params)
        filtered-items (apply-filters filters entity-item)]
    (if (nil? subselect)
      filtered-items
      (do-selection subselect filtered-items))))

(defn- contains-wildcard? [select-params]
  (if (set? select-params)
    (some #(= :* %) select-params)
    false))

(defn- select-single [select-params entity-value]
  (reduce (fn [result select-param]
            (cond
              (= :* select-param) result
              (keyword? select-param) (assoc result select-param (select-keyword select-param entity-value))
              (vector? select-param) (assoc result (first select-param) (select-expression select-param entity-value))
              :else result))
          (if (contains-wildcard? select-params) entity-value {})
          select-params))

(defn- do-selection [fields-to-select result]
  (if (sequential? result)
    (map #(do-selection fields-to-select %) result)
    (select-single fields-to-select result)))

(defn- filter-resource-response [fields-to-select resource-response]
  (->> resource-response
       (:result)
       (do-selection fields-to-select)
       (assoc {} :result)
       (merge {:details (:details resource-response)})))

(defn- reduce-with [query]
  (fn [acc resource resource-response]
    (let [select-params (:select (resource query))]
      (cond
        (set? select-params) (assoc acc resource (filter-resource-response select-params resource-response))
        (= :none select-params) acc
        :else (assoc acc resource resource-response)))))

(defn select [query result]
  (let [query-map (apply hash-map query)]
    (reduce-kv (reduce-with query-map) {} result)))
