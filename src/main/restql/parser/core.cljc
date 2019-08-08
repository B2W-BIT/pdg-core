(ns restql.parser.core
  (:require [restql.parser.query :as query]
            [restql.parser.statement.core :as statement]
            [restql.core.cache :as cache]
            #?(:clj [slingshot.slingshot :refer [throw+]])))

(def parse-with-cache (cache/cached (fn [query-text]
                                      (query/from-text query-text))))

(defn- parse [query-type query-text]
  (if (= :ad-hoc query-type)
    (query/from-text query-text)
    (parse-with-cache query-text)))

(defn parse-query
  "Parses the restQL query"
  [query-text & {:keys [context query-type]}]
  (if (string? query-text)
    (->> query-text
         (parse query-type)
         (statement/from-query context))
    #?(:clj (throw+ {:type :parser-error :message "Parser error: invalid query format"})
       :cljs (throw {:type :parser-error :message "Parser error: invalid query format"}))))
