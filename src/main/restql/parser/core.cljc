(ns restql.parser.core
  (:require [restql.parser.query :as query]
            [restql.parser.statement.core :as statement]
            [restql.core.cache :as cache]))

(def parse-with-cache (cache/cached (fn [query-text]
                                      (query/from-text query-text))))

(defn- parse [query-type query-text]
  (if (= :ad-hoc query-type)
    (query/from-text query-text)
    (parse-with-cache query-text)))

(defn parse-query
  "Parses the restQL query"
  [query-text & {:keys [context query-type]}]
  (->> query-text
       (parse query-type)
       (statement/from-query context)))
