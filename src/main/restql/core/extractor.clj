(ns restql.core.extractor)

(defn traverse [data [map-key & path]]
  (if map-key
    (if (sequential? data)
      (recur (map map-key (flatten data)) path)
      (recur (map-key data) path))
    data))
