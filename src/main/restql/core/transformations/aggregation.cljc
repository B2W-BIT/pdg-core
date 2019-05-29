(ns restql.core.transformations.aggregation
  (:require [clojure.string :as string]))

(defn- find-target-resource [in-option]
  (some->
   in-option
   (identity)
   (name)
   (string/split #"\.")
   (first)
   (keyword)))

(defn- find-target-path [in-option]
  (some->
   in-option
   (identity)
   (name)
   (string/split #"\.")
   (rest)
   (as-> s (map keyword s))
   (doall)))

(defn resource-exists? [to result]
  (contains? result to))

(defn- find-aggregations [query result]
  (let [resource (first query)
        in-option (-> query second :in)
        to (find-target-resource in-option)
        path (find-target-path in-option)]
    (if (and in-option (resource-exists? to result))
      {:from resource :to to :path path})))

(defn- build-aggregation [result-from result-to [path & rest]]
  (cond
    (sequential? result-to) (map #(build-aggregation %1 %2 (conj rest path)) result-from result-to)
    (nil? rest) (assoc-in result-to [path] result-from)
    (sequential? (path result-to)) (assoc-in result-to [path] (map #(build-aggregation %1 %2 rest) result-from (path result-to)))
    :else (assoc-in result-to [path] (build-aggregation result-from (path result-to) rest))))

(defn- replace-result [from result]
  (if (seq? (from result))
    (->>
     (from result)
     (map #(dissoc % :result))
     (assoc result from))
    (update-in result [from] dissoc :result)))

(defn- replace-aggregation [result {from :from to :to path :path}]
  (let [result-from (->> result from :result)
        result-to (->> result to :result)]
    (->>
     path
     (build-aggregation result-from result-to)
     (assoc-in result [to :result])
     (replace-result from))))

(defn- replace-aggregation-when-needed [result from-to]
  (if (nil? from-to)
    result
    (replace-aggregation result from-to)))

(defn aggregate [query result]
  (->>
   query
   (map #(find-aggregations % result))
   (reduce replace-aggregation-when-needed result)))