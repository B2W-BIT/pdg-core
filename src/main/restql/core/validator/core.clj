(ns restql.core.validator.core
  (:require [restql.core.validator.util :refer [defrules rule]]
            [restql.core.encoders.core :as encoders]
            [clojure.set :as s]
            [restql.core.query :as query])
  (:use [slingshot.slingshot :only [throw+ try+]])
)

(defn without-from [[_ data]]
  (not (contains? data :from)))

(defn invalid-data-key [key]
  (not
    (#{:from :in :method :with :with-headers :with-body :timeout :select} key)))

(defn keyword-or-vector? [value]
  (or
    (keyword? value)
    (vector? value)))

(defn get-bindings [q]
  (->> q
       (partition 2)
       (map first)
       (into #{})))

(defn is-mapped [mappings resource]
  (not
    (nil? (get mappings resource))))

(defn all-keys-are-keywords [a-map]
  (->> a-map keys (filter (complement keyword?)) count (= 0)))

(defn valid-encoders [custom-encoders-map]
  (let [base-encoders (->> (encoders/get-default-encoders) keys (into #{}))
        custom-encoders (->> custom-encoders-map keys (into #{}))]
    (into base-encoders custom-encoders)))

(defn valid-timeout [data]
  (if (contains? data :timeout)
    (if (-> data :timeout number?)
      (and
        (-> data :timeout (<= 5000))
        (-> data :timeout (> 0)))
      false)
    true))

(defn is-valid-select [value]
  (or
    (= :none value)
    (set? value)))

(defn is-valid-url [url]
    (try+
        (clojure.java.io/as-url url)
        true
    (catch Object _
        false
    ))
)

(defn get-urls [mappings resources]
    (map (partial get mappings) resources)
)

(defrules validate

  (rule "Query must be written between square brackets"
        [context q]
        (vector? q))

  (rule "Query must not be empty"
        [context q]
        (-> q count (not= 0)))

  (rule "All query items must have a binding and a query item data"
        [context q]
        (-> q count (mod 2) (= 0)))

  (rule "Query has invalid bindings. Error was in |:invalid|. Use keywords instead"
        [context q]
        (let [invalids (->> q
                         (partition 2)
                         (map first)
                         (filter (complement keyword?)))]
          (if (= 0 (count invalids))
            true
            {:invalid (-> invalids first pr-str)})))

  (rule "Query must not have repeated bindings. Error was in |:invalid|"
        [context q]
        (let [duplicates (for [[id freq] (frequencies (map first (partition 2 q))) :when (> freq 1)]
                           id)]
          (if (= 0 (count duplicates))
            true
            {:invalid (-> duplicates first pr-str)})))

  (rule "Query item data must be a map. Error was in :invalid"
        [context q]
        (let [invalids (->> q (partition 2) (filter (fn [[_ data]] (not (map? data)))))]
          (if (= 0 (count invalids))
            true
            {:invalid (-> invalids first pr-str)})))

  (rule "Query item data must have a :from key. Found error in :invalid"
        [context q]
        (let [invalids (->> q (partition 2) (filter without-from))]
          (if (= 0 (count invalids))
            true
            {:invalid (-> invalids first pr-str)})))

  (rule "Query item data must have only valid keys. Invalid key found was |:invalid|"
        [context q]
        (let [invalids (->> q (partition 2) (map second) (mapcat keys) (filter invalid-data-key))]
          (if (= 0 (count invalids))
            true
            {:invalid (-> invalids first pr-str)})))

  (rule "from key in Query Item Data should be a keyword or a vector. Invalid was |:invalid|"
        [context q]
        (let [invalids (->> q (partition 2) (map second) (map :from) (filter (complement keyword-or-vector?)))]
          (if (= 0 (count invalids))
            true
            {:invalid (-> invalids first pr-str)})))

  (rule "from as a vector should reference a valid binding. Error was in :invalid"
        [context q]
        (let [invalids (->> q
                            (partition 2)
                            (map second)
                            (map :from)
                            (filter vector?)
                            (map first)
                            (filter (complement (get-bindings q))))]
          (if (= 0 (count invalids))
            true
            {:invalid (-> invalids first pr-str)})))

  (rule "from as a keyword should reference a valid mapped resource. Error was in :invalid"
        [context q]
        (let [invalids (->> q
                            (partition 2)
                            (map second)
                            (map :from)
                            (filter keyword?)
                            (filter (complement (partial is-mapped (:mappings context)))))]
          (if (= 0 (count invalids))
            true
            {:invalid (-> invalids first pr-str)})))

  (rule "from as a keyword should resolve to a valid URL. Error was in URL :invalid"
        [context q]
        (let [invalids (->> q
                            (partition 2)
                            (map second)
                            (map :from)
                            (filter keyword?)
                            (get-urls (:mappings context))
                            (filter (complement is-valid-url)))]
            (if (= 0 (count invalids))
                true
                {:invalid (-> invalids first pr-str)})))

  (rule "with must be a map or not be used. Found a problem in :invalid"
        [context q]
        (let [invalids (->> q
                            (partition 2)
                            (map second)
                            (map :with)
                            (filter (complement nil?))
                            (filter (complement map?)))]
          (if (= 0 (count invalids))
            true
            {:invalid (-> invalids first pr-str)})))

  (rule "with must be a map with keywords as keys. Error was in :invalid"
        [context q]
        (let [invalids (->> q
                            (partition 2)
                            (map second)
                            (map :with)
                            (filter map?)
                            (filter (complement all-keys-are-keywords)))]
          (if (= 0 (count invalids))
            true
            {:invalid (-> invalids first pr-str)})))

  (rule "A query should only use valid encoders. Found error in :invalid"
        [context q]
        (let [invalids (->> q
                            (partition 2)
                            (map second)
                            (map :with)
                            (mapcat vals)
                            (filter coll?)
                            (map meta)
                            (map :encoder)
                            (filter (complement nil?))
                            (filter (complement (valid-encoders (:encoders context)))))]
          (if (= 0 (count invalids))
            true
            {:invalid (-> invalids first pr-str)})))

  (rule "A query must have a :timeout value between 1 and 5000 or not have it. Error found in :invalid"
        [context q]
        (let [invalids (->> q
                            (partition 2)
                            (map second)
                            (filter (complement valid-timeout)))]
          (if (= 0 (count invalids))
            true
            {:invalid (-> invalids first pr-str)})) )

  (rule "When a query has a select key, it must be a set or :none. Error found in :invalid"
        [context q]
        (let [invalids (->> q
                            (partition 2)
                            (map second)
                            (map :select)
                            (filter (complement nil?))
                            (filter (complement is-valid-select)))]
          (if (= 0 (count invalids))
            true
            {:invalid (-> invalids first pr-str)})))

  (rule "All chainings of a query must reference a valid binding. Found error in |:invalid|"
        [context q]
        (let [chainings (->> q (partition 2) (map query/get-dependencies) (apply s/union))
              bindings (get-bindings q)
              invalids (filter (complement bindings) chainings)]
          (if (= 0 (count invalids))
            true
            {:invalid (-> invalids first pr-str)}))))
