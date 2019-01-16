(ns restql.restql-test
  (:require [clojure.test :refer [deftest is testing]]
            [restql.parser.core :as parser]
            [restql.core.api.restql :as restql]
            [byte-streams :as bs]
            [cheshire.core :as json]
            [stub-http.core :refer :all]))

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
                                                      :weapon              (str base-url "/weapon/:id")
                                                      :product             (str base-url "/product/:id")
                                                      :product-price       (str base-url "/price/:productId")
                                                      :product-description (str base-url "/description/:productId")}
                                           :query query
                                           :params params))
  ([base-url query]
   (execute-query base-url query {})))

(deftest simple-request
  (with-routes!
    {"/hero" (hero-route)}
    (let [result (execute-query uri "from hero")]
      (is (= 200 (get-in result [:hero :details :status])))
      (is (= {:hi "I'm hero", :sidekickId "A20" :villains ["1" "2"] :weapons ["pen" "papel clip"]} (get-in result [:hero :result])))
      (is (= "I'm hero" (get-in result [:hero :result :hi]))))))

(deftest multiplexed-request

  (with-routes!
    {"/hero"     {:status 200 :content-type "application/json" :body (json/generate-string {:villains ["1" "2"]})}
     "/villain/1" (villain-route "1")
     "/villain/2" (villain-route "2")}
    (let [result (execute-query uri "from hero\n from villain with id = hero.villains")]
      (is (= {:villains ["1" "2"]} (get-in result [:hero :result])))
      (is (= [{:hi "I'm villain", :id "1"} {:hi "I'm villain", :id "2"}] (get-in result [:villain :result])))))
  
  ; Test simple case with: single, list with one, list with two
  (with-routes!
    {"/hero" (hero-route)}
    (let [response (execute-query uri "from hero with name = $name" {:name "Doom"})
          details (get-in response [:hero :details])
          result (get-in response [:hero :result])]
      (is (= 200 (:status details)))
      (is (= {:hi "I'm hero", :sidekickId "A20" :villains ["1" "2"] :weapons ["pen" "papel clip"]} result))))
  
  (with-routes!
    {"/hero" (hero-route)}
    (let [response (execute-query uri "from hero with name = $name" {:name ["Doom"]})
          details (get-in response [:hero :details])
          result (get-in response [:hero :result])]
      (is (= 200 (:status (first details))))
      (is (= [{:hi "I'm hero", :sidekickId "A20" :villains ["1" "2"] :weapons ["pen" "papel clip"]}] result))))
  
  (with-routes!
    {"/hero" (hero-route)}
    (let [response (execute-query uri "from hero with name = $name" {:name ["Doom" "Duke Nuken"]})
          details (get-in response [:hero :details])
          result (get-in response [:hero :result])]
      (is (= 200 (:status (first details))))
      (is (= 200 (:status (second details))))
      (is (= [{:hi "I'm hero", :sidekickId "A20" :villains ["1" "2"] :weapons ["pen" "papel clip"]}
              {:hi "I'm hero", :sidekickId "A20" :villains ["1" "2"] :weapons ["pen" "papel clip"]}] result))))
  
  ;; Test simple case with list: single, list with one, list with two
  (with-routes!
    {"/hero"     {:status 200 :content-type "application/json" :body (json/generate-string {:villains ["1" "2"]})}
     "/villain/1" (villain-route "1")
     "/villain/2" (villain-route "2")}
    (let [result (execute-query uri "from hero with id =1\nfrom villain with id = hero.villains")]
      (is (= 200 (:status (get-in result [:hero :details]))))
      (is (= 200 (:status (first   (get-in result [:villain :details])))))
      (is (= 200 (:status (second  (get-in result [:villain :details])))))
      (is (= {:villains ["1" "2"]} (get-in result [:hero :result])))
      (is (= [{:hi "I'm villain", :id "1"} {:hi "I'm villain", :id "2"}] (get-in result [:villain :result])))))
  
  (with-routes!
    {"/hero"     {:status 200 :content-type "application/json" :body (json/generate-string {:villains ["1" "2"]})}
     "/villain/1" (villain-route "1")
     "/villain/2" (villain-route "2")}
    (let [result (execute-query uri "from hero with id =[1]\nfrom villain with id = hero.villains")]
      (is (= 200 (:status (first (get-in result [:hero :details])))))
      (is (= 200 (:status (first  (first (get-in result [:villain :details]))))))
      (is (= 200 (:status (second (first (get-in result [:villain :details]))))))
      (is (= [{:villains ["1" "2"]}] (get-in result [:hero :result])))
      (is (= [[{:hi "I'm villain", :id "1"} {:hi "I'm villain", :id "2"}]] (get-in result [:villain :result])))))
  
  (with-routes!
    {"/hero"     {:status 200 :content-type "application/json" :body (json/generate-string {:villains ["1" "2"]})}
     "/villain/1" (villain-route "1")
     "/villain/2" (villain-route "2")}
    (let [result (execute-query uri "from hero with id =[1,2]\nfrom villain with id = hero.villains")]
      (is (= 200 (:status (first (get-in result [:hero :details])))))
      (is (= 200 (:status (second (get-in result [:hero :details])))))
      (is (= 200 (:status (first  (first (get-in result [:villain :details]))))))
      (is (= 200 (:status (second (first (get-in result [:villain :details]))))))
      (is (= 200 (:status (first  (second (get-in result [:villain :details]))))))
      (is (= 200 (:status (second (second  (get-in result [:villain :details]))))))
      (is (= [{:villains ["1" "2"]} {:villains ["1" "2"]}] (get-in result [:hero :result])))
      (is (= [[{:hi "I'm villain", :id "1"} {:hi "I'm villain", :id "2"}]
              [{:hi "I'm villain", :id "1"} {:hi "I'm villain", :id "2"}]] (get-in result [:villain :result])))))
  
  (with-routes!
    {"/heroes"     {:status 200 :content-type "application/json" :body (json/generate-string [{:villains ["1" "2"]} {:villains ["3" "4"]}])}
     "/villain/1" (villain-route "1")
     "/villain/2" (villain-route "2")
     "/villain/3" (villain-route "3")
     "/villain/4" (villain-route "4")}
    (let [result (execute-query uri "from heroes\nfrom villain with id = heroes.villains")]
      (is (= 200 (get-in result [:heroes :details :status])))
      (is (= 200 (:status (first  (first  (get-in result [:villain :details]))))))
      (is (= 200 (:status (second (first  (get-in result [:villain :details]))))))
      (is (= 200 (:status (first  (second (get-in result [:villain :details]))))))
      (is (= 200 (:status (second (second (get-in result [:villain :details]))))))
      (is (= [{:villains ["1" "2"]} {:villains ["3" "4"]}] (get-in result [:heroes :result])))
      (is (= [[{:hi "I'm villain", :id "1"} {:hi "I'm villain", :id "2"}]
              [{:hi "I'm villain", :id "3"} {:hi "I'm villain", :id "4"}]] (get-in result [:villain :result])))))
  
  (with-routes!
    {"/hero"     {:status 200 :content-type "application/json" :body (json/generate-string {:villains "1"})}
     "/villain/1" {:status 200 :content-type "application/json" :body (json/generate-string {:weapons ["dagger" "sword"]})}
     "/weapon/dagger" {:status 200 :content-type "application/json" :body (json/generate-string {:name "dagger"})}
     "/weapon/sword" {:status 200 :content-type "application/json" :body (json/generate-string {:name "sword"})}}
    (let [result (execute-query uri "from hero\n 
                                     from villain with id = hero.villains\n
                                     from weapon with id = villain.weapons")]
      (is (= {:villains "1"} (get-in result [:hero :result])))
      (is (= {:weapons ["dagger" "sword"]} (get-in result [:villain :result])))
      (is (= [{:name "dagger"} {:name "sword"}] (get-in result [:weapon :result])))))
  
  (with-routes!
    {"/hero"     {:status 200 :content-type "application/json" :body (json/generate-string {:villains ["1"]})}
     "/villain/1" {:status 200 :content-type "application/json" :body (json/generate-string {:weapons ["dagger" "sword"]})}
     "/weapon/dagger" {:status 200 :content-type "application/json" :body (json/generate-string {:name "dagger"})}
     "/weapon/sword" {:status 200 :content-type "application/json" :body (json/generate-string {:name "sword"})}}
    (let [result (execute-query uri "from hero\n 
                                     from villain with id = hero.villains\n
                                     from weapon with id = villain.weapons")]
      (is (= {:villains ["1"]} (get-in result [:hero :result])))
      (is (= [{:weapons ["dagger" "sword"]}] (get-in result [:villain :result])))
      (is (= [[{:name "dagger"} {:name "sword"}]] (get-in result [:weapon :result])))))
 
  (testing "With map->list->simple_value" 
    (with-routes!
      {"/hero"          {:status 200 :content-type "application/json" :body (json/generate-string {:villains [{:id "1" :weapon "DAGGER"}]})}
       "/villain/1"     {:status 200 :content-type "application/json" :body (json/generate-string {:id "1"})}
       "/weapon/DAGGER" {:status 200 :content-type "application/json" :body (json/generate-string {:id "DAGGER"})}}
      (let [result (execute-query uri "from hero \n
                                      from villain with id = hero.villains.id \n 
                                      from weapon with id = hero.villains.weapon")]
        (is (= {:villains [{:id "1" :weapon "DAGGER"}]} (get-in result [:hero :result])))
        (is (= [{:id "1"}] (get-in result [:villain :result])))
        (is (= [{:id "DAGGER"}] (get-in result [:weapon :result]))))))
  
  (testing "With map->list->complex_value" 
    (with-routes!
      {"/hero"                  {:status 200 :content-type "application/json" :body (json/generate-string {:villains [{:v {:id "1"}}]})}
       "/villain/{\"id\":\"1\"}"  {:status 200 :content-type "application/json" :body (json/generate-string {:id "1"})}}
      (let [result (execute-query uri "from hero \n
                                      from villain with id = hero.villains.v")]
        (is (= {:villains [{:v {:id "1"}}]} (get-in result [:hero :result])))
        (is (= [{:id "1"}] (get-in result [:villain :result]))))))
  
  (testing "With list->map->list->simple_value and list->map->list->complex_value"
    (with-routes!
      {"/heroes"         {:status 200 :content-type "application/json" :body (json/generate-string [{:villains [{:id "1" :weapons ["DAGGER"]}]}])}
       "/villain/1"      {:status 200 :content-type "application/json" :body (json/generate-string {:id "1"})}
       "/weapon/DAGGER"  {:status 200 :content-type "application/json" :body (json/generate-string {:id "DAGGER"})}}
      (let [result (execute-query uri "from heroes\n
                                      from villain with id = heroes.villains.id\n 
                                      from weapon with id = heroes.villains.weapons")]
        (is (= [{:villains [{:id "1" :weapons ["DAGGER"]}]}] (get-in result [:heroes :result])))
        (is (= [[{:id "1"}]] (get-in result [:villain :result])))
        (is (= [[[{:id "DAGGER"}]]] (get-in result [:weapon :result])))))))

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
      (is (= {:hi "I'm hero", :sidekickId "A20" :villains ["1" "2"] :weapons ["pen" "papel clip"]} (get-in result [:hero :result]))))))

(deftest request-with-encoder
  (with-routes!
    {{:path "/hero" :query-params {:bag "%7B%22capacity%22%3A10%7D"}} (hero-route)}
    (let [result (execute-query uri "from hero with bag = {capacity: 10} -> json")]
      (is (= 200 (get-in result [:hero :details :status])))
      (is (= {:hi "I'm hero", :sidekickId "A20" :villains ["1" "2"] :weapons ["pen" "papel clip"]} (get-in result [:hero :result]))))))

(deftest request-with-encoder-2
  (with-routes!
    {"/hero" (hero-with-bag-route)
     {:path "/sidekick" :query-params {:bag "%7B%22capacity%22%3A10%7D"}} (sidekick-route)}
    (let [result (execute-query uri "from hero \n from sidekick with bag = hero.bag -> json")]
      (is (= 200 (get-in result [:hero :details :status])))
      (is (= {:hi "I'm hero", :bag {:capacity 10}} (get-in result [:hero :result])))
      (is (= 200 (get-in result [:sidekick :details :status])))
      (is (= {:hi "I'm sidekick"} (get-in result [:sidekick :result]))))))

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

(deftest request-with-multiplexed-param-map
  (with-routes!
    {{:path "/hero" :query-params {:name "Jiraiya"}} (hero-route)
     {:path "/hero" :query-params {:name "Jaspion"}} (hero-route)}
    (let [response (execute-query uri "from hero with $hero" {:hero {:name ["Jiraiya" "Jaspion"]}})
          details (get-in response [:hero :details])
          result (get-in response [:hero :result])]
      (is (= 200 (:status (first details))))
      (is (= 200 (:status (second details))))
      (is (= [{:hi "I'm hero", :sidekickId "A20" :villains ["1" "2"] :weapons ["pen" "papel clip"]}
        {:hi "I'm hero", :sidekickId "A20" :villains ["1" "2"] :weapons ["pen" "papel clip"]}] result)))))

(deftest timeout-request-should-return-408
  (with-routes!
    {"/hero" (assoc (hero-route) :delay 500)}
    (let [result (execute-query uri "from hero timeout 100")]
      (is (= 408 (get-in result [:hero :details :status])))
      (is (= {:message "RequestTimeoutException"} (get-in result [:hero :result]))))))

;(deftest unreachable-resource-should-return-503
; (let [result (execute-query "http://localhost:9999" "from hero ignore-errors")]
;  (is (= 503 (get-in result [:hero :details :status])))
;)
;)

(deftest chained-call
  (with-routes! {"/hero" (hero-route) "/sidekick" (sidekick-route)}
    (let [result (execute-query uri "from hero\nfrom sidekick")]
      (is (= 200 (get-in result [:hero :details :status])))
      (is (= {:hi "I'm hero", :sidekickId "A20" :villains ["1" "2"] :weapons ["pen" "papel clip"]} (get-in result [:hero :result])))
      (is (= 200 (get-in result [:sidekick :details :status])))
      (is (= {:hi "I'm sidekick"} (get-in result [:sidekick :result]))))))

(deftest with-params
  (with-routes! {"/product/1234" (product-route 1234)}
    (let [result (execute-query uri "from product with id = $id" {:id "1234"})]
      (is (= 200 (get-in result [:product :details :status])))
      (is (= {:product "1234"} (get-in result [:product :result]))))))

(deftest shouldnt-throw-exeption-if-chainned-resource-timeout-and-ignore-error
  (with-routes!
    {"/hero" (hero-route)}
    {"/sideck" (assoc (sidekick-route) :delay 200)}
    (let [result (execute-query uri "from hero\nfrom sidekick timeout 100 with id = hero.sidekickId ignore-errors")]
      (is (= 200 (get-in result [:hero :details :status])))
      (is (= {:hi "I'm hero", :sidekickId "A20" :villains ["1" "2"] :weapons ["pen" "papel clip"]} (get-in result [:hero :result])))
      (is (= 408 (get-in result [:sidekick :details :status])))
      (is (not (nil? (get-in result [:sidekick :result :message])))))))

