(ns restql.core.extractor-test
  (:require [clojure.test :refer [deftest is]])
  (:use restql.core.extractor))

(deftest simple-traverse-test
  (is (=
    "123"
    (traverse {:customer {:id "123"}} [:customer :id]))))

(deftest vector-traverse-test
  (is (=
    ["1" "2"]
    (traverse {:lines [{:id "1"} {:id "2"}]} [:lines :id]))))
