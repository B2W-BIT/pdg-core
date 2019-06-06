(ns restql.duplicate-request-test
  (:require [clojure.test :refer :all]
            [restql.parser.json :as json]
            [restql.core.api.restql :as restql]
            [stub-http.core :refer :all]))

(defn hero-route [counter]
  {:status 200 :content-type "application/json" :body (json/generate-string {:hi "I'm hero" :sidekickId "A20"}) :counter counter})

(deftest will-call-only-once
  (let [counter (atom 0)]
    (with-routes! {"/hero" (hero-route counter)}
      (restql/execute-query :mappings {:hero (str uri "/hero")}
                            :query "from hero")
      (is (= 1 @counter)))))

