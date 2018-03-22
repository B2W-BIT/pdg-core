(ns restql.restql-test
  (:require [clojure.test :refer [deftest is]]
            [restql.core.api.restql :as restql]
            [cheshire.core :as json]
            [stub-http.core :refer :all]
  )
)

(defn hero-route []
  {:status 200 :content-type "application/json" :body (json/generate-string {:hi "I'm hero" :sidekickId "A20"})})

(defn sidekick-route []
  {:status 200 :content-type "application/json" :body (json/generate-string {:hi "I'm sidekick"})})


(defn execute-query ([base-url query params]
    (restql/execute-query :mappings {:hero                (str base-url "/hero")
                                     :sidekick            (str base-url "/sidekick")
                                     :product             (str base-url "/product/:id")
                                     :product-price       (str base-url "/price/:productId")
                                     :product-description (str base-url "/description/:productId")}
                          :query query
                          :params params)

  )
  ([base-url query]
   (execute-query base-url query {})
  )
)

(deftest simple-request
  (with-routes!
    {"/hero" (hero-route)}
    (let [result (execute-query uri "from hero")]
      (is (= 200 (get-in result [:hero :details :status])))
      (is (= "I'm hero" (get-in result [:hero :result :hi])))
    )
  )
)

(deftest multiplexed-request
  (with-routes!
    {"/hero" (hero-route)}
    (let [response (execute-query uri "from hero with name = $name" { :name [ "Doom" "Duke Nuken" ] })
          details (get-in response [:hero :details])
          result (get-in response [:hero :result])]
      (is (= 200 (:status (first details))))
      (is (= 200 (:status (second details))))
      (is (= "I'm hero" (:hi (first result))))
      (is (= "I'm hero" (:hi (second result))))
    )
  )
)

;(deftest error-request-should-throw-exception
; (with-routes!
;  {"/hero" (assoc (hero-route) :status 500)}
; (is (thrown? Exception (execute-query uri "from hero")))
;)
;)

(deftest error-request-with-ignore-errors-shouldnt-throw-exception
  (with-routes!
    {"/hero" (assoc (hero-route) :status 500)}
    (let [result (execute-query uri "from hero ignore-errors")]
      (is (= 500 (get-in result [:hero :details :status])))
    )
  )
)

(deftest timeout-request-shoudl-return-408
  (with-routes!
    {"/hero" (assoc (hero-route) :delay 200)}
    (let [result (execute-query uri "from hero timeout 100")]
      (is (= 408 (get-in result [:hero :details :status])))
    )
  )
)

;(deftest unreachable-resource-should-return-503
; (let [result (execute-query "http://localhost:9999" "from hero ignore-errors")]
;  (is (= 503 (get-in result [:hero :details :status])))
;)
;)

(deftest chained-call
  (with-routes! {"/hero" (hero-route) "/sidekick" (sidekick-route)}
    (let [result (execute-query uri "from hero\nfrom sidekick")]
      (is (= 200 (get-in result [:hero :details :status])))
      (is (= 200 (get-in result [:sidekick :details :status])))
    )
  )
)

;(deftest should-throw-exeption-if-chainned-resource-fails
; (with-routes!
;{"/hero" (hero-route)
; "/sidekick" (sidekick-route)}
;(is (thrown? Exception (execute-query uri "from hero\nfrom sidekick with id = hero.sidekickId")))
;)
;)

(deftest shouldnt-throw-exeption-if-chainned-resource-timeout-and-ignore-error
  (with-routes!
    {"/hero" (hero-route)}
    {"/sideck" (assoc (sidekick-route) :delay 200)}
    (let [result (execute-query uri "from hero\nfrom sidekick timeout 100 with id = hero.sidekickId ignore-errors")]
      (is (= 200 (get-in result [:hero :details :status])))
      (is (= 408 (get-in result [:sidekick :details :status])))
    )
  )
)

