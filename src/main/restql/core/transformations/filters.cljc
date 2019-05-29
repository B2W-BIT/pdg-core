(ns restql.core.transformations.filters)

(defn use-field [fun]
  (fn [arg]
    (if (map? arg)
      (fn [data] (fun (:value arg) (get data (:field arg))))
      (fn [data] (fun arg data)))))

(defn vector-disjoint [fun]
  (fn [arg data]
    (if (vector? arg)
      (some #(fun % data) arg)
      (fun arg data))))

(defn filter-match [arg data]
  (boolean (re-find (re-pattern arg) (str data))))

(defn filter-equals [arg data]
  (= data arg))

(def filters {:matches (-> filter-match vector-disjoint use-field)
              :equals (-> filter-equals vector-disjoint use-field)})

(defn get-filter [[name arg]]
  (let [func (get filters name)]
    (func arg)))

(defn get-filters [a-filter]
  (map get-filter a-filter))

(defn reduce-with-or [bools]
  (reduce (fn [a b] (or a b)) false bools))

(defn combine-filters [filter-list]
  (fn [item]
    (reduce-with-or (map (fn [fun] (fun item)) filter-list))))

(defn apply-filter [data a-filter]
  (filter (combine-filters (get-filters a-filter)) data))

(defn apply-filters [data [a-filter & others]]
  (if (seq others)
    (recur (apply-filter data a-filter) others)
    (apply-filter data a-filter)))
