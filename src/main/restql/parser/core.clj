(ns restql.parser.core
  (:require [instaparse.core :as insta]
            [restql.core.cache :as cache]
            [restql.parser.printer :refer [pretty-print]]
            [restql.parser.producer :refer [produce *restql-variables*]]
            [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn])
  (:use     [slingshot.slingshot :only [throw+]]))

(def query-parser
  (insta/parser (io/resource "grammar.ebnf") :output-format :enlive))

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
    (throw+ {:type :parse-error
             :reason (:reason error)
             :line (:line error)
             :column (:column error)})))

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

(def parse-query-text (cache/cached (fn [query-text]
                                      (query-parser query-text))))

(defn parse-query
  "Parses the restQL query"
  [query-text & {:keys [pretty context]}]

  (let [result (parse-query-text query-text)]
    (if (insta/failure? result)
      (handle-error result)
      (handle-success (handle-produce result context) :pretty pretty))))