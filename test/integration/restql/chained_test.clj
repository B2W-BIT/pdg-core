(ns restql.chained-test
  (:require [clojure.test :refer :all]
            [restql.core.api.restql :as restql]
            [restql.test-util :as test-util]
            [stub-http.core :refer :all]))

(defn execute-query
  ([base-url query]
   (execute-query base-url query {}))
  ([base-url query params]
   (restql/execute-query :mappings {:hero     (str base-url "/hero")
                                    :sidekick (str base-url "/sidekick")
                                    :weapon (str base-url "/weapon/:id")}
                         :query query
                         :params params)))

(deftest chained-call
  (testing "Simple chain"
    (with-routes!
      {"/hero" (test-util/route-response {:hi "I'm hero" :sidekickId "A"})
       {:path "/sidekick" :query-params {:id "A"}} (test-util/route-response {:hi "I'm sidekick"})}
      (let [result (execute-query uri "from hero\n from sidekick with id = hero.sidekickId")]
        (is (= 200 (get-in result [:hero :details :status])))
        (is (= 200 (get-in result [:sidekick :details :status]))))))

  (testing "Chained with list"
    (with-routes!
      {"/hero" (test-util/route-response {:hi "I'm hero" :sidekickId ["A"]})
       {:path "/sidekick" :query-params {:id "A"}} (test-util/route-response {:hi "I'm sidekick"})}
      (let [result (execute-query uri "from hero\n from sidekick with id = hero.sidekickId")]
        (is (= 200 (get-in result [:hero :details :status])))
        (is (= [200] (map :status (get-in result [:sidekick :details])))))))

  (testing "Chained with list and single attr"
    (with-routes!
      {"/hero" (test-util/route-response {:hi "I'm hero" :sidekickId ["A"] :sidekickCode "C"})
       {:path "/sidekick" :query-params {:id "A" :code "C"}} (test-util/route-response {:hi "I'm sidekick"})}
      (let [result (execute-query uri "from hero\n from sidekick with id = hero.sidekickId, code = hero.sidekickCode")]
        (is (= 200 (get-in result [:hero :details :status])))
        (is (= [200] (map :status (get-in result [:sidekick :details])))))))

  (testing "Chained with list and empty param"
    (with-routes!
      {"/hero" (test-util/route-response {:hi "I'm hero" :sidekickId ["A"]})
       {:path "/sidekick" :query-params {:id "A"}} (test-util/route-response {:hi "I'm sidekick"})}
      (let [result (execute-query uri "from hero\n from sidekick with id = hero.sidekickId, code = hero.sidekickCode")]
        (is (= 200 (get-in result [:hero :details :status])))
        (is (= [200] (map :status (get-in result [:sidekick :details])))))))

  (testing "Chained with more than 1 level"
    (with-routes!
      {"/hero" (test-util/route-response [{:id "batman"}])

       {:path "/sidekick" :query-params {:sidekick "batman"}}
       (test-util/route-response [{:id "robin" :weaponId "123"}])

       "/weapon/123" (test-util/route-response {:descricao "Batarang"})}
      (let [result (execute-query uri "from hero\n
                                        from sidekick with sidekick = hero.id\n
                                        from weapon with id = sidekick.weaponId")]
        (is (= 200 (get-in result [:hero :details :status])))
        (is (= 200 (:status (first (get-in result [:sidekick :details])))))
        (is (= 200 (:status (first (first (get-in result [:weapon :details])))))))))

  (testing "Chained with value from header"
    (with-routes!
      {"/hero" (test-util/route-response 200 {:hi "I'm hero"} {:sidekickId "A"})
       {:path "/sidekick" :query-params {:id "A"}} (test-util/route-response {:hi "I'm sidekick"})}
      (let [result (execute-query uri "from hero\n from sidekick with id = hero.sidekickId")]
        (is (= 200 (get-in result [:hero :details :status])))
        (is (= 200 (get-in result [:sidekick :details :status]))))))

  (testing "Chained with value from header (empty body)"
    (with-routes!
      {"/hero" {:status 201 :content-type "application/json" :headers {:sidekickId "A"}}
       {:path "/sidekick" :query-params {:id "A"}} (test-util/route-response {:hi "I'm sidekick"})}
      (let [result (execute-query uri "from hero\n from sidekick with id = hero.sidekickId")]
        (is (= 201 (get-in result [:hero :details :status])))
        (is (= 200 (get-in result [:sidekick :details :status])))))))