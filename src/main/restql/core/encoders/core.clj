(ns restql.core.encoders.core
  (:require [restql.core.encoders.json-encoder :as json]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [throw+ try+]]))

(defn default-encoder [data]
  (if (nil? data) nil
  (str data)))

(defn set-encoder [data]
  (log/warn "use of deprecated encoder :set on" data)
  (->> data
       (map str)
       (into [])
       ))

(def base-encoders
  {:json json/encode
   :set set-encoder})

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
      (sequential? data) :json
      :else              :default)))

(defn encode [encoders data]
  (-> encoders
      (merge base-encoders)
      (assoc :default default-encoder)
      (perfom-encoding (get-encoder-key data) data)
  )
)
