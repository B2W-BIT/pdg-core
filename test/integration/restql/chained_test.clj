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
                                    :sidekick (str base-url "/sidekick")}
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
        (is (= [200] (map :status (get-in result [:sidekick :details]))))))))

