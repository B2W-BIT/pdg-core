(ns restql.parser.printer
  (:require [clojure.pprint :refer [pprint]]))

(defn pprint-meta [obj]
  (binding [*print-meta* false]
    (let [orig-dispatch clojure.pprint/*print-pprint-dispatch*]
      (clojure.pprint/with-pprint-dispatch 
        (fn pm [o]
          (when (meta o)
            (print "^")
            (pm (meta o))
            (.write ^java.io.Writer *out* " ")
            (clojure.pprint/pprint-newline :fill))
          (orig-dispatch o))
        (clojure.pprint/pprint obj)))))

(defn pretty-print [result]
  (let [writer (new java.io.StringWriter)]
    (binding [*out* writer]
      (pprint-meta result))
    (.toString writer)))
