(ns restql.parser.only-rule-formatter
  (:refer-clojure :exclude [format])
  (:require [schema.core :as s]
            [clojure.string :refer [join]]))

(def Modifier {s/Keyword s/Any})

(def ProducedItem
  {:path [s/Str]
   :modifiers [Modifier]})

(def TreeNode
  {:root s/Str
   :modifiers [Modifier]
   :inner [(s/recursive #'TreeNode)]})

(declare initialize-tree)

(s/defn get-root-path :- (s/maybe s/Str)
  [produced-item :- ProducedItem]
  (let [path (:path produced-item)]
    (first path)))

(s/defn has-root? :- s/Bool
  [root :- s/Str
   item :- ProducedItem]
  (= root (get-root-path item)))

(s/defn get-root-items :- [ProducedItem]
  [produced-items :- [ProducedItem]
   root :- s/Str]
  (filter (partial has-root? root)  produced-items))

(s/defn get-root-modifiers :- [Modifier]
  [produced-items :- [ProducedItem]
   root :- s/Str]
  (let [root-items (get-root-items produced-items root)]
    (->> root-items
         (map :modifiers)
         flatten)))

(s/defn remove-root :- ProducedItem
  [root :- s/Str
   item :- ProducedItem]
  (update item :path rest))

(s/defn create-tree-node :- TreeNode
  [produced-items :- [ProducedItem]
   root :- s/Str]
  {:root root
   :modifiers (get-root-modifiers produced-items root)
   :inner (->> produced-items
               (filter (partial has-root? root))
               (map (partial remove-root root))
               initialize-tree)})

(s/defn initialize-tree :- TreeNode
  [produced-items :- [ProducedItem]]

  (let [roots (->> produced-items (map get-root-path) (into #{}) (filter (complement nil?)))
        nodes (map (partial create-tree-node produced-items) roots)]
    nodes))

(s/defn simple-tree-node? :- s/Bool
  [tree :- TreeNode]
  (and
   (= [] (:modifiers tree))
   (= [] (:inner tree))))

(s/defn produce-modifier-item :- s/Str
  [[key value] :- [s/Keyword s/Any]]
  (str "{" key " " value "}"))

(s/defn produce-modifier :- s/Str
  [modifier :- Modifier]
  (->> modifier
       (into [])
       (map produce-modifier-item)
       (join " ")))

(s/defn produce-modifiers :- s/Str
  [tree :- TreeNode]
  (if (seq (:modifiers tree))
    (->> (:modifiers tree)
         (map produce-modifier)
         (join " ")
         (str " "))
    ""))

(declare produce-only-tree)

(s/defn produce-subitems :- s/Str
  [tree :- TreeNode]
  (if (seq (:inner tree))
    (str " #{" (join " " (map produce-only-tree (:inner tree)))   "}")
    ""))

(s/defn produce-only-tree :- s/Str
  [tree :- TreeNode]
  (cond
    (simple-tree-node? tree) (str ":" (:root tree))
    :else (str "[:" (:root tree) (produce-modifiers tree) (produce-subitems tree) "]")))

(s/defn format :- s/Str
  [produced-items :- [ProducedItem]]
  (let [tree (initialize-tree produced-items)
        generated-parts (map produce-only-tree tree)
        generated-string (str ":select #{" (join " " generated-parts) "}")]
    generated-string))

(comment
  (s/set-fn-validation! true)
  (s/set-fn-validation! false))
