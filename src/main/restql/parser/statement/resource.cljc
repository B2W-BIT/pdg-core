(ns restql.parser.statement.resource
  (:require [restql.parser.statement.tree :as tree]
            [restql.parser.statement.modifier :as modifier]
            [clojure.walk :refer [keywordize-keys]]))

(def action-method {"from"   :get
                    "to"     :post
                    "into"   :put
                    "delete" :delete
                    "update" :patch})

(defn- with-modifier-functions [context item-value-tag resource-value]
  (if (or (sequential? resource-value)
          (map? resource-value))
    (->> item-value-tag
         (modifier/function context)
         (with-meta resource-value))
    resource-value))

(defn- resource-item-value [context resource-value-tag]
  (->> resource-value-tag
       (filter #(not= (:tag %) :ModifierList))
       (first)
       (tree/read-value context)
       (with-modifier-functions context resource-value-tag)))

(defn- resource-name [query-item]
  (->> query-item
       (tree/first-of-tag :ActionRule)
       (tree/first-of-tag :ActionRuleValue)
       (:content)
       (first)
       (keyword)))

(defn- resource-method [query-item]
  (->> query-item
       (tree/first-of-tag :ActionRule)
       (tree/first-of-tag :ActionRuleKey)
       (:content)
       (first)
       (action-method)))

(defn- resource-alias [query-item]
  (->> query-item
       (tree/first-of-tag :ActionRule)
       (tree/first-of-tag :ActionRuleAlias)
       (:content)
       (first)
       (keyword)))

(defn- resource-in [query-item]
  (->> query-item
       (tree/first-of-tag :ActionRule)
       (tree/first-of-tag :ActionRuleIn)
       (:content)
       (first)
       (keyword)))

(declare with-item-value)

(defn- list-param-value [context item-value]
  (->> item-value
       (:content)
       (first)
       (:content)
       (map (partial with-item-value context))
       (into [])
       (with-modifier-functions
         context
         (-> item-value :content))))

(defn- complex-item [context item-value]
  (assoc {}
         (->> item-value first :content first keyword)
         (->> item-value second (with-item-value context))))

(defn- complex-param-value [context item-value]
  (->> item-value
       (:content)
       (first)
       (:content)
       (map :content)
       (map (partial complex-item context))
       (into {})
       (with-modifier-functions
         context
         (-> item-value :content))))

(defn- simple-param-value [context item-value]
  (->> item-value
       (:content)
       (resource-item-value context)))

(defn- param-name [item-value]
  (-> item-value
      (:content)
      (first)
      (keyword)))

(defn- with-item-value [context item-value]
  (cond
    (= (-> item-value :tag) :WithParamName) (param-name item-value)
    (= (-> item-value :content first :tag) :ListParam) (list-param-value context item-value)
    (= (-> item-value :content first :tag) :ComplexParam) (complex-param-value context item-value)
    :else (simple-param-value context item-value)))

(defn- resource-with-item [context item]
  (if (= (-> item first :tag) :Variable)
    (->> item first (tree/read-value context) keywordize-keys)
    (->> item
         (map (partial with-item-value context))
         (apply assoc {}))))

(defn- resource-params [query-item context]
  (->> query-item
       (tree/first-of-tag :WithRule)
       (:content)
       (map :content)
       (map (partial resource-with-item context))
       (into {})))

(defn- resource-select-filter [context query-select-tag]
  (->> query-select-tag
       (filter #(not= (:tag %) :ModifierList))
       (map #(-> % :content))
       (flatten)
       (map keyword)
       (into [])
       (with-modifier-functions context query-select-tag)))

(defn- resource-select-filters [query-item context]
  (->> query-item
       (tree/first-of-tag :OnlyRule)
       (:content)
       (map :content)
       (map (partial resource-select-filter context))
       (into [])))

(defn- resource-hidden [query-item]
  (->> query-item
       (tree/first-of-tag :HideRule)
       (some?)))

(defn- resource-flag [flag-tag]
  (case (:tag flag-tag)
    :IgnoreErrorsFlag {:ignore-errors "ignore"}
    {}))

(defn- statement-from [query-item]
  (->> query-item
       (resource-name)
       (assoc {} :from)))

(defn- statement-method [query-item]
  (->> query-item
       (resource-method)
       (assoc {} :method)))

(defn- statement-in [query-item]
  (-> query-item
      (resource-in)
      (as-> in-value
            (when-not (nil? in-value) {:in in-value}))))

(defn- statement-params [context query-item]
  (-> query-item
      (resource-params context)
      (as-> with-value
            (when-not (empty? with-value) {:with with-value}))))

(defn- statement-select-filters [context query-item]
  (if (resource-hidden query-item)
    {:select :none}
    (-> query-item
        (resource-select-filters context)
        (as-> select-value
              (when-not (empty? select-value) {:select select-value})))))

(defn- with-resource-flags [resource-statement query-item]
  (->> query-item
       (tree/first-of-tag :FlagsRule)
       (:content)
       (map :content)
       (flatten)
       (map resource-flag)
       (into {})
       (with-meta resource-statement)))

(defn- with-resource-modifiers [resource-statement context query-item]
  (->> query-item
       (modifier/resource context)
       (conj resource-statement)))

(defn- resource-statement [context query-item]
  (-> {}
      (into (statement-from query-item))
      (into (statement-method query-item))
      (into (statement-in query-item))
      (into (statement-params context query-item))
      (into (statement-select-filters context query-item))
      (with-resource-modifiers context query-item)
      (with-resource-flags query-item)))

(defn- resource-item [context query-item]
  (-> []
      (conj (or (resource-alias query-item)
                (resource-name query-item)))
      (conj (resource-statement context query-item))))

(defn- flatten-statement [statements]
  (reduce (fn [acc [item-name item-statement]]
            (conj acc item-name item-statement))
          [] statements))

(defn from-query
  ([query-parsed] (from-query {} query-parsed))
  ([context query-parsed]
   (->> query-parsed
        (tree/first-of-tag :QueryBlock)
        (:content)
        (map (partial resource-item context))
        (flatten-statement))))
