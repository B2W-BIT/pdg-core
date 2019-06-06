(ns restql.parser.json
  (:require [jsonista.core :as json]))

(defonce mapper
  (json/object-mapper
   {:decode-key-fn true}))

(defn parse-string [s]
  (json/read-value s mapper))

(defn generate-string [s]
  (json/write-value-as-string s))

(def encode generate-string)