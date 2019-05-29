(ns restql.core.api.core
  (:require [restql.core.api.restql :as restql]
            [restql.parser.json :as json]))

(defn concat-encoders [encoders]
  (if (nil? encoders)
    (restql/get-default-encoders)
    (into (restql/get-default-encoders) encoders)))

(defn stringify-query [query]
  (binding [*print-meta* true]
    (pr-str query)))

(defn query [& {:keys [mappings encoders query query-opts callback]}]
  (let [output (promise)]
    (restql/execute-query-async :mappings mappings
                                :encoders (concat-encoders encoders)
                                :query (stringify-query query)
                                :options query-opts
                                :callback (fn [result]
                                            (let [parsed (json/parse-string result true)]
                                              (deliver output parsed)
                                              (when-not (nil? callback)
                                                (callback result)))))
    output))