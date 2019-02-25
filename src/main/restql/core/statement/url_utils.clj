(ns restql.core.statement.url-utils
  (:require [clojure.string :as str]
            [ring.util.codec :refer [url-encode]]))

(defn- filter-variables [items]
  (->> items
       (filter #(re-matches #":[\w-]+" %))
       (map #(keyword (subs % 1)))
       (into #{})))

(defn- extract-path-parameters
  "receives an string containing a url pattern and returns
   a set with all declared parameters"
  [path]
  (->> (str/split path #"\/")
       (filter-variables)))

(defn- extract-query-parameters [query-strings]
  (->> (str/split query-strings #"\&")
       (filter-variables)))

(defn- splitted-url->map-by-type [splitted-url]
  (let [path-parameters (some-> splitted-url (get 0) (extract-path-parameters))
        query-parameters (if (= 2 (count splitted-url)) (some-> splitted-url (get 1) (extract-query-parameters)) #{})]
    (assoc {} :path path-parameters
              :query query-parameters)))

(defn- replace-url-with-param [params url param-key]
  (-> url
      (str/split #"\?")
      (get 0)
      (str/replace (re-pattern (str ":" (name param-key)))
                   (url-encode (str (param-key params))))))

(defn extract-url-parameters
  "given a parameterized url returns a
   map with :path and :query extrected variables"
  [url]
  (-> (str/split url #"\?")
      (splitted-url->map-by-type)))

(defn interpolate
  "given a parameterized url and a map with values, returns
   a string with a result url, with the values applied"
  [url params]
  (->> url
       (extract-url-parameters)
       (:path)
       (reduce (partial replace-url-with-param params) url)))


(defn dissoc-path-params
  "removes all keys of the map that appear as a parameter of
  the url"
  [url params]
  (->> url
       (extract-url-parameters)
       (:path)
       (reduce (fn [result param-key]
                 (dissoc result param-key))
               params)))

(defn- dissoc-query-params
  "removes all keys of the map that appear as a parameter of
  the url"
  [url params]
  (->> url
       (extract-url-parameters)
       (:query)
       (reduce (fn [result param-key]
                 (dissoc result param-key))
               params)))

(defn dissoc-params
  "removes all keys of the map that appear as a parameter of
  the url"
  [url params]
  (->> params
       (dissoc-path-params url)
       (dissoc-query-params url)))

(defn filter-explicit-query-params [url params]
  (-> url
      (extract-url-parameters)
      (:query)
      (as-> explicit-query-params (filter (fn [[k v]] (contains? explicit-query-params k)) params))
      (flatten)
      (as-> m (if-not (empty? m) (apply assoc {} m) {}))))

(defn from-mappings
  "gets a resource url from mappings"
  [mappings statement]
  (->> (:from statement)
       (get mappings)
  )
)
