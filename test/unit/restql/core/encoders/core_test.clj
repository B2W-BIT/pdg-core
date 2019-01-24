(ns restql.core.encoders.core-test
  (:require [slingshot.slingshot :refer [try+]]
            [clojure.test :refer [deftest is]])
  (:use restql.core.encoders.core))

(deftest test-simple-values
  (is (= 10   (encode base-encoders 10)))
  (is (= true (encode base-encoders true)))
  (is (= "[\"a\",\"b\"]" (encode base-encoders ^{:encoder :json} ["a" "b"])))
  (is (= "[1,2]" (encode base-encoders ^{:encoder :json} [1 2])))
  (is (nil? (encode base-encoders nil))))

(deftest test-simple-values-whithout-encoders
  (is (= 10   (encode nil 10)))
  (is (= true (encode nil true)))
  (is (= "a" (encode nil "a")))
  (is (= ["a" "b"] (encode nil ["a" "b"])))
  (is (= [100.00] (encode nil [100.00])))
  (is (nil? (encode nil nil))))

