(ns restql.parser.producer
  (:require [clojure.string :refer [join]]
            [restql.parser.only-rule-formatter :as only]
            [clojure.tools.reader :as edn]))

(def ^:dynamic *restql-variables* {})

(declare produce)

(defn find-first [tag content]
  (first (filter (fn [item] (= tag (:tag item))) content)))

(defn join-chars [prefix content]
  (str prefix (join "" content)))

(defn produce-query [blocks]
  (let [use-block   (->> blocks (find-first :UseBlock) produce)
        query-block (->> blocks (find-first :QueryBlock) produce)]
    (str use-block query-block)))

(defn produce-use-block [content]
  (let [produced-use-items (map produce content)]
    (str "^{" (join " " produced-use-items) "} ")))

(defn produce-use-rule [content]
  (let [rule-key   (->> content (find-first :UseRuleKey) produce)
        rule-value (->> content (find-first :UseRuleValue) produce)]
    (str rule-key " " rule-value)))

(defn produce-query-block [query-items]
  (let [produced-query-items (map produce query-items)]
    (str "[" (join "\n" produced-query-items)  "]")))


(defn produce-query-item [query-clauses]
  (let [resource          (->> query-clauses (find-first :FromResource) produce)
        method            (->> query-clauses (find-first :HttpMethod) produce)
        alias-rule        (->> query-clauses (find-first :ResultAlias))
        alias             (if (nil? alias-rule) resource (produce alias-rule))
        header-rule       (->> query-clauses (find-first :HeaderRule) produce)
        timeout-rule      (->> query-clauses (find-first :TimeoutRule) produce)
        with-rule         (->> query-clauses (find-first :WithRule) produce)
        only-rule         (->> query-clauses (find-first :OnlyRule) produce)
        hide-rule         (->> query-clauses (find-first :HideRule) produce)
        flags-rule        (->> query-clauses (find-first :FlagsRule) produce)
        ]
    (str alias
         flags-rule
         " {:from " resource header-rule timeout-rule with-rule only-rule hide-rule " :method " method "}")))

(defn produce-header-rule [content]
  (let [produced-header-items (map produce content)]
    (str " :with-headers {" (join " " produced-header-items) "}")))

(defn produce-header-rule-item [content]
  (let [produced-header-name  (->> content (find-first :HeaderName) produce)
        produced-header-value (->> content (find-first :HeaderValue) produce)]
    (str produced-header-name " " produced-header-value)))

(defn produce-header-name [content]
  (str "\"" (join-chars "" content) "\""))

(defn produce-header-value [content]
  (-> content first produce))

(defn produce-timeout-rule [content]
  (let [value (->> content (find-first :TimeoutRuleValue) produce)]
    (str " :timeout " value)))

(defn produce-with-rule [with-rule-items]
  (let [produced-items (map produce with-rule-items)]
    (str " :with {" (join " " produced-items) "}")))

(defn produce-with-rule-item [with-rule-item]
  (let [item-key   (->> with-rule-item (find-first :WithParamName) produce)
        item-value (->> with-rule-item (find-first :WithParamValue) produce)]
    (str item-key " " item-value)))

(defn produce-with-rule-item-variable [with-rule-item]
  (let [item (get *restql-variables* (join "" (:content (find-first :Variable with-rule-item))))]
    (join " " (map (fn [[key val]] (str ":"key " " val)) item))
  ))

(defn produce-with-rule-item-conditional [with-rule-item]
  (if (find-first :Variable with-rule-item) 
    (produce-with-rule-item-variable with-rule-item)
    (produce-with-rule-item with-rule-item)
  ))

(defn produce-with-param-value [with-param-value]
  (let [value     (->> with-param-value (find-first :WithParamValueData) produce)
        modifiers (->> with-param-value (find-first :WithParamValueModifierList) produce)]
    (str modifiers value)))

(defn produce-with-param-modifier-list [param-modifiers]
  (if (nil? param-modifiers) ""
    (let [modifiers (map produce param-modifiers)]
      (str "^" (pr-str (into {} modifiers)) " "))))

(defn produce-with-param-modifier [param-modifier]
  (produce (first param-modifier)))

(defn produce-with-modifier-alias [content]
  (let [alias (join-chars "" content)]
    (case alias
      "flatten"            {:expand false}
      "contract"           {:expand false}
      "expand"             {:expand true}
      {:encoder (keyword alias)})))

(defn produce-with-modifier-function [content]
  (let [fn-name (->> content (find-first :WithModifierFunctionName) produce)
        fn-args (->> content (find-first :WithModifierFunctionArgList) produce)]
    {:encoder (keyword fn-name)
     :args fn-args}))

(defn product-with-modifier-function-arg-list [content]
  (into [] (map produce content)))

(defn produce-primitive-value [content]
  (let [data (first content)]
    (cond
      (nil? data)               ""
      (= :True  (:tag data)) "true"
      (= :False (:tag data)) "false"
      (= :Null  (:tag data)) "nil"
      :else                  (join-chars "" content))))

(defn produce-list-value [content]
  (let [produced-values (map produce content)]
    (str "[" (join " " produced-values) "]")))

(defn produce-complex-value [content]
  (let [values (map produce content)]
    (str "{" (join " " values) "}")))

(defn produce-complex-param-item [content]
  (let [the-key   (->> content (find-first :ComplexParamKey) produce)
        the-value (->> content (find-first :WithParamValue) produce)]
    (str the-key " " the-value)))

(defn produce-chaining [path-items]
  (let [produced-path-items (map produce path-items)]
    (str "[" (join " " produced-path-items) "]")))


