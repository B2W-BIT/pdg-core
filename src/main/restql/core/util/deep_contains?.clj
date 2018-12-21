(ns restql.core.util.deep-contains?)

(defn deep-contains? [m ks]
  (->> (reduce (fn [m k]
                   (if (contains? m k)
                       (get m k)
                       false))
               m ks
       )
       (false?)
       (not)
  )
)
