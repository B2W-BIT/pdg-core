(ns restql.core.statement.resolve-chained-values
  (:require [restql.core.statement.explode-list-vals-in-new-maps :refer [explode-list-vals-in-new-maps]]
            [restql.core.util.deep-merge :refer [deep-merge]]
            [restql.core.util.get-in-with-list-support :refer [get-in-with-list-support]]))

(defn- get-chained-params [chained]
  (cond
    (and (sequential? chained) (sequential? (first chained))) [(get-chained-params (first chained))]
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

(defn- get-value-from-body [path body]
  (if (sequential? body)
    (map #(get-in-with-list-support path %) body)
    (get-in-with-list-support path body)))

(defn- nil-or-empty? [value]
  (or (nil? value)
      (and (string? value) (empty? value))
      (and (sequential? value) (empty? (flatten value)))))

(defn- get-value-from-body-or-headers [path body headers]
  (let [value-from-body (get-value-from-body path body)
        value-from-headers ((first path) headers)]
    (if (and value-from-headers
             (nil-or-empty? value-from-body))
      value-from-headers
      value-from-body)))

(defn- get-value-from-path [path {status :status body :body headers :headers}]
  (if (and status (not (>= 399 status 200)))
    :empty-chained
    (get-value-from-body-or-headers path body headers)))

(defn- get-value-from-resource-list [path resource]
  (if (sequential? resource)
    (->> resource (map #(get-value-from-resource-list path %)) (vec))
    (get-value-from-path path resource)))

(defn- get-chain-value-from-done-requests [[resource-name & path] done-requests]
  (let [resource (->> done-requests
                      (filter (fn [[key _]] (= key resource-name)))
                      first
                      second)]
    (if (sequential? resource)
      (->> resource (map #(get-value-from-resource-list path %)) (vec))
      (get-value-from-path path resource))))

(defn- meta-available? [object]
  (instance? clojure.lang.IMeta object))

(defn- has-meta? [object]
  (some? (meta object)))

(defn- get-param-value [done-requests chain]
  (if (sequential? (first chain))
    [(get-param-value done-requests (first chain))]
    (->
     (get-chain-value-from-done-requests chain done-requests)
     (as-> value
           (if (and (has-meta? chain) (meta-available? value))
             (with-meta value (meta chain))
             value)))))

(defn- assoc-value-to-param [done-requests [param-name chain]]
  (if (map? chain)
    (assoc {} param-name (->> chain (map #(assoc-value-to-param done-requests %)) (into {})))
    (assoc {} param-name (get-param-value done-requests chain))))

(defn- merge-chained-with-done-requests [done-requests params]
  (map (partial assoc-value-to-param done-requests) params))

(defn- merge-params-with-statement [statement params]
  (->>
   params
   (map (fn [[k v]] (if (map? v)
                      [k (explode-list-vals-in-new-maps v)]
                      [k v])))
   (into {})
   (deep-merge statement)))

(defn- do-resolve [statement done-requests]
  (->> statement
       (:with)
       (get-chained-params)
       (merge-chained-with-done-requests done-requests)
       (into {})
       (merge-params-with-statement (:with statement))
       (assoc statement :with)))

(defn resolve-chained-values [statement done-requests]
  (if (has-chained-value? statement)
    (do-resolve statement done-requests)
    statement))
