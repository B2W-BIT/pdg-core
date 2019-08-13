(ns restql.timeout-test
  (:require [clojure.test :refer :all]
            [stub-http.core :refer :all]
            [clojure.core.async :refer :all]
            [restql.core.api.restql :as restql]
            [restql.test-util :refer [route-response route-request route-header]]))

(defn hero-route []
  (route-response {:hi "I'm hero" :sidekickId "A20" :villains ["1" "2"] :weapons ["pen" "papel clip"]}))

(defn sidekick-route []
  (route-response {:hi "I'm sidekick"}))

(deftest timeout-request-should-return-408
  (with-routes!
    {"/hero" (assoc (hero-route) :delay 500)}
    (let [result (restql/execute-query :mappings {:hero (str uri "/hero")}
                                       :query "from hero timeout 100")]
      (is (= 408 (get-in result [:hero :details :status])))
      (is (= {:message "RequestTimeoutException"} (get-in result [:hero :result]))))))

(deftest shouldnt-throw-exeption-if-chainned-resource-timeout-and-ignore-error
  (with-routes!
    {"/hero" (hero-route)}
    {"/sidekick" (assoc (sidekick-route) :delay 200)}
    (let [result (restql/execute-query :mappings {:hero (str uri "/hero")
                                                  :sidekick (str uri "/sidekick")}
                                       :query "from hero\nfrom sidekick timeout 100 with id = hero.sidekickId ignore-errors")]
      (is (= 200 (get-in result [:hero :details :status])))
      (is (= {:hi "I'm hero", :sidekickId "A20" :villains ["1" "2"] :weapons ["pen" "papel clip"]} (get-in result [:hero :result])))
      (is (= 408 (get-in result [:sidekick :details :status])))
      (is (not (nil? (get-in result [:sidekick :result :message])))))))

(deftest with-global-timeout
  (testing "If a global timeout occurs returns timeout error in result-ch."
    (with-routes!
      {"/hero" (hero-route)}
      (is (= {:error :timeout}
             (->
              (restql/execute-query-channel :mappings {:hero (str uri "/hero")}
                                            :encoders {}
                                            :query (restql.parser.core/parse-query "from hero" :context {})
                                            :query-opts {:global-timeout 1})
              (first)
              (clojure.core.async/<!!)))))))

(deftest stop-waiting-requests
  (testing "If a global timeout occurs."
    (let [counter (atom 0)
          do-request restql.core.runner.executor/do-request]
      (with-redefs [restql.core.runner.executor/do-request
                    (fn [p1 p2 p3] (do (swap! counter inc) (do-request p1 p2 p3)))]
        (do
          (->>
           (restql/execute-query-channel :mappings {:hero "http://0.0.0.0/hero" :sidekick "http://0.0.0.0/sidekick"}
                                         :encoders {}
                                         :query (restql.parser.core/parse-query "from hero \n from sidekick with id = hero.sidekickId")
                                         :query-opts {:global-timeout 1})
           (first)
           (<!!))
          (is (> 2 @counter))))))

    (testing "If a global timeout occurs."
      (let [counter2 (atom 0)
            do-request restql.core.runner.executor/do-request]
        (with-redefs [restql.core.runner.executor/do-request
                      (fn [p1 p2 p3] (do (swap! counter2 inc) (do-request p1 p2 p3)))]
          (do
            (->>
             (restql/execute-query-channel :mappings {:hero "http://0.0.0.0/hero" :sidekick "http://0.0.0.0/sidekick"}
                                           :encoders {}
                                           :query (restql.parser.core/parse-query "use timeout = 1 \n from hero \n from sidekick with id = hero.sidekickId"))
             (first)
             (<!!))
            (is (> 2 @counter2)))))))
