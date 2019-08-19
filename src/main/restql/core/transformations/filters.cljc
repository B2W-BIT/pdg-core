(ns restql.core.transformations.filters
  (:require [restql.log :as log]))

(defn- filter-match [arg data]
  (try
    (->> data
         str
         (re-find (re-pattern arg))
         (boolean))
    (catch Exception e (do
                         (log/error {:message (.getMessage e)}
                                    "filter-match: error filtering data:" data
                                    " with arg:" arg)
                         false))))

(defn- filter-equals [arg data]
  (= data arg))

(defn- do-filter [modifier value]
  (let [[filter-name filter-arg] modifier]
    (cond
      (= filter-name :matches) (when (filter-match filter-arg value) value)
      (= filter-name :equals)  (when (filter-equals filter-arg value) value)
      :else value)))

(defn- apply-filter [value modifier]
  (if (sequential? value)
    (filter (partial do-filter modifier) value)
    (do-filter modifier value)))

(defn apply-to-value [value modifiers]
  (->> modifiers
       (reduce apply-filter value)))
