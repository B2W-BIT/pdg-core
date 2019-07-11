(ns restql.hooks.core-test
  (:require [clojure.test :refer :all]
            [restql.hooks.core :as hooks]))

(deftest testing-register-hooks
  (testing "Register :testing hook"
    (hooks/register-hook :testing [1])
    (is (= [1] (:testing @hooks/hook-store)))
    (reset! hooks/hook-store {})))

(deftest testing-execute-hook
  (testing "Execute simple hook fn"
    (let [cnt (atom 0)
          _   (hooks/register-hook :testing [(fn [ctx] (swap! cnt inc))])
          ctx (hooks/execute-hook :testing)]
      (is (= 1 @cnt))
      (is (= {} ctx))
      (reset! hooks/hook-store {})))

  (testing "Execute hook with context"
    (let [_   (hooks/register-hook :testing [(fn [ctx] (->> ctx :a inc (assoc {} :b)))])
          ctx (hooks/execute-hook :testing {:a 1})]
      (is (= {:b 2} ctx))
      (reset! hooks/hook-store {})))

  (testing "Execute 2 hooks with context"
    (let [_   (hooks/register-hook :testing [(fn [ctx] (->> ctx :a inc (assoc {} :b)))
                                             (fn [ctx] (->> ctx :a inc inc (assoc {} :c)))])
          ctx (hooks/execute-hook :testing {:a 1})]
      (is (= {:b 2 :c 3} ctx))
      (reset! hooks/hook-store {}))))

(deftest testing-execute-hook-pipeline
  (testing "Execute simple hook fn"
    (let [cnt (atom 0)
          _   (hooks/register-hook :testing [(fn [ctx] (swap! cnt inc))])
          ctx (hooks/execute-hook-pipeline :testing)]
      (is (= 1 @cnt))
      (is (= 1 ctx))
      (reset! hooks/hook-store {})))

  (testing "Execute simple hook with initial value"
    (let [_   (hooks/register-hook :testing [(fn [v] (+ v 1))])
          ctx (hooks/execute-hook-pipeline :testing 1)]
      (is (= 2 ctx))
      (reset! hooks/hook-store {})))

  (testing "Execute multiple hooks with initial value"
    (let [_   (hooks/register-hook :testing [(fn [v] (+ v 1))
                                             (fn [v] (+ v 3))])
          ctx (hooks/execute-hook-pipeline :testing 1)]
      (is (= 5 ctx))
      (reset! hooks/hook-store {}))))
