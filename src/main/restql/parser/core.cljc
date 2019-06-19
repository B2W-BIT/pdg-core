(ns restql.parser.core
  (:require [instaparse.core :as insta]
            [restql.core.cache :as cache]
            [restql.parser.printer :refer [pretty-print]]
            [restql.parser.producer :refer [produce *restql-variables*]]
            [clojure.tools.reader.edn :as edn]
            #?(:clj [clojure.java.io :as io]
               :cljs ["fs"])
            #?(:cljs ["path"]))
  #?(:clj (:use [slingshot.slingshot :only [throw+]])))

#?(:clj (def query-parser
          (insta/parser (io/resource "grammar.ebnf") :output-format :enlive))
   :cljs (def query-parser
           (insta/parser (fs/readFileSync (or process.env.grammar_ebnf (path/join (js* "__dirname") "./grammar.ebnf")) "utf8") :output-format :enlive)))

(defn handle-success
  "Handles parsing success"
  [result & {:keys [pretty]}]

  (if pretty
    (pretty-print result)
    result))

(defn handle-error
  "Handles any parsing errors"
  [result]

  (let [error (insta/get-failure result)]
    #?(:clj (throw+ {:type :parse-error
                     :reason (:reason error)
                     :line (:line error)
                     :column (:column error)})
       :cljs (throw            {:type :parse-error
                                :reason (:reason error)
                                :line (:line error)
                                :column (:column error)}))))

(defn- escape-double-quotes
  "Escape double quotes in params to prevent parsing errors"
  [param]
  (if (string? param)
    (clojure.string/escape param {\" "\\\""})
    param))

(defn escape-context-values
  "Returns context with escaped values"
  [context]
  (reduce (fn [map [key value]] (assoc map key (escape-double-quotes value))) {} context))

(defn handle-produce
  "Produces the EDN query of a given restQL query"
  [tree context]

  (let [escaped-ctx (escape-context-values context)]
    (binding [*restql-variables* (if (nil? escaped-ctx) {} escaped-ctx)]
      (-> (produce tree)
          (edn/read-string)))))

(def query-parser-cache (cache/cached (fn [query-text]
                                        (query-parser query-text))))

(defn parse [query-text query-type]
  (if (= :ad-hoc query-type)
    (query-parser query-text)
    (query-parser-cache query-text)))

(defn parse-query
  "Parses the restQL query"
  [query-text & {:keys [pretty context query-type]}]
  (let [result (parse query-text query-type)]
    (if (insta/failure? result)
      (handle-error result)
      (handle-success (handle-produce result context) :pretty pretty))))