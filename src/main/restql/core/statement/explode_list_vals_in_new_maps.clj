(ns restql.core.statement.explode-list-vals-in-new-maps)

(defn- should-expand? [v]
  (not (false? (:expand (meta v)))))

(defn- min-list-size-from-map-or-zero
  "Returns least of the lists size inside a map or zero if no list"
  [m]
  (->>
   m
   (filter (fn [[k v]] (and (sequential? v) (should-expand? v))))
   (sort-by second #(< (count %1) (count %2)))
   (first)
   (second)
   (count)))

(defn explode-list-vals-in-new-maps
  "Explodes list vals inside a map in new maps accordingly with :expand meta.
   Note - Doesn't explode lists in nested maps.
   {:s [1 2] :t 3} -> [{:s 1 :t 3} {:s 2 :t 3}]"
  [m]
  (let [n (min-list-size-from-map-or-zero m)]
    (if (zero? n)
      m
      (->>
       (for [i (range n)]
         (map (fn [[k v]] (if (and (sequential? v) (should-expand? v))
                            [k (nth v i)]
                            [k v])) m))
       (map #(into {} %))
       (vec)))))
