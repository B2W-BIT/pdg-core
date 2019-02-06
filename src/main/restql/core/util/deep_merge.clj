(ns restql.core.util.deep-merge)

(defn deep-merge
  "{:a {:b :replace-me :c 2}} {:a {:b 1}} -> {:a {:b 1, :c 2}}
   {:a {:b :replace-me :d 3}} {:a [{:b 1}{:b 2}]} -> {:a [{:b 1, :d 3} {:b 2, :d 3}]}"
  [v & vs]
  (letfn [(rec-merge [v1 v2]
            (cond
              (and (map? v1) (map? v2)) (merge-with deep-merge v1 v2)
              (and (map? v1) (sequential? v2)) (let [merged (->> v2 (map #(merge v1 %)) (vec))]
                                                 (if (and (meta v1) (instance? clojure.lang.IMeta v1))
                                                   (with-meta merged (meta v1))
                                                   merged))
              :else v2))]
    (if (some identity vs)
      (reduce #(rec-merge %1 %2) v vs)
      (last vs))))