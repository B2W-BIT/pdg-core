(ns restql.parser.statement.modifier-function
  (:require [restql.parser.statement.tree :as tree]))

(defn- translate-function-name [fn-name]
  (case (first fn-name)
    "flatten" :expand
    (-> fn-name first keyword)))

(defn- format-arg-list [arg-list]
  (if (= 1 (count arg-list))
    (first arg-list)
    (into [] arg-list)))

(defn- modifier-param [context modifier-item]
  (case (:tag modifier-item)
    :ModifierFunctionName (-> modifier-item :content translate-function-name)
    :ModifierFunctionArgList (->> modifier-item :content (map (partial tree/read-value context)) (format-arg-list))))

(defn- format-modifier-function [context modifier]
  (->> modifier
       (map (partial modifier-param context))
       (apply assoc {})))

(defn- format-modifier-function-alias [modifier]
  (case (-> modifier :content first)
    "flatten"  {:expand false}
    "contract" {:expand false}
    "expand"   {:expand true}
    "json"     {:encoder :json}
    "base64"   {:encoder :base64}
    {}))

(defn- modifier-function [context modifier]
  (when (-> modifier :content first some?)
    (case (:tag modifier)
      :ModifierFunction (format-modifier-function context (:content modifier))
      :ModifierAlias (format-modifier-function-alias modifier)
      {})))

(defn produce [context modifier-list]
  (->> modifier-list
       (map (partial modifier-function context))
       (into {})))

(defn from-item-value [item-value]
  (->> item-value
       (filter #(= (:tag %) :ModifierList))
       (first)
       (:content)
       (map :content)
       (flatten)))
