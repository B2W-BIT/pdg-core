(ns restql.core.statement.explode-list-vals-in-new-maps-test
  (:require [clojure.test :refer [deftest is]])
  (:use restql.core.statement.explode-list-vals-in-new-maps))

(deftest explode-list-vals-in-new-maps-test
  (is (= {:ids 1 :type 3}
         (explode-list-vals-in-new-maps {:ids 1 :type 3})))

  (is (= [{:ids 1 :type 3} {:ids 2 :type 3}]
         (explode-list-vals-in-new-maps {:ids [1 2] :type 3})))

  (is (= {:ids [1 2] :type 3}
         (explode-list-vals-in-new-maps {:ids ^{:expand false} [1 2] :type 3})))

  (is (= [{:ids [1 2] :type 3 :lines "a"} {:ids [1 2] :type 3 :lines "b"}]
         (explode-list-vals-in-new-maps {:ids ^{:expand false} [1 2] :type 3 :lines ["a" "b"]})))

  (is (= [{:ids 1 :type 3 :lines {:n ["a" "b"]}} {:ids 2 :type 3 :lines {:n ["a" "b"]}}]
         (explode-list-vals-in-new-maps {:ids [1 2] :type 3 :lines {:n ["a" "b"]}}))))
