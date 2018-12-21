(ns restql.core.query)

(declare extract-complex-dependencies)
(declare extract-dependencies)

(defn extract-complex-dependencies [structure]
  (let [values (vals structure)
        deps   (map extract-dependencies values)]
    (reduce into #{} deps)))

(defn vector-of-keywords? [value]
  (and
    (vector? value)
    (every? keyword? value)))

(defn extract-dependencies [value]
  (cond
    (vector-of-keywords? value) #{value}
    (map? value) (extract-complex-dependencies value)
    :else #{}))

(defn get-dependency-paths
  "given only a query-item-data ({:from ... :with ...}), returns a set of
   vectors with the dependency paths listed"
  [query-item-data]
  (if (vector? (:from query-item-data))
    #{(:from query-item-data)}
    (->> query-item-data
         :with
         vals
         (map extract-dependencies)
         (reduce into #{}))))

(defn get-dependencies
  "given a query-item, checks its :with parameter and returns a set
   of keywords with all dependencies"
  [[_query-item-name query-item-data]]
  (let [deps (get-dependency-paths query-item-data)]
    (->> deps (map first) (into #{}))))

(defn find-query-item
  "given a query-item-name (a keyword) returns the query-item (a pair with keyword and data)
   from state. State is a vector of pairs."
  [query-item-name state]
  (first (filter (fn [[k _]] (= query-item-name k)) state)))
