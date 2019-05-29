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
            [clojure.core.async :refer [go go-loop  <! >! alt! alts! timeout]]
            [environ.core :refer [env]]
            #?(:clj [clojure.core.async :refer [<!!]]
               :cljs [promesa.core :as p]))
  (:use #?(:clj [clojure.core :only  [read-string]]
           :cljs [cljs.reader :only  [read-string]])))

(def default-values {:query-resource-timeout 5000
                     :query-global-timeout 30000})

(defn get-default [key]
  (if (contains? env key) (read-string (env key)) (default-values key)))

(defn- parse-query [context string]
  (->> string
       (validator/validate context)
       (partition 2)))

(defn- extract-result [parsed-query timeout-ch exception-ch query-ch query-opts]
  (go
    (alt!
      timeout-ch ([]
                  {:error :timeout})
      exception-ch ([err]
                    err)
      query-ch ([result]
                (->> (response-builder/build (reduce (fn [res [key value]] (assoc res key value))
                                                     {}
                                                     result))
                     (select (flatten parsed-query))
                     (aggregation/aggregate parsed-query))))))

(defn get-default-encoders []
  (encoders/get-default-encoders))

(defn- set-default-query-options [query-options]
  (into {:timeout        (get-default :query-resource-timeout)
         :global-timeout (get-default :query-global-timeout)} query-options))

(defn execute-query-channel [& {:keys [mappings encoders query query-opts]}]
  (let [; Before query hook
        _ (hook/execute-hook :before-query {:query query
                                            :query-options query-opts})

        time-before #?(:clj (System/currentTimeMillis)
                       :cljs (.getTime (js/Date.)))

        ; Executing query
        query-opts (set-default-query-options query-opts)
        parsed-query (parse-query {:mappings mappings :encoders encoders} query)
        [output-ch exception-ch] (runner/run mappings parsed-query encoders query-opts)
        parsed-ch (extract-result parsed-query (timeout (:global-timeout query-opts)) exception-ch output-ch query-opts)
        return-ch (go
                    (let [[query-result _] (alts! [parsed-ch exception-ch])
                          ; After query hook
                          _ (hook/execute-hook :after-query {:query-options query-opts
                                                             :query         query
                                                             :result        query-result
                                                             :response-time (-  #?(:clj (System/currentTimeMillis)
                                                                                   :cljs (.getTime (js/Date.))) time-before)})]
                      query-result))]
    [return-ch exception-ch]))

#?(:clj
   (defn execute-parsed-query [& {:keys [mappings encoders query query-opts]}]
     (let [[result-ch _exception-ch] (execute-query-channel :mappings mappings
                                                            :encoders encoders
                                                            :query query
                                                            :query-opts query-opts)
           result (<!! result-ch)]
       result)))

#?(:clj
   (defn execute-query [& {:keys [mappings encoders query params options]}]
     (let [parsed-query (parser/parse-query query :context (stringify-keys params))]
       (execute-parsed-query :mappings mappings
                             :encoders encoders
                             :query parsed-query
                             :query-opts options))))

(defn execute-parsed-query-async [& {:keys [mappings encoders query query-opts callback]}]
  (go
    (let [[result-ch _exception-ch] (execute-query-channel :mappings mappings
                                                           :encoders encoders
                                                           :query query
                                                           :query-opts query-opts)
          result (<! result-ch)]
      (callback result))))

#?(:clj (defn execute-query-async [& {:keys [mappings encoders query params options callback]}]
          (let [parsed-query (parser/parse-query query :context (stringify-keys params))]
            (execute-parsed-query-async :mappings mappings
                                        :encoders encoders
                                        :query parsed-query
                                        :query-opts options
                                        :callback callback)))
   :cljs (defn ^:export execute-query-async [mappings query params options]
           (let [p (p/promise)]
             (execute-parsed-query-async :mappings (js->clj mappings :keywordize-keys true)
                                         :query (parser/parse-query (js->clj query) :context (stringify-keys  (js->clj params)))
                                         :query-opts (js->clj options)
                                         :callback (fn [result] (p/resolve! p (clj->js result))))
             p)))