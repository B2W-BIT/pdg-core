(ns restql.core.url
  (:require [clojure.string :as str]
            [ring.util.codec :refer [url-encode]]))

(defn extract-parameters
  "receives an string containing a url pattern and returns
   a set with all declared parameters"
  [url]
  (->> (str/split url #"/")
       (filter #(re-matches #":[\w-]+" %))
       (map #(keyword (subs % 1)))
       (into #{})))

(defn interpolate
  "given a parameterized url and a map with values, returns
   a string with a result url, with the values applied"
  [url params]
  (reduce (fn [result param-key]
              (str/replace result
                         (re-pattern (str ":" (name param-key)))
                           (url-encode (str (param-key params)))))
          url (extract-parameters url)))

(defn dissoc-params
  "removes all keys of the map that appear as a parameter of
  the url"
  [url params]
  (reduce (fn [result param-key]
            (dissoc result param-key))
          params (extract-parameters url)))

(defn from-mappings
  "gets a resource url from mappings"
  [mappings statement]
  (->> (:from statement)
       (get mappings)
  )
)
