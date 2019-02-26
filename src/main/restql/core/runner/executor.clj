(ns restql.core.runner.executor
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure.core.async :refer [chan go go-loop >! <!]]
            [slingshot.slingshot :refer [try+]]
            [restql.core.statement.core :as statement]
            [restql.core.runner.request :refer [make-request]]))

(defn- query-and-join [requests query-opts]
  (let [perform-func (fn [func requests query-opts]
                       (go-loop [[ch & others] (map #(func % query-opts) requests)
                                 result []]
                         (if ch
                           (recur others (conj result (<! ch)))
                           result)))]
    (cond
      (sequential? (first requests)) (perform-func query-and-join requests query-opts)
      :else (perform-func make-request requests query-opts))))

(defn- single-request-not-multiplexed? [requests]
  (and
   (= 1 (count requests))
   (not (sequential? (first requests)))
   (not (:multiplexed (first requests)))))

(defn- perform-request [result-ch query-opts statements]
  (cond
    (single-request-not-multiplexed? statements) (make-request (first statements) query-opts result-ch)
    :else (go (->>
               (query-and-join statements query-opts)
               (<!)
               (>! result-ch)))))

(defn  do-request [mappings {:keys [to-do state]} encoders exception-ch {:keys [_debugging] :as query-opts}]
  (try+
   (let [[query-name statement] to-do
         result-ch (chan 1 (map #(vector query-name %)))]
     (->> (statement/build mappings statement state encoders)
          (perform-request result-ch query-opts))
     result-ch)
   (catch Object e
     (go (>! exception-ch e)))))
