(ns restql.core.runner.executor
  (:require [clojure.core.async :refer [go go-loop >! <! close!]]
            [restql.core.runner.request :refer [verify-and-make-request]])
  #?(:clj (:use [slingshot.slingshot :only [try+]])))

(defn- extract-result-keeping-channels-order [channels]
  (go-loop [[ch & others] channels
            result []]
    (if ch
      (recur others (conj result (<! ch)))
      result)))

(defn- query-and-join [requests query-opts]
  (let [operation (if (sequential? (first requests)) query-and-join verify-and-make-request)]
    (->> requests
         (mapv #(operation % query-opts))
         (extract-result-keeping-channels-order))))

(defn- single-request-not-multiplexed? [requests]
  (and
   (= 1 (count requests))
   (not (sequential? (first requests)))
   (not (:multiplexed (first requests)))))

(defn do-request [statements exception-ch {:keys [_debugging] :as query-opts}]
  #?(:clj (try+
           (if (single-request-not-multiplexed? statements)
             (verify-and-make-request (first statements) query-opts)
             (query-and-join statements query-opts))
           (catch Object e
             (go
               (>! exception-ch {:type "exception" :message (.getMessage e) :exception e})
               (close! exception-ch))))
     :cljs (try
             (if (single-request-not-multiplexed? statements)
               (verify-and-make-request (first statements) query-opts)
               (query-and-join statements query-opts))
             (catch :default e
               (go
                 (>! exception-ch {:type "exception" :message (.getMessage e) :exception e})
                 (close! exception-ch))))))