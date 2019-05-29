(ns restql.parser.json
  (:require [cheshire.core :as json]))

(defn parse-string [s]
  (json/parse-string s true))

(defn generate-string [s]
  (json/generate-string s))

(def encode generate-string)