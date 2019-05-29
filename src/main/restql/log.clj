(ns restql.log
  (:require [clojure.tools.logging :as log]))

(defn warn [& s]
  (log/warn s))

(defn error [& s]
  (log/error s))

(defn debug [& s]
  (log/debug s))