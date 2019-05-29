(ns restql.parser.json)

(defn parse-string [s]
  (JSON.parse s))

(defn generate-string [s]
  (JSON.stringify s))

(def encode generate-string)