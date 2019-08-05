(ns restql.parser.statement.core
  (:require [restql.parser.statement.tree :as tree]
            [restql.parser.statement.modifier :as modifier]
            [restql.parser.statement.resource :as resource]))

(defn- with-global-modifiers [context query-parsed statement]
  (->> query-parsed
       (modifier/use context)
       (with-meta statement)))

(defn from-query
  ([query-parsed] (from-query {} query-parsed))
  ([context query-parsed]
   (->> query-parsed
        (resource/from-query context)
        (with-global-modifiers context query-parsed))))
