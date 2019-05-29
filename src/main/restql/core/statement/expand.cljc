(ns restql.core.statement.expand
  (:require [restql.core.util.update-in-seq :refer [update-in-seq]]))

(defn- filter-params-with-list-value [params]
  (filter #(sequential? (second %)) params))

(defn- filter-expandable-params [statement]
  (->> (:with statement)
       (filter-params-with-list-value)
       (filter #(not (false? (get (meta (second %)) :expand))))
       (into {})))

(defn- has-expandable-param? [statement]
  (->> statement
       (filter-expandable-params)
       (count)
       (not= 0)))

(defn- should-expand? [params]
  (every? not-empty (vals params)))

(defn- remove-first-values [list-params]
  (update-in-seq list-params (keys list-params) rest))

(defn- get-first-values [list-params]
  (update-in-seq list-params (keys list-params) first))

; Sad but true
(declare do-expand)
(defn- create-expanded-statement [statement list-params]
  (let [new-statement (->> (get-first-values list-params)
                           (merge (:with statement))
                           (assoc statement :multiplexed true :with))]
    (if (some vector? (vals (get-first-values list-params)))
      (do-expand new-statement)
      new-statement)))

(defn- do-expand [statement]
  (loop [expanded-statement []
         list-params (filter-expandable-params statement)]
    (if (should-expand? list-params)
      (recur (conj expanded-statement
                   (create-expanded-statement statement list-params))
             (remove-first-values list-params))
      expanded-statement)))

(defn expand [statement]
  (if (has-expandable-param? statement)
    (do-expand statement)
    (conj [] statement)))
