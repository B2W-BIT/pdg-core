(ns restql.core.request.statement.expand-test
  (:require [clojure.test :refer :all]
            [restql.core.request.statement.expand :refer [expand]]))

(deftest expand-test
  (testing "Do nothing if theres no with"
    (is (= [{:from :resource-name}]
           (expand {:from :resource-name}))))

  (testing "Single value with returns self"
    (is (= [{:from :resource-name :with {:id 1}}]
           (expand {:from :resource-name :with {:id 1}}))))

  (testing "List value with returns one statement for each value"
    (is (= [{:from :resource-name :with {:id 1} :multiplexed true}, {:from :resource-name :with {:id 2} :multiplexed true}]
           (expand {:from :resource-name :with {:id [1, 2]}})))
    (is (= [{:from :resource-name :with {:name "a"} :multiplexed true}, {:from :resource-name :with {:name "b"} :multiplexed true}]
           (expand {:from :resource-name :with {:name ["a", "b"]}}))))

  (testing "Expand list value with keeps non list values"
    (is (= [{:from :resource-name :with {:id 1 :name "a"} :multiplexed true}, {:from :resource-name :with {:id 2 :name "a"} :multiplexed true}]
           (expand {:from :resource-name :with {:id [1, 2] :name "a"}}))))

  (testing "Expand multiple list value with keeps non list values"
    (is (= [{:from :resource-name :with {:id 1 :name "a" :job "clojurist"} :multiplexed true}, {:from :resource-name :with {:id 2 :name "b" :job "clojurist"} :multiplexed true}]
           (expand {:from :resource-name :with {:id [1, 2] :name ["a", "b"] :job "clojurist"}}))))

  (testing "Expand multiple list value with keeps non list values"
    (is (= [{:from :resource-name :with {:id [1, 2]}}]
           (expand {:from :resource-name :with {:id ^{:expand false} [1, 2]}}))))

  (testing "Expand multiple list value with keeps non list values"
    (is (= [{:from :resource-name :with {:id 1} :multiplexed true}]
           (expand {:from :resource-name :with {:id [1]}}))))

  (testing "Expand multiple list value with keeps non list values"
    (is (= [[{:from :resource-name :with {:id 1} :multiplexed true}] [{:from :resource-name :with {:id 2} :multiplexed true}]]
           (expand {:from :resource-name :with {:id [[1] [2]]}}))))

  (testing "Expand multiple list value with keeps non list values"
    (is (= [{:from :resource-name :with {:id 1 :name "a" :job "clojurist"} :multiplexed true} {:from :resource-name :with {:id nil :name "b" :job "clojurist"} :multiplexed true}]
           (expand {:from :resource-name :with {:id [1 nil] :name ["a", "b"] :job "clojurist"}})))))
