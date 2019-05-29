(ns restql.core.cache
  (:require [environ.core :refer [env]]
            ["lru-cache" :as cache]))

(def DEFAULT_CACHED_COUNT (if (contains? env :cache-count) (cljs.reader/read-string (env :cache-count)) 2000))

(defonce LRU (cache. DEFAULT_CACHED_COUNT))

(defn cached
  "Verifies if a given function is cached, executing and saving on the cache
     if not cached or returning the cached value"
  [function]
  (fn  [param]
    (if-let [result (.get LRU param)]
      result
      (let [result (function param)]
        (.set LRU param result)
        result))))