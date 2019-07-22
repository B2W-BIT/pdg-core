(ns restql.core.runner.core
  (:require [clojure.core.async :refer [go-loop go <! >! chan alt! timeout close!]]
            [clojure.core.async.impl.protocols :refer [closed?] :rename {closed? chan-closed?}]
            [restql.log :as log]
            [clojure.set :as s]
            [restql.core.query :as query]
            [restql.core.runner.executor :as executor]
            [restql.core.statement.core :as statement]
            #?(:cljs ["uuid/v4" :as uuid4])))

(defn- all-done? [state]
  (and (empty? (:to-do state)) (empty? (:requested state))))

(defn- can-request?
  "given a single query item and the map with the current results
   returns true if all the dependencies of the query-item are
   resolved"
  [query-item state]
  (let [deps  (query/get-dependencies query-item)
        dones (->> state :done (map first) (into #{}))]
    (empty? (s/difference deps dones))))

(defn- all-that-can-request
  "takes a state with queries :done :requested and :to-do and returns
   a sequence of pairs with only the queries that can be executed, because all
   their dependencies are already met.

   Example return: ([:cart {:with ...}] [:freight {:with ...})"
  [state]
  (filter #(can-request? % state) (:to-do state)))

(defn- is-done? [[query-item-key _] state]
  (->> state
       :done
       (map first)
       (into #{})
       query-item-key
       nil?
       not))

(defn- update-state
  "it passes all to-do queries that could be requested to :requested state and
   adds a completed request to the :done state"
  [state completed]
  {:done (conj (:done state) completed)
   :requested (filter
               #(and (not= (first completed) (first %)) (not (is-done? % state)))
               (into (:requested state) (all-that-can-request state)))
   :to-do (filter #(not (can-request? % state)) (:to-do state))})

(defn- do-run
  "it separates all queries in three states, :done :requested and :to-do
   then sends all to-dos to resolve, changing their statuses to :requested.
   As the results get ready, update the query status to :done and send all to-dos again.
   When all queries are :done, the process is complete, and the :done part of the state is returned."
  [query {:keys [request-ch result-ch output-ch exception-ch timeout-ch]}]
  (go-loop [state {:done [] :requested [] :to-do query}]
    (doseq [to-do (all-that-can-request state)]
      (go (>! request-ch {:to-do to-do :state state})))
    (let [new-state (update-state state (<! result-ch))]
      (cond
        (all-done? new-state) (do (>! output-ch (:done new-state))
                                  (mapv close! [request-ch result-ch output-ch exception-ch]))
        (some chan-closed? [exception-ch timeout-ch]) (mapv close! [request-ch result-ch output-ch exception-ch])
        :else (recur new-state)))))

; ######################################; ######################################

(defn- log-if-408-or-aborted [result uid resource]
  (let [status (:status result)]
    (cond
      (= status 408) (log/warn {:session uid :resource resource} "Request timed out")
      (nil? status)  (log/warn {:session uid :resource resource} "Request aborted")
      :else          :no-action)))

(defn- log-status [result uid resource]
  "in case of result being a list, for multiplexed calls"
  (if (sequential? result)
    (doall (map #(log-if-408-or-aborted % uid resource) (flatten result)))
    (log-if-408-or-aborted result uid resource)))

(defn- generate-uuid! []
  #?(:clj (.toString (java.util.UUID/randomUUID))
     :cljs (.toString (uuid4))))

(defn- build-and-execute [mappings encoders {:keys [to-do state]} query-opts uuid result-ch exception-ch]
  (go
    (let [[query-name statement] to-do
          from (:from (second statement))
          result (->
                  (statement/build mappings statement (:done state) encoders)
                  (executor/do-request exception-ch query-opts)
                  (<!))]
      (log-status result uuid from)
      (>! result-ch (vector query-name result)))))

(defn- make-requests
  "goroutine that keeps listening from request-ch and performs http requests
   sending their result to result-ch"
  [mappings encoders {:keys [request-ch result-ch exception-ch]} query-opts]
  (go-loop [next-req (<! request-ch)
            uuid  (generate-uuid!)]
    (build-and-execute mappings encoders next-req query-opts uuid result-ch exception-ch)
    (let [request (<! request-ch)]
      (when request (recur request uuid)))))

; ######################################; ######################################

(defn run [mappings output-ch exception-ch timeout-ch query encoders {:keys [_debugging] :as query-opts}]
  (let [chans {:request-ch   (chan)
               :result-ch    (chan)
               :exception-ch exception-ch
               :output-ch output-ch
               :timeout-ch timeout-ch}]
    (make-requests mappings encoders chans query-opts)
    (do-run query chans)))
