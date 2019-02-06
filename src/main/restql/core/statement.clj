(ns restql.core.statement
  (:require [restql.core.util.deep-merge :refer [deep-merge]]
            [restql.core.statement.explode-list-vals-in-new-maps :refer [explode-list-vals-in-new-maps]]
            [restql.core.util.update-in-seq :refer [update-in-seq]]
            [restql.core.encoders.core :as encoder]
            [restql.core.util.get-in-with-list-support :refer [get-in-with-list-support]]))

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

(defn do-expand [statement]
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

(defn- get-chained-params [chained]
  (cond
    (and (sequential? chained) (every? keyword? chained)) chained
    (map? chained) (->>
                    (map (fn [[k v]]
                           (let [chained-params (get-chained-params v)]
                             (when chained-params {k chained-params})))
                         chained)
                    (into {}))
    :else nil))

(defn- has-chained-value? [statement]
  (->> statement
       (:with)
       (get-chained-params)
       (count)
       (not= 0)))

(defn get-value-from-path [path {body :body}]
  (if (sequential? body)
    (->> body
         (map #(get-in-with-list-support path %)))
    (get-in-with-list-support path body)))

(defn get-value-from-resource-list [path resource]
  (if (sequential? resource)
    (->> resource (map #(get-value-from-resource-list path %)) (vec))
    (get-value-from-path path resource)))

(defn- get-chain-value-from-done-state [[resource-name & path] state]
  (let [resource (->> state
                      :done
                      (filter (fn [[key _]] (= key resource-name)))
                      first
                      second)]
    (if (sequential? resource)
      (->> resource (map #(get-value-from-resource-list path %)) (vec))
      (get-value-from-path path resource))))

(defn meta-available? [object]
  (instance? clojure.lang.IMeta object))

(defn has-meta? [object]
  (some? (meta object)))

(defn get-param-value [state chain]
  (->
   (get-chain-value-from-done-state chain state)
   (as-> value
         (if (and (has-meta? chain) (meta-available? value))
           (with-meta value (meta chain))
           value))))

(defn- assoc-value-to-param [state [param-name chain]]
  (if (map? chain)
    (assoc {} param-name (->> chain (map #(assoc-value-to-param state %)) (into {})))
    (assoc {} param-name (get-param-value state chain))))

(defn- merge-chained-with-state [state params]
  (map (partial assoc-value-to-param state) params))

(defn- merge-params-with-statement [statement params]
  (->>
   params
   (map (fn [[k v]] (if (map? v)
                      [k (explode-list-vals-in-new-maps v)]
                      [k v])))
   (into {})
   (deep-merge statement)))

(defn- do-resolve [statement state]
  (->> statement
       (:with)
       (get-chained-params)
       (merge-chained-with-state state)
       (into {})
       (merge-params-with-statement (:with statement))
       (assoc statement :with)))

(defn resolve-chained-values [statement state]
  (if (has-chained-value? statement)
    (do-resolve statement state)
    statement))

(defn- encode-param-value [encoders [param-key param-value]]
  (assoc {} param-key (encoder/encode encoders param-value)))

(defn encode-params [encoders statements]
  (map #(->> %
             (:with)
             (map (partial encode-param-value encoders))
             (into {})
             (assoc {} :with)
             (deep-merge  %)) statements))

(defn apply-encoders [encoders expanded-statements]
  (if (sequential? (first expanded-statements))
    (map (partial apply-encoders encoders) expanded-statements)
    (encode-params encoders expanded-statements)))
