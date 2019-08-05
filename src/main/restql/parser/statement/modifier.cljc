(ns restql.parser.statement.modifier
  (:require [restql.parser.statement.modifier-param :as param]
            [restql.parser.statement.modifier-function :as function]))

(defn use [context query-parsed]
  (->> query-parsed
       (param/use-modifiers)
       (param/produce context)))

(defn resource [context query-parsed]
  (->> query-parsed
       (param/resource-modifiers)
       (param/produce context)))

(defn function [context query-item-value]
  (->> query-item-value
       (function/from-item-value)
       (function/produce context)))
