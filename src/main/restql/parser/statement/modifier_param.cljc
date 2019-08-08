(ns restql.parser.statement.modifier-param
  (:require [restql.parser.statement.tree :as tree]))

(def modifier-name {:TimeoutRule :timeout
                    :MaxAgeRule  :max-age
                    :SMaxAgeRule :s-max-age
                    :HeaderRule  :with-headers})

(defn- header-param-item [context header-content]
  (->> header-content
       (map :content)
       (flatten)
       (map (partial tree/read-value context))
       (apply assoc {})))

(defn- headers-params [context modifier]
  (->> modifier
       (map :content)
       (map (partial header-param-item context))
       (into {})))

(defn- param-item [context modifier]
  (if-let [name (-> modifier :tag modifier-name)]
    (assoc {}
           name
           (if (= name :with-headers)
             (->> modifier :content (headers-params context))
             (->> modifier :content first (tree/read-value context))))
    {}))

(defn produce [context modifiers]
  (->> modifiers
       (map (partial param-item context))
       (into {})))

(defn use-modifiers [query-parsed]
  (->> query-parsed
       (tree/first-of-tag :UseBlock)
       (:content)
       (map :content)
       (flatten)
       (map :content)
       (flatten)))

(defn resource-modifiers [resource-item]
  (->> resource-item
       (tree/first-of-tag :ModifierBlock)
       (:content)))
