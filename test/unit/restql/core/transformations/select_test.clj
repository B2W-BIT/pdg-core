(ns restql.core.transformations.select-test
  (:require [clojure.test :refer [deftest is]])
  (:use restql.core.transformations.select))

(deftest testing-simple-select
  (is (=
    {:cart {:details {:status 200 :success true} 
            :result {:id 1 :lines 2}} 
     :customer {:details {:status 200 :success true} 
                :result {:another 5}}}

     (select
       [:cart {:select #{:id :lines}}
        :customer {:select #{:another}}]

       {:cart {:details {:status 200 :success true} 
               :result {:id 1 :lines 2 :name 3}}
        :customer {:details {:status 200 :success true} 
                   :result {:other 4 :another 5}}}))))

(deftest testing-nested-selection
  (is (=
    {:data {:details {:status 200 :success true}
            :result {:top {:foo 1}}}}

    (select
      [:data {:select #{[:top #{:foo}]}}]
      {:data {:details {:status 200 :success true}
              :result {:top {:foo 1 :bar 2}}}}))))

(deftest testing-simple-filter
  (is (= 
    {:data {:details {:status 200 :success true}
            :result {:foo ["abcdef"]}}}

    (select
      [:data {:select #{[:foo {:matches "^abc.*"}]}}]
      {:data {:details {:status 200 :success true}
              :result {:foo ["bla" "abcdef" "bar"]}}})))
  (is (=
        {:data {:details {:status 200 :success true}
                :result {:foo ["abcdef"]}}}

        (select
          [:data {:select #{[:foo {:matches "^abc"}]}}]
          {:data {:details {:status 200 :success true}
                  :result {:foo ["bla" "abcdef" "bar"]}}}))))

(deftest testing-nested-filter
  (is (=  
    {:data {:details {:status 200 :success true}
            :result {:foo [{:text "abc"}]}}}

    (select
      [:data {:select #{[:foo {:matches {:field :text :value "^abc.*"}}]}}]
      {:data {:details {:status 200 :success true}
              :result {:foo [{:text "abc"}
                            {:text "xyz"}]}}}))))
