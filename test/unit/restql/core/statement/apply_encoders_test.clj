(ns restql.core.statement.apply-encoders-test
  (:require [clojure.test :refer :all]
            [restql.core.statement.apply-encoders :refer [apply-encoders]]))

(deftest apply-encoders-test
  (testing "Resolve without encoder on single return value"
    (is (= [{:from :resource-name
             :method :get
             :with {:bag "{\"capacity\":10}"
                    :name ["a" "b"]}}]
           (apply-encoders nil
                           [{:from :resource-name
                             :method :get
                             :with {:bag {:capacity 10}
                                    :name ["a" "b"]}}]))))
  (testing "Resolve with encoder on single return value"
    (is (= [{:from :resource-name
             :method :get
             :with {:bag "{\"capacity\":10}"
                    :name "[\"a\",\"b\"]"}}]
           (apply-encoders nil
                           [{:from :resource-name
                             :method :get
                             :with {:bag {:capacity 10}
                                    :name ^{:encoder :json} ["a" "b"]}}])))))

(deftest apply-encoders-post-test
  (testing "Resolve without encoder on post statement"
    (is (= [{:from :resource-name
             :method :post
             :with {:bag {:capacity 10}
                    :name ["a" "b"]}}]
           (apply-encoders nil
                           [{:from :resource-name
                             :method :post
                             :with {:bag {:capacity 10}
                                    :name ["a" "b"]}}]))))
  (testing "Resolve with encoder on post statement"
    (is (= [{:from :resource-name
             :method :post
             :with {:bag "{\"capacity\":10}"
                    :name ["a" "b"]}}]
           (apply-encoders nil
                           [{:from :resource-name
                             :method :post
                             :with {:bag ^{:encoder :json} {:capacity 10}
                                    :name ["a" "b"]}}])))))
