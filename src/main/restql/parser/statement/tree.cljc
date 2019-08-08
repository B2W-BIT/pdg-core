(ns restql.parser.statement.tree)

(defn first-of-tag [tag-name item]
  (->> item
       (:content)
       (filter #(= (:tag %) tag-name))
       (first)))

(defn- read-variable [context primitive-tag]
  (->> primitive-tag
       (:content)
       (first)
       (get context)))

(defn read-value [context primitive-tag]
  (cond
    (not (map? primitive-tag))             primitive-tag
    (= (:tag primitive-tag) :Chaining)     (->> primitive-tag :content (map (partial read-value context)) (into []))
    (= (:tag primitive-tag) :PathItem)     (->> primitive-tag :content first keyword)
    (= (:tag primitive-tag) :PathVariable) (->> primitive-tag (read-variable context) str keyword)
    (= (:tag primitive-tag) :True)         true
    (= (:tag primitive-tag) :False)        false
    (= (:tag primitive-tag) :Null)         nil
    (= (:tag primitive-tag) :Variable)     (->> primitive-tag (read-variable context))
    :else                                  (->> primitive-tag :content first read-string)))
