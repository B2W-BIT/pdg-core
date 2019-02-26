(ns restql.core.runner.core
  (:require [clojure.core.async :refer [go-loop go <! >! chan alt! timeout close!]]
            [clojure.tools.logging :as log]
            [clojure.set :as s]
            [restql.core.query :as query]
            [restql.core.runner.executor :as executor]))

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
  [query {:keys [request-ch result-ch]}]
  (go-loop [state {:done [] :requested [] :to-do query}]
    (doseq [to-do (all-that-can-request state)]
      (go
        (>! request-ch {:to-do to-do :state state})))
    (let [new-state (update-state state (<! result-ch))]
      (if (all-done? new-state)
        (do
          (close! request-ch)
          (:done new-state))
        (recur new-state)))))

; ######################################; ######################################

(defn- get-status-log [uid resource result]
  "get log message depending on the status code of result"
  (let [status (-> result second :status)]
    (cond
      (= status 408) (log/warn {:session uid :resource resource} "Request timed out")
      (nil? status)  (log/warn {:session uid :resource resource} "Request aborted")
      :else          :no-action)))

(defn- log-status [uid resource result]
  "in case of result being a list, for multiplexed calls"
  (if (sequential? result)
    (map #(get-status-log uid resource %) result)
    (get-status-log uid resource result)))

(defn- generate-uuid! []
  (.toString (java.util.UUID/randomUUID)))

(defn- make-requests
  "goroutine that keeps listening from request-ch and performs http requests
   sending their result to result-ch"
  [do-request encoders {:keys [request-ch result-ch exception-ch]} query-opts]
  (go-loop [next-req (<! request-ch)
            uuid  (generate-uuid!)]
    (go (let [result (<! (do-request next-req encoders exception-ch query-opts))]
          (log-status uuid (:from (second (second (first next-req)))) result)
          (>! result-ch result)))
    (if-let [request (<! request-ch)]
      (recur request
             uuid)
      (close! result-ch))))

; ######################################; ######################################

(defn run [mappings query encoders {:keys [_debugging] :as query-opts}]
  (let [chans {:request-ch   (chan)
               :result-ch    (chan)
               :exception-ch (chan)}]
    (make-requests (partial executor/do-request mappings) encoders chans query-opts)
    [(do-run query chans)
     (:exception-ch chans)]))
