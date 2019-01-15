(ns restql.restql-test
  (:require [clojure.test :refer [deftest is]]
            [restql.parser.core :as parser]
            [restql.core.api.restql :as restql]
            [byte-streams :as bs]
            [cheshire.core :as json]
            [stub-http.core :refer :all]
            )
)

(defn get-stub-body [request]
  (val (first (:body request))))

(defn hero-route []
  {:status 200 :content-type "application/json" :body (json/generate-string {:hi "I'm hero" :sidekickId "A20" :villains ["1" "2"] :weapons ["pen" "papel clip"]})})

(defn hero-with-bag-route []
  {:status 200 :content-type "application/json" :body (json/generate-string {:hi "I'm hero" :bag {:capacity 10}})})

(defn sidekick-route []
  {:status 200 :content-type "application/json" :body (json/generate-string {:hi "I'm sidekick"})})

(defn product-route [id]
  {:status 200 :content-type "application/json" :body (json/generate-string {:product (str id)})})

(defn heroes-route []
  {:status 200
   :content-type "application/json"
   :body (json/generate-string [{:hi "I'm hero" :villains ["1" "2"]} {:hi "I'm hero" :villains ["3" "4"]}])})

(defn villain-route [id]
  {:status 200 :content-type "application/json" :body (json/generate-string {:hi "I'm villain" :id (str id)})})

(defn execute-query ([base-url query params]
    (restql/execute-query :mappings {:hero                (str base-url "/hero")
                                     :heroes              (str base-url "/heroes")
                                     :sidekick            (str base-url "/sidekick")
                                     :villain             (str base-url "/villain/:id")
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

  (with-routes!
    {"/hero"     (hero-route)
     "/villain/1" (villain-route "1")
     "/villain/2" (villain-route "2")}
    (let [result (execute-query uri "from hero with id =[1,2]\nfrom villain with id = hero.villains")]
      (is (= 200 (:status (first (get-in result [:hero :details])))))
      (is (= 200 (:status (second (get-in result [:hero :details])))))
      (is (= 200 (:status (first  (first (get-in result [:villain :details]))))))
      (is (= 200 (:status (second (first (get-in result [:villain :details]))))))
      (is (= 200 (:status (first  (second (get-in result [:villain :details]))))))
      (is (= 200 (:status (second (second  (get-in result [:villain :details]))))))))

  (with-routes!
    {"/heroes"     (heroes-route)
     "/villain/1" (villain-route "1")
     "/villain/2" (villain-route "2")
     "/villain/3" (villain-route "3")
     "/villain/4" (villain-route "4")}
    (let [result (execute-query uri "from heroes\nfrom villain with id = heroes.villains")]
      (is (= 200 (get-in result [:heroes :details :status])))
      (is (= 200 (:status (first  (first  (first (get-in result [:villain :details])))))))
      (is (= 200 (:status (second (first  (first (get-in result [:villain :details])))))))
      (is (= 200 (:status (first  (second (first (get-in result [:villain :details])))))))
      (is (= 200 (:status (second (second (first (get-in result [:villain :details])))))))))
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

(deftest request-with-encoder
  (with-routes!
    {{:path "/hero" :query-params { :bag "%7B%22capacity%22%3A10%7D"}} (hero-route)}
    (let [result (execute-query uri "from hero with bag = {capacity: 10} -> json")]
      (is (= 200 (get-in result [:hero :details :status])))
    )
  )
)

(deftest request-with-encoder-2
  (with-routes!
    {"/hero" (hero-with-bag-route)
     {:path "/sidekick" :query-params {:bag "%7B%22capacity%22%3A10%7D"}} (sidekick-route)}
    (let [result (execute-query uri "from hero \n from sidekick with bag = hero.bag -> json")]
      (is (= 200 (get-in result [:hero :details :status])))
      (is (= 200 (get-in result [:sidekick :details :status])))
    )
  )
)

(deftest request-with-quoted-param
  (with-routes!
    {{:path "/hero" :query-params {:name "Dwayne+%22The+Rock%22+Johnson"}} (hero-route)}
    (let [result (execute-query uri "from hero with name = $name" {:name "Dwayne \"The Rock\" Johnson"})]
      (is (= 200 (get-in result [:hero :details :status]))))))

(deftest execute-query-post
  (with-routes!
    {(fn [request]
       (and (= (:path request) "/hero")
            (= (:method request) "POST")
            (= (get-stub-body request) (json/generate-string {:id 1})))) (hero-route)}
    (let [result (execute-query uri "to hero body id = 1")]
      (is (= 200 (get-in result [:hero :details :status]))))))

(deftest request-with-param-map
  (with-routes!
    {{:path "/hero" :query-params {:name "Jiraiya" :age "45"}} (hero-route)}
    (let [result (execute-query uri "from hero with $hero" {:hero {:name "Jiraiya" :age 45}})]
      (is (= 200 (get-in result [:hero :details :status]))))))

(deftest timeout-request-should-return-408
  (with-routes!
    {"/hero" (assoc (hero-route) :delay 500)}
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

(deftest chained-call
  (with-routes! {"/hero" (hero-route) "/sidekick" (sidekick-route)}
    (let [result (execute-query uri "from hero\nfrom sidekick")]
      (is (= 200 (get-in result [:hero :details :status])))
      (is (= 200 (get-in result [:sidekick :details :status])))
    )
  )
)

(deftest with-params
  (with-routes! {"/product/1234" (product-route 1234)}
    (let [result (execute-query uri "from product with id = $id" {:id "1234"})]
      (is (= 200 (get-in result [:product :details :status])))
    )
  )
)

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

