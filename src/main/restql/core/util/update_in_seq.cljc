(ns restql.core.util.update-in-seq)

(defn update-in-seq [m ks f]
  "Do update-in in a keyword list"
  (reduce #(update-in % [%2] f) m ks)
)
