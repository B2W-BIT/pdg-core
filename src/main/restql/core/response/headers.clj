(ns restql.core.response.headers
  (:require [clojure.walk :refer [keywordize-keys]]
            [clojure.set :refer [rename-keys]]
            [restql.core.response.headers.external :as external-headers]
            [restql.core.response.headers.cache-control :as cache-control-headers]))

(defn map-headers-to-resource
  "Given a key-value pair, where key is the resource alias
   and value is it's value, extracts only the headers to a
   new map."
  [[resource response]]
  (into {} {resource (some-> response :details :headers)})
)

(defn map-response-headers-by-resources
  "Gets all result headers and return a map of :resource headers"
  [response]
  (->>
    response
    (map map-headers-to-resource)
    (into {})
  )
)

(defn get-response-headers [query response]
  (merge
    (->> response
         (map-response-headers-by-resources)
         (external-headers/get-alias-suffixed-headers)
    )
    (->> response
         (map-response-headers-by-resources)
         (cache-control-headers/get-cache-header query)
    )
  )
)
