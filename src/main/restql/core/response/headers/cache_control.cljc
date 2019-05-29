(ns restql.core.response.headers.cache-control
  (:require [clojure.string :as string]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.set :refer [rename-keys]]))

(defn map-headers-to-aliases
  "Given a key-value pair, where key is the resource alias
   and value is it's value, extracts only the headers to a
   new map."
  [[k v]]
  (into {} {k (some-> v :details :headers)}))

(defn map-response-headers-to-aliases
  "Return a map of :resource headers"
  [response]
  (->>
   response
   (map map-headers-to-aliases)
   (into {})))

(defn has-prefix-on-key?
  "Verify if a given key has the expected prefix"
  [prefix [key _]]
  (some-> key
          (keyword)
          (name)
          (string/starts-with? prefix)))

(defn suffixed-keyword [alias [k v]]
  (assoc {}
         (keyword (str (name k) "-" (name alias))) v))

(defn map-suffixes-to-headers [[alias headers]]
  (->>
   (filter #(has-prefix-on-key? "x-" %) headers)
   (map #(suffixed-keyword alias %))
   (into {})))

(defn get-alias-suffixed-headers
  "Given a key-value pair, where key is the resource alias
  and value is it's value, inserts the key prefix on each key
  of the headers map."
  [headers-with-aliases-key]
  (->>
   headers-with-aliases-key
   (map map-suffixes-to-headers)
   (into {})))

(defn filter-cache-control-headers [headers]
  (select-keys headers [:cache-control]))

(defn get-cache-control-values [headers-by-resources]
  (->>
   headers-by-resources
   (map (fn [[_ headers]] (filter-cache-control-headers headers)))
   (map vals)
   (reduce concat)))

(defn split-cache-control-values [cache-control-values]
  (map #(string/split % #"=") cache-control-values))

(defn cache-control-values-to-map [splitted-cache-control-values]
  (->> splitted-cache-control-values
       (map #(if (= (count %) 1) (conj % true) %))
       (into {})
       (keywordize-keys)))

(defn parse-cache-control-values [cache-control-values]
  (->>
   cache-control-values
   (map #(string/split % #", "))
   (map split-cache-control-values)
   (map cache-control-values-to-map)))

(defn get-cache-control-headers [headers-by-resources]
  (->>
   headers-by-resources
   (get-cache-control-values)
   (parse-cache-control-values)))

(defn get-minimal-cache-type-value [cache-control-headers cache-type]
  "Returns a map with the minimal 'max-age' and 's-maxage' values"
  (->>
   cache-control-headers
   (map #(get-in % [cache-type]))
   (remove nil?)
   (reduce (fn [p n] (if (compare p n) -1) p n))
   (assoc {} cache-type)))

(defn get-query-cache-control [query]
  "Returns, if any, the value of query cache-control"
  (let [query-meta (select-keys (meta query) [:cache-control :max-age :s-maxage])]
    (if (some? (get query-meta :max-age))
      (dissoc query-meta :cache-control)
      (rename-keys query-meta {:cache-control :max-age}))))

(defn check-query-for-cache-control [query]
  "Returns true if headers have 'no-cache' or false if it doesn't"
  (if
   (empty? (get-query-cache-control query))
    false
    true))

(defn has-key-in-list-of-maps [key cache-control-headers]
  "Verify if a given key exists in a list of maps"
  (some true? (map #(contains? % key) cache-control-headers)))

(defn get-minimal-response-cache-control-values [cache-control-headers]
  "Returns a map with minimal 'max-age' and 's-maxage' values"
  (merge {}
         (if (has-key-in-list-of-maps :max-age cache-control-headers) (get-minimal-cache-type-value cache-control-headers :max-age) {})
         (if (has-key-in-list-of-maps :s-maxage cache-control-headers) (get-minimal-cache-type-value cache-control-headers :s-maxage) {})))

(defn check-headers-for-no-cache [cache-control-headers]
  "Returns true if headers have 'no-cache' or false if it doesn't"
  (some
   true?
   (map #(get-in % [:no-cache]) cache-control-headers)))

(defn generate-cache-string [query]
  "Generates cache string to be associated with :cache-control key"
  (->> query
       (map (fn [[k v]] (str (name k) "=" v)))
       (string/join ", ")))

(defn get-cache-control-string [query headers-by-resources]
  "Returns the string to be associated with ':cache-contral'"
  (let [cache-control-headers (get-cache-control-headers headers-by-resources)]
    (cond
      (check-query-for-cache-control query) (->> query
                                                 (get-query-cache-control)
                                                 (generate-cache-string))
      (check-headers-for-no-cache cache-control-headers) "no-cache"
      :else (->> cache-control-headers
                 (get-minimal-response-cache-control-values)
                 (generate-cache-string)))))

(defn get-cache-header [query headers-by-resources]
  "Adds cache control header to header list"
  (if (not-empty (get-cache-control-string query headers-by-resources))
    (assoc {} :cache-control
           (get-cache-control-string query headers-by-resources))
    {}))
