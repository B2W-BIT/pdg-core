(ns restql.core.api.restql
  (:require [restql.core.runner.core :as runner]
            [restql.core.validator.core :as validator]
            [restql.core.transformations.select :refer [select]]
            [restql.core.transformations.aggregation :as aggregation]
            [restql.hooks.core :as hook]
            [restql.core.api.response-builder :as response-builder]
            [restql.core.encoders.core :as encoders]
            [restql.parser.core :as parser]
            [clojure.walk :refer [stringify-keys]]
            [clojure.core.async :refer [go go-loop <!! <! >! alt! alts! timeout]]
            [environ.core :refer [env]]))

(defn- all-done?
  "given a state with queries :done :requested and :to-do returns
   true if all entries are :done"
  [state]
  (and (empty? (:to-do state)) (empty? (:requested state))))

(def default-values {:query-resource-timeout 5000
                     :query-global-timeout 30000})

(defn get-default [key]
  (if (contains? env key) (read-string (env key)) (default-values key)))

(defn- wait-until-finished [output-ch query-opts]
  (go-loop [state (<! output-ch)]
    (if (all-done? state)
      (response-builder/build (reduce (fn [res [key value]]
                                          (assoc res key value)) {} (:done state)) query-opts)
      (recur (<! output-ch)))))

(defn- parse-query [context string]
  (->> string
       (validator/validate context)
       (partition 2)))

(defn- extract-result [parsed-query timeout-ch exception-ch query-ch]
  (go
    (alt!
      timeout-ch ([] {:error :timeout})
      exception-ch ([err] err)
      query-ch ([result]
                 (let [output (->> result
                                   (select (flatten parsed-query))
                                   (aggregation/aggregate parsed-query))]
                   output)))))

(defn get-default-encoders []
  (encoders/get-default-encoders))

(defn- set-default-query-options [query-options]
  (into {:timeout        (get-default :query-resource-timeout)
         :global-timeout (get-default :query-global-timeout)} query-options))

(defn execute-query-channel [& {:keys [mappings encoders query query-opts]}]
  (let [; Before query hook
        _ (hook/execute-hook :before-query {:query query
                                            :query-options query-opts})
        time-before (System/currentTimeMillis)

        ; Executing query
        query-opts (set-default-query-options query-opts)
        parsed-query (parse-query {:mappings mappings :encoders encoders} query)
        [output-ch exception-ch] (runner/run mappings parsed-query encoders query-opts)
        result-ch (wait-until-finished output-ch query-opts)
        parsed-ch (extract-result parsed-query (timeout (:global-timeout query-opts)) exception-ch result-ch)
        return-ch (go
                    (let [[query-result ch] (alts! [parsed-ch exception-ch])

                          ; After query hook
                          _ (hook/execute-hook :after-query {:query-options query-opts
                                                             :query         query
                                                             :result        query-result
                                                             :response-time (- (System/currentTimeMillis) time-before)})]
                      query-result))]
    [return-ch exception-ch]))

(defn execute-parsed-query [& {:keys [mappings encoders query query-opts]}]
  (let [[result-ch _exception-ch] (execute-query-channel :mappings mappings
                                                        :encoders encoders
                                                        :query query
                                                        :query-opts query-opts)
        result (<!! result-ch)]
    result))

(defn execute-parsed-query-async [& {:keys [mappings encoders query query-opts callback]}]
  (go
    (let [[result-ch _exception-ch] (execute-query-channel :mappings mappings
                                                          :encoders encoders
                                                          :query query
                                                          :query-opts query-opts)
          result (<! result-ch)]
      (callback result))))

(defn execute-query [& {:keys [mappings encoders query params options]}]
  (let [parsed-query (parser/parse-query query :context (stringify-keys params))]
    (execute-parsed-query :mappings mappings
                          :encoders encoders
                          :query parsed-query
                          :query-opts options)))

(defn execute-query-async [& {:keys [mappings encoders query params options callback]}]
  (let [parsed-query (parser/parse-query query :context (stringify-keys params))]
    (execute-parsed-query-async :mappings mappings
                                :encoders encoders
                                :query parsed-query
                                :query-opts options
                                :callback callback)))
