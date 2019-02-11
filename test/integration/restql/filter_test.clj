(ns restql.filter-test
  (:require [clojure.test :refer [deftest is]]
            [restql.core.api.restql :as restql]
            [restql.test-util :as test-util]
            [cheshire.core :as json]
            [stub-http.core :refer :all]
            [clojure.pprint :refer :all]))

(defn execute-query [baseUrl query]
  (restql/execute-query :mappings {:hero     (str baseUrl "/hero")
                                   :sidekick (str baseUrl "/sidekick")
                                   :heroes   (str baseUrl "/heroes")}
                        :query query))

(deftest simple-filter
  (with-routes! {(test-util/route-request "/hero")
                 (test-util/route-response {:id "B20" :name "Batman"})}
      (let [response (execute-query uri "from hero only name")]
        (is (= "Batman" (get-in response [:hero :result :name])))
        (is (= nil      (get-in response [:hero :result :id]))))))

(deftest multiplex-filter
  (with-routes! {(test-util/route-request "/hero" {:id 1})
                 (test-util/route-response {:id "B10" :name "Batman"})
                 (test-util/route-request "/hero" {:id 2})
                 (test-util/route-response {:id "B20" :name "Robin"})}
      (let [response (execute-query uri "from hero with id = [1,2] only name")
            result (get-in response [:hero :result])]
        (is (= nil      (:id (first result))))
        (is (= "Batman" (:name (first result))))
        (is (= nil      (:id (second result))))
        (is (= "Robin"  (:name (second result)))))))

(deftest array-filter
  (with-routes! {(test-util/route-request "/hero")
                 (test-util/route-response {:id "B10" :sidekicks [{:name "Robin"} {:name "Alfred"}]})}
      (let [response (execute-query uri "from hero only sidekicks.name")
            result (get-in response [:hero :result])]
          (is (= nil (:id (first (:sidekicks result)))))
          (is (= "Robin" (:name (first (:sidekicks result)))))
          (is (= nil (:id (second (:sidekicks result)))))
          (is (= "Alfred" (:name (second (:sidekicks result))))))))

(deftest array-multiplex-filter
  (with-routes! {(test-util/route-request "/heroes" {:id 1})
                 (test-util/route-response [{:id "B10" :name "Batman"} {:id "B20" :name "Robin"}])
                 (test-util/route-request "/heroes" {:id 2})
                 (test-util/route-response [{:id "B30" :name "Superman"} {:id "B40" :name "Superwoman"}])}
    (let [response (execute-query uri "from heroes with id=[1,2] only name")
          first-request (first (get-in response [:heroes :result]))
          second-request (second (get-in response [:heroes :result]))]
      (is (= nil      (:id (first first-request))))
      (is (= "Batman" (:name (first first-request))))
      (is (= nil      (:id (second first-request))))
      (is (= "Robin"  (:name (second first-request))))

      (is (= nil          (:id (first second-request))))
      (is (= "Superman"   (:name (first second-request))))
      (is (= nil          (:id (second second-request))))
      (is (= "Superwoman" (:name (second second-request)))))))

(deftest multiplex-with-list
  (with-routes! {(test-util/route-request "/heroes")
                 (test-util/route-response [{:id "B10"} {:id "B20"}])
                 (test-util/route-request "/hero" {:id "B10"})
                 (test-util/route-response {:id "B10" :name "Batman"})
                 (test-util/route-request "/hero" {:id "B20"})
                 (test-util/route-response {:id "B10" :name "Robin"})}
    (let [response (execute-query uri "from heroes \n from hero with id = heroes.id only name")]
      (is (= nil      (:id (first (get-in response [:hero :result])))))
      (is (= "Batman" (:name (first (get-in response [:hero :result])))))
      (is (= nil      (:id (second (get-in response [:hero :result])))))
      (is (= "Robin"  (:name (second (get-in response [:hero :result]))))))))

(deftest multiplex-with-list
  (with-routes! {(test-util/route-request "/heroes")
                 (test-util/route-response {:heroes [{:id "B10"} {:id "B20"}]})
                 (test-util/route-request "/hero" {:id "B10"})
                 (test-util/route-response {:id "B10" :name "Batman" :sidekicks [{:name "Catwoman"}]})
                 (test-util/route-request "/hero" {:id "B20"})
                 (test-util/route-response {:id "B20" :name "Robin" :sidekicks [{:name "Alfred"}]})
                 (test-util/route-request "/sidekick" {:name "Catwoman"})
                 (test-util/route-response {:id "S10" :name "Catwoman" :movie "Batman"})
                 (test-util/route-request "/sidekick" {:name "Alfred"})
                 (test-util/route-response {:id "S20" :name "Alfred" :movie "Batman"})}
    (let [response (execute-query uri "from heroes
                                         with id = [1,2] \n
                                       from hero
                                         with id = heroes.heroes.id
                                         only name \n
                                       from sidekick
                                         with name = hero.sidekicks.name
                                         only id, name")]
      (is (= [[{:name "Batman"} {:name "Robin"}]
              [{:name "Batman"} {:name "Robin"}]] (get-in response [:hero :result])))
      (is (= [[[{:name "Catwoman" :id "S10"}] [{:name "Alfred" :id "S20"}]]
              [[{:name "Catwoman" :id "S10"}] [{:name "Alfred" :id "S20"}]]] (get-in response [:sidekick :result]))))))
