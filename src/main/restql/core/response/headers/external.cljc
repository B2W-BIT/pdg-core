(ns restql.core.response.headers.external
  (:require [clojure.string :as string]))

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
  [headers-by-resources]
  (->>
   headers-by-resources
   (map map-suffixes-to-headers)
   (into {})))
