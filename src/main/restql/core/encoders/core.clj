(ns restql.core.encoders.core
  (:require [restql.core.encoders.json-encoder :as json]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [throw+ try+]]))

(defn set-encoder [data]
  (log/warn "use of deprecated encoder :set on" data)
  (->> data
       (map str)
       (into [])))

(def base-encoders
  {:json json/encode
   :set set-encoder
   :default identity})

(defn perfom-encoding [encoders encoding value]
  (if (contains? encoders encoding)
    (let [encoding-fn (encoders encoding)]
      (encoding-fn value))
    (throw+ {:type :unrecognized-encoding :data encoding})))

(defn get-encoder-key [data]
  (let [from-meta (-> data meta :encoder)]
    (cond
      from-meta          from-meta
      (set? data)        :set
      (map? data)        :json
      :else              :default)))

(defn encode [encoders data]
  (-> encoders
      (merge base-encoders)
      (perfom-encoding (get-encoder-key data) data)
  )
)
