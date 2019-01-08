(ns restql.core.statement
  (:require [restql.core.util.deep-merge :refer [deep-merge]]
            [restql.core.util.update-in-seq :refer [update-in-seq]]
            [restql.core.encoders.core :as encoder])
)

(defn- filter-params-with-list-value [params]
  (filter #(sequential? (second %)) params)
)

(defn- filter-expandable-params [statement]
  (->> (:with statement)
       (filter-params-with-list-value)
       (filter #(not (false? (get (meta (second %)) :expand))))
       (into {})
  )
)

(defn- has-expandable-param? [statement]
  (->> statement
       (filter-expandable-params)
       (count)
       (not= 0)
  )
)

(defn- should-expand? [params]
  (every? not-empty (vals params))
)

(defn- remove-first-values [list-params]
  (update-in-seq list-params (keys list-params) rest)
)

(defn- get-first-values [list-params]
  (update-in-seq list-params (keys list-params) first)
)

; Sad but true
(declare do-expand)
(defn- create-expanded-statement [statement list-params]
  (let [new-statement (->> (get-first-values list-params)
                           (merge (:with statement))
                           (assoc statement :with))]
       (if (some vector? (vals (get-first-values list-params)))
           (do-expand new-statement)
           new-statement
       )
  )
)

(defn do-expand [statement]
   (loop [expanded-statement []
          list-params (filter-expandable-params statement)]
      (if (should-expand? list-params)
          (recur (conj expanded-statement
                       (create-expanded-statement statement list-params))
                 (remove-first-values list-params))
          expanded-statement
      )
   )
)

(defn expand [statement]
  (if (has-expandable-param? statement)
      (do-expand statement)
      (conj [] statement)
  )
)

(defn- get-chained-params [statement]
  (->> (:with statement)
       (filter (fn [[_ values]] (sequential? values)))
       (filter (fn [[_ values]] (every? keyword? values)))
       (into {})
  )
)


(defn- has-chained-value? [statement]
  (->> statement
       (get-chained-params)
       (count)
       (not= 0)
  )
)

(defn get-field-from-statement [statement]
  (->> statement
    (:with)
    (keys)
    (first)
  )
)

(defn get-resource-name-from-statement [statement]
  (-> (:with statement)
      (get (get-field-from-statement statement))
      (second)
  )
)

(defn get-value-from-resource-id [state resource-id]
  (->>  state
        (:done)
        (map #(apply assoc {} %))
        (map :done-resource)
        (map :body)
        (map resource-id)
        (first)))

(defn get-value-from-path [path {body :body}]
  (if (sequential? body) 
    (->> body  
        (map #(get-in % path)) 
        (vec))
    (get-in body path)))

(defn- get-chain-value-from-done-state [[resource-name & path] state]
  (->> (:done state)
       (filter (fn [[key _]] (= key resource-name)))
       (map second)
       (flatten)
       (map (partial get-value-from-path path))
       (into [])
  )
)

(defn meta-available? [object]
  (instance? clojure.lang.IMeta object)
)

(defn has-meta? [object]
  (some? (meta object))
)

(defn get-param-value [state chain]
  (->
    (get-chain-value-from-done-state chain state)
    (as-> value
          (if (and (has-meta? chain) (meta-available? value))
              (with-meta value (meta chain))
              value
          )
    )
  )
)

(defn assoc-value-to-param [state [param-name chain]]
  (assoc {} param-name (get-param-value state chain))
)

(defn- merge-chained-with-state [state params]
  (map (partial assoc-value-to-param state) params)
)

(defn- merge-params-with-statement [statement params]
  (->> (assoc {} :with params)
       (deep-merge statement)
  )
)

(defn- do-resolve [statement state]
  (->> statement
       (get-chained-params)
       (merge-chained-with-state state)
       (into {})
       (merge-params-with-statement statement)
  )
)

(defn resolve-chained-values [statement state]
  (if (has-chained-value? statement)
      (do-resolve statement state)
      statement
  )
)

(defn- encode-param-value [encoders [param-key param-value]]
  (assoc {} param-key (encoder/encode encoders param-value))
)

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
