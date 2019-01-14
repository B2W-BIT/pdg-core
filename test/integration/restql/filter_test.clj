(ns restql.filter-test
  (:require [clojure.test :refer [deftest is]]
            [restql.core.api.restql :as restql]
            [cheshire.core :as json]
            [stub-http.core :refer :all]
            [clojure.pprint :refer :all]
            )
)

(def hero-response {:id "B20" :name "Batman" :sidekick "A20" :sidekicks [{ :id "A20" :name "Robin" } { :id "A30" :name "Alfred" } ] })
(defn hero-route []
  {:status 200 :content-type "application/json" :body (json/generate-string hero-response)})

(def heroes-response [ {:id "B20" :name "Batman"} {:id "B30" :name "Superman"} ])
(defn heroes-route []
  {:status 200 :content-type "application/json" :body (json/generate-string heroes-response)})

(def sidekick-response {:id "A20" :name "Robin" :heroes [ { :id "B20" :name "Batman" } { :id "B30" :name "Superman" }]})
(defn sidekick-route []
  {:status 200 :content-type "application/json" :body (json/generate-string sidekick-response)})

(defn execute-query [baseUrl query]
  (restql/execute-query :mappings {:hero                (str baseUrl "/hero")
                                   :sidekick            (str baseUrl "/sidekick")
                                   :heroes              (str baseUrl "/heroes")}
                        :query query)
)

(deftest simple-filter
    (with-routes! {"/hero" (hero-route)}
        (let [response (execute-query uri "from hero only name")]
            (is (= nil (get-in response [:hero :result :id])))
        )
    )
)

(deftest multiplex-filter
  (with-routes! {"/hero" (hero-route)}
      (let [response (execute-query uri "from hero with id = [1,2] only name")
              result (get-in response [:hero :result])]
          (is (= nil (:id (first result))))
          (is (= nil (:id (second result))))
      )
  )
)

(deftest array-filter
  (with-routes! {"/hero" (hero-route)}
      (let [response (execute-query uri "from hero only sidekicks.name")
            result (get-in response [:hero :result])]
          (is (= nil (:id (first (:sidekicks result)))))
          (is (= "Robin" (:name (first (:sidekicks result)))))

          (is (= nil (:id (second (:sidekicks result)))))
          (is (= "Alfred" (:name (second (:sidekicks result)))))
        )
  )
)

(deftest array-multiplex-filter
    (with-routes! {"/heroes" (heroes-route)}
       (let [response (execute-query uri "from heroes with id=[1,2] only name")
            first-request (first (get-in response [:heroes :result]))
            first-request-first-hero (first first-request)

            second-request (second (get-in response [:heroes :result]))
            second-request-second-hero (second second-request)]

          (is (= nil (:id first-request-first-hero)))
          (is (= "Batman" (:name first-request-first-hero)))

          (is (= nil (:id  second-request-second-hero)))
          (is (= "Superman" (:name second-request-second-hero)))
       )
    )
)
