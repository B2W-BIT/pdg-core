(ns restql.core.transformations.select
  (:require [restql.core.transformations.filters :as filters]
            [restql.core.util.deep-merge :refer [deep-merge]]
            [restql.core.util.get-in-with-list-support :refer [get-in-with-list-support]]
            [clojure.walk :refer [postwalk]]))

(defn apply-filters [value filters]
  (if (nil? filters)
    value
    (filters/apply-to-value value filters)))

(defn- select-rules [query resource-name]
  (->> query
       (apply assoc {})
       (resource-name)
       (:select)))

(defn- select-all? [selection-rules]
  (->> selection-rules
       (some #(= :* (first %)))))

(defn- initial-result [selection-rules resource-result]
  (cond
    (select-all? selection-rules) resource-result
    (sequential? resource-result) []
    :else {}))

(defn select-item
  ([raw-result selector] (select-item raw-result selector (meta selector)))
  ([raw-result selector filters]
   (cond
     (= [:*] selector)        {}
     (sequential? raw-result) (->> raw-result (map #(select-item % selector filters)) vec)
     (map? raw-result)        (-> selector first raw-result (select-item (rest selector) filters) (as-> val (if (nil? val) {} {(first selector) val})))
     :else                    (apply-filters raw-result filters))))

(defn- merge-selects [r1 r2]
  (cond
    (empty? r1) r2
    (and (sequential? r1) (sequential? r2)) (vec (map merge-selects r1 r2))
    (and (map? r1) (map? r2)) (merge-with merge-selects r1 r2)
    :else r2))

(defn- filter-result [raw-result filtered-result select-rule]
  (->> select-rule
       (select-item raw-result)
       (merge-selects filtered-result)))

(defn- filter-nils [result]
  (postwalk
     (fn [el]
       (cond
         (sequential? el) (->> el (filter (complement nil?)) (into []))
         (map? el) (->> el (filter (comp not nil? second)) (into {}))
         :else el))
     result))

(defn- resource-filtered [selection-rules resource-name resource-data]
  (let [result (:result resource-data)
        initial (initial-result selection-rules result)]
    (->> selection-rules
         (reduce (partial filter-result result) initial)
         (filter-nils)
         (assoc-in resource-data [:result])
         (conj [resource-name]))))

(defn- resource-hidden? [selection-rules]
  (= :none selection-rules))

(defn- resource-selection [query resource-response]
  (let [[name data] resource-response
        selection-rules (select-rules query name)]
    (cond
      (nil? selection-rules) resource-response
      (resource-hidden? selection-rules) nil
      :else (resource-filtered selection-rules name data))))

(defn from-result [query result]
  (->> result
       (map (partial resource-selection query))
       (into {})))
