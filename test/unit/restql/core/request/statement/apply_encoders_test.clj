(ns restql.core.request.statement.apply-encoders-test
  (:require [clojure.test :refer :all]
            [restql.core.request.statement.apply-encoders :refer [apply-encoders]]))

(deftest apply-encoders-test
  (testing "Resolve without encoder on single return value"
    (is (= [{:from :resource-name :with {:bag "{\"capacity\":10}" :name ["a" "b"]}}]
           (apply-encoders nil
                                     [{:from :resource-name
                                       :with {:bag {:capacity 10} :name ["a" "b"]}}]))))
  (testing "Resolve with encoder on single return value"
    (is (= [{:from :resource-name :with {:bag "{\"capacity\":10}" :name "[\"a\",\"b\"]"}}]
           (apply-encoders nil
                                     [{:from :resource-name
                                       :with {:bag {:capacity 10} :name ^{:encoder :json} ["a" "b"]}}])))))
