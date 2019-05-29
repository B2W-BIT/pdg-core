(ns restql.core.validator.util
  (:require [clojure.string :as string])
  #?(:clj (:use [slingshot.slingshot :only [throw+]])))

(defn interpolate [message context]
  (let [keys (keys context)]
    (reduce (fn [output key]
              (string/replace output (str key) (get context key))) message keys)))

(defn validation [fun message]
  (fn [context query]
    (let [res (fun context query)]
      (cond
        (= true res) query
        (= false res) #?(:clj (throw+ {:type :validation-error :message message})
                         :cljs (throw {:type :validation-error :message message}))
        (map? res) #?(:clj (throw+ {:type :validation-error :message (interpolate message res)})
                      :cljs (throw {:type :validation-error :message (interpolate message res)}))
        :else query))))

(defmacro rule [msg arg & body]
  `(let [fun# (fn ~arg ~@body)]
     (validation fun# ~msg)))

(defn apply-rule [context query rule]
  (rule context query))

(defn apply-rules [& rules]
  (fn [context query]
    (reduce (partial apply-rule context) query rules)))

(defmacro defrules [name & rules]
  `(def ~name (apply-rules ~@rules)))
