(ns restql.log)

(defn warn [& s]
  (js/console.log s))

(defn error [& s]
  (js/console.log s))

(defn debug [& s]
  (js/console.log s))