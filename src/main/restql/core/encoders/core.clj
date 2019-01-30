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

(defn get-encoder-key [data]
  (let [from-meta (-> data meta :encoder)]
    (cond
      from-meta          from-meta
      (set? data)        :set
      (map? data)        :json
      :else              :default)))

(defn perfom-encoding [encoders data]
  (let [encoder-key (get-encoder-key data)]
    (cond
      (and (sequential? data) (= encoder-key :default)) (map (partial perfom-encoding encoders) data)
      :else (let [encoder-fn (if-not (nil? encoder-key) (encoders encoder-key))]
              (if-not (nil? encoder-fn)
                (encoder-fn data)
                (throw+ {:type :unrecognized-encoding :data encoder-key}))))))

(defn encode [encoders data]
  (-> base-encoders
      (merge encoders)
      (perfom-encoding data)))
