(ns restql.core.util.get-in-with-list-support)

(defn get-in-with-list-support
  "Like get-in but with list support. (Not Lazy)"
  [[k & rest] v]
  (cond
    (and (nil? rest) (sequential? (k v))) (vec (map #(get-in-with-list-support [k] %) (k v)))
    (and (nil? rest) (map? v)) (k v)
    (nil? rest) v
    (sequential? (k v)) (vec (map #(get-in-with-list-support rest %) (k v)))
    :else (get-in-with-list-support rest (k v))))