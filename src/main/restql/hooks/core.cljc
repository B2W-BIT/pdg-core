(ns restql.hooks.core
  (:require [restql.log :as log]))

(defonce hook-store (atom {}))

(defn merge-hooks [hooks1 hooks2]
  (into []
        (concat hooks1 hooks2)))

(defn execute-hook-fn [hook-function hook-name context]
  #?(:clj (try
            (hook-function context)
            (catch Exception e
              (do
                (log/warn "Hook: error executing fn, hook-name: " hook-name " message: " (.getMessage e))
                {})))
     :cljs  (try
              (hook-function context)
              (catch :default e
                (do
                  (log/warn "Hook: error executing fn, hook-name: " hook-name " message: " (.getMessage e))
                  {})))))

(defn conj-context [context1 context2]
  (if (map? context2)
    (conj context1 context2)
    context1))

(defn register-hook [hook-name hook-fns]
  (cond
    (contains? @hook-store hook-name)
    (swap! hook-store assoc hook-name (merge-hooks hook-fns (hook-name @hook-store)))
    :else
    (swap! hook-store assoc hook-name hook-fns)))

(defn execute-hook [hook-name context]
  (if (contains? @hook-store hook-name)
    (->> (map #(execute-hook-fn % hook-name context) (@hook-store hook-name))
         (reduce conj-context {})
         (doall))))

(comment
  "
  This is the hook map format restQL-core understands
  "
  hook-format {:before-query [hook-bq-1]
               :after-query [hook-aq-1 hook-aq-2]
               :before-request [hook-br-1, hook-br-2]
               :after-request [hook-ar-1 hook-ar-2 hook-ar-3]})