(defn format-variable [value]
  (cond
    (nil? value) "nil"
    (sequential? value) (str "[" (->> value (map format-variable) (join " ") ) "]")
    (= "true" value) "true"
    (= "false" value) "false"
    :else (str "\"" value "\"")))

(defn produce-variable [content]
  (let [varname (join "" content)
        value (get *restql-variables* varname)
       ]
    (format-variable value)))

(defn produce-with-param-value-data [value-data]
  (produce (first value-data)))

(defn produce-hide-rule []
  " :select :none")

(defn produce-only-rule [only-rule-items]
  (let [produced-items (map produce only-rule-items)]
    (str " " (only/format produced-items))))

(defn produce-only-rule-item [only-rule-item]
  (let [item-name (->> only-rule-item (find-first :OnlyRuleItemName) produce)
        modifiers (->> only-rule-item (find-first :OnlyRuleItemModifierList) produce)]
    {:path item-name
     :modifiers modifiers}))

(defn produce-only-rule-item-name [content]
  (map produce content))

(defn produce-only-rule-item-modifer-list [modifier-list]
  (map produce modifier-list))

(defn produce-only-rule-item-modifier [modifier]
  (let [name (->> modifier (find-first :OnlyRuleItemModifierName) produce)
        args (->> modifier (find-first :OnlyRuleItemModifierArgList) produce)]
    (hash-map (keyword name) args)))

(defn produce-only-rule-item-modifier-arg-list [arg-list]
  (let [produced-args (map produce arg-list)]
    (if (= 1 (count produced-args))
      (first produced-args)
      (str "[" (join " " produced-args) "]"))))

(defn produce-only-rule-with-variable [content]
  (-> (find-first :Variable content)
      (produce)))

(defn produce-flags-rule [content]
  (let [flags (map produce content)]
    (str " ^{" (join " " flags) "}")))

(defn produce-flag-rule [content]
  (-> content first produce))

(defn produce-ignore-error-flag []
  ":ignore-errors \"ignore\"")

(defn produce-http-method [content]
  (let [content-str (join-chars "" content)]
    (cond
      (= "from" content-str) ":get"
      (= "to" content-str) ":post"
      (= "into" content-str) ":put"
      (= "delete" content-str) ":delete"
      :else ":get")))

(defn produce
  "Produces a query EDN of a restQL grammar tree"
  [tree]

  (if (nil? tree) ""
    (let [{:keys [tag content]} tree]
      (case tag
      :Query                       (produce-query content)

      :UseBlock                    (produce-use-block content)
      :UseRule                     (produce-use-rule content)
      :UseRuleKey                  (join-chars ":" content)
      :UseRuleValue                (join-chars "" content)

      :QueryBlock                  (produce-query-block content)
      :QueryItem                   (produce-query-item content)

      :FromResource                (join-chars ":" content)

      ; Keeping from for "get" default for backwards compatibility reasons
      :HttpMethod                  (produce-http-method content)
      :ResultAlias                 (join-chars ":" content)

      :HeaderRule                  (produce-header-rule content)
      :HeaderRuleItem              (produce-header-rule-item content)
      :HeaderName                  (produce-header-name content)
      :HeaderValue                 (produce-header-value content)
      :LiteralHeaderValue          (join-chars "" content)

      :TimeoutRule                 (produce-timeout-rule content)
      :TimeoutRuleValue            (join-chars "" content)

      :WithRule                    (produce-with-rule content)
      :WithRuleItem                (produce-with-rule-item-conditional content)
      :WithParamName               (join-chars ":" content)
      :WithParamValue              (produce-with-param-value content)
      :WithParamValueData          (produce-with-param-value-data content)
      :WithParamPrimitiveValue     (produce-primitive-value content)
      :ListParamValue              (produce-list-value content)
      :ComplexParamValue           (produce-complex-value content)
      :Chaining                    (produce-chaining content)
      :Variable                    (produce-variable content)
      :PathItem                    (join-chars ":" content)

      :WithParamValueModifierList  (produce-with-param-modifier-list content)
      :WithParamModifier           (produce-with-param-modifier content)
      :WithModifierAlias           (produce-with-modifier-alias content)
      :WithModifierFunction        (produce-with-modifier-function content)
      :WithModifierFunctionName    (join-chars "" content)
      :WithModifierFunctionArgList (product-with-modifier-function-arg-list content)
      :WithModifierFunctionArg     (edn/read-string (join-chars "" content))

      :ComplexParamItem            (produce-complex-param-item content)
      :ComplexParamKey             (join-chars ":" content)

      :HideRule                    (produce-hide-rule)

      :OnlyRule                    (produce-only-rule content)
      :OnlyRuleItem                (produce-only-rule-item content)
      :OnlyRuleItemName            (produce-only-rule-item-name content)
      :OnlyRuleItemPath            (join-chars "" content)
      :OnlyRuleItemModifierList    (produce-only-rule-item-modifer-list content)
      :OnlyRuleItemModifier        (produce-only-rule-item-modifier content)
      :OnlyRuleItemModifierName    (join-chars "" content)
      :OnlyRuleItemModifierArgList (produce-only-rule-item-modifier-arg-list content)
      :OnlyRuleItemModifierArg     (join-chars "" content)
      :OnlyRuleItemModifierArgVar  (produce-only-rule-with-variable content)

      :FlagsRule                   (produce-flags-rule content)
      :FlagRule                    (produce-flag-rule content)
      :IgnoreErrorsFlag            (produce-ignore-error-flag)

      (str "<UNKNOWN RULE>" (pr-str {:tag tag :content content}))))))
