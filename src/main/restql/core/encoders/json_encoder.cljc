(ns restql.core.encoders.json-encoder
  (:require [restql.parser.json :as json]))

(declare remove-nils)

(defn remove-nils-from-sequential [data]
  (let [filtered (filter (complement nil?) data)]
    (map remove-nils filtered)))

(defn remove-nils-from-map [data]
  (reduce-kv (fn [m k v]
               (if (nil? v)
                 m
                 (assoc m k (remove-nils v)))) {} data))

(defn remove-nils [data]
  (cond
    (map? data) (remove-nils-from-map data)
    (sequential? data) (remove-nils-from-sequential data)
    :else data))

(defn encode [data]
  (json/generate-string (remove-nils data)))