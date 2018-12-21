(ns restql.core.encoders.core-test
  (:require [slingshot.slingshot :refer [try+]]
            [clojure.test :refer [deftest is]])
  (:use restql.core.encoders.core))

(deftest test-simple-values
  (is (= "10"   (encode base-encoders 10)))
  (is (= "true" (encode base-encoders true)))
  (is (nil? (encode base-encoders nil))))

(deftest test-simple-values-whithout-encoders
  (is (= "10"   (encode nil 10)))
  (is (= "true" (encode nil true)))
  (is (nil? (encode nil nil))))

