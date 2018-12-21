(ns restql.core.util.deep-merge)

(defn deep-merge [v & vs]
  (letfn [(rec-merge [v1 v2]
            (if (and (map? v1) (map? v2))
                (merge-with deep-merge v1 v2)
                v2
            )
          )]
    (if (some identity vs)
        (reduce #(rec-merge %1 %2) v vs)
        (last vs)
    )
  )
)
