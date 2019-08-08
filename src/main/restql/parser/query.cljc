(ns restql.parser.query
  (:require [instaparse.core :as insta :refer [defparser]]
            #?(:clj [clojure.java.io :as io]
               :cljs ["fs"])
            #?(:clj [slingshot.slingshot :refer [throw+]])
            #?(:cljs ["path"])))

#?(:clj  (defparser to-tree
           (io/resource "grammar.ebnf")
           :output-format :enlive)
   :cljs (defparser to-tree
           (fs/readFileSync (or process.env.grammar_ebnf
                                (path/join (js* "__dirname") "./grammar.ebnf"))
                            "utf8")
           :output-format :enlive))

(defn- handle-error [result]
  (let [error (insta/get-failure result)]
    #?(:clj (throw+ {:type :parse-error
                     :reason (:reason error)
                     :line (:line error)
                     :column (:column error)})
       :cljs (throw {:type :parse-error
                     :reason (:reason error)
                     :line (:line error)
                     :column (:column error)}))))

(defn- handle-result [result]
  (if (insta/failure? result)
    (handle-error result)
    result))

(defn from-text [query-text]
  (-> query-text
      (to-tree)
      (handle-result)))
