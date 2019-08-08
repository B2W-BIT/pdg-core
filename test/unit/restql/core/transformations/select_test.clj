(ns restql.core.transformations.select-test
  (:require [clojure.test :refer [deftest is]]
            [restql.core.transformations.select :as select]))

(deftest testing-simple-select
  (is (= {:cart {:details {:status 200 :success true}
                 :result {:id 1 :lines 2}}
          :customer {:details {:status 200 :success true}
                     :result {:another 5}}}
         (select/from-result [:cart {:select [[:id] [:lines]]}
                              :customer {:select [[:another]]}]
                             {:cart {:details {:status 200 :success true}
                                     :result {:id 1 :lines 2 :name 3}}
                              :customer {:details {:status 200 :success true}
                                         :result {:other 4 :another 5}}}))))

(deftest testing-list-result
  (is (= {:heroes {:details {:status 200 :success true}
                   :result [{:id "B10"} {:id "B20"}]}
          :hero {:details [{:status 200 :success true}
                           {:success true :status 200}]
                 :result [{:name "Batman"}
                          {:name "Robin"}]}}
         (select/from-result [:heroes {:from :heroes
                                       :method :get}
                              :hero   {:from :hero
                                       :method :get
                                       :with {:id [:heroes :id]}
                                       :select [[:name]]}]
                             {:heroes {:details {:status 200 :success true}
                                       :result [{:id "B10"} {:id "B20"}]}
                              :hero {:details [{:status 200 :success true}
                                               {:success true, :status 200}]
                                     :result [{:name "Batman", :id "B10"} {:name "Robin", :id "B10"}]}})))
  (is (= {:heroes {:details {:status 200 :success true}
                   :result [{:id "B10"} {:id "B20"}]}
          :hero {:details [{:status 200 :success true}
                           {:success true :status 200}]
                 :result [[{:name "Batman"}]
                          [{:name "Robin"}]]}}
         (select/from-result [:heroes {:from :heroes
                                       :method :get}
                              :hero   {:from :hero
                                       :method :get
                                       :with {:id [:heroes :id]}
                                       :select [[:name]]}]
                             {:heroes {:details {:status 200 :success true}
                                       :result [{:id "B10"} {:id "B20"}]}
                              :hero {:details [{:status 200 :success true}
                                               {:success true, :status 200}]
                                     :result [[{:name "Batman", :id "B10"}] [{:name "Robin", :id "B10"}]]}})))
  (is (= {:heroes {:details {:status 200 :success true}
                   :result [{:id "B10"} {:id "B20"}]}
          :hero {:details [{:status 200 :success true}
                           {:success true :status 200}]
                 :result [[{:name "Batman"}] []]}}
         (select/from-result [:heroes {:from :heroes
                                       :method :get}
                              :hero   {:from :hero
                                       :method :get
                                       :with {:id [:heroes :id]}
                                       :select [^{:equals "Batman"} [:name]]}]
                             {:heroes {:details {:status 200 :success true}
                                       :result [{:id "B10"} {:id "B20"}]}
                              :hero {:details [{:status 200 :success true}
                                               {:success true, :status 200}]
                                     :result [[{:name "Batman", :id "B10"}] [{:name "Robin" :id "B10"}]]}})))
  (is (= {:heroes {:details {:status 200 :success true}
                   :result [{:id "B10"} {:id "B20"}]}
          :hero {:details [{:status 200 :success true}
                           {:success true :status 200}]
                 :result [[{:name "Batman" :id "B10"}]
                          [{:name "Robin" :id "B20"}]]}}
         (select/from-result [:heroes {:from :heroes
                                       :method :get}
                              :hero   {:from :hero
                                       :method :get
                                       :with {:id [:heroes :id]}
                                       :select [[:name] [:id]]}]
                             {:heroes {:details {:status 200 :success true}
                                       :result [{:id "B10"} {:id "B20"}]}
                              :hero {:details [{:status 200 :success true}
                                               {:success true, :status 200}]
                                     :result [[{:name "Batman", :id "B10"}] [{:name "Robin", :id "B20"}]]}}))))

(deftest testing-nested-selection
  (is (= {:data {:details {:status 200 :success true}
                 :result {:top {:foo 1}}}}
         (select/from-result [:data {:select [[:top :foo]]}]
                             {:data {:details {:status 200 :success true}
                                     :result {:top {:foo 1 :bar 2}}}})))
  (is (= {:data {:details {:status 200 :success true}
                 :result {:top {:foo 1 :bar {:deepbar 2}}}}}
         (select/from-result [:data {:select [[:top :foo] [:top :bar :deepbar]]}]
                             {:data {:details {:status 200 :success true}
                                     :result {:top {:foo 1 :bar {:deepbar 2 :any 3} :another 4}}}}))))

(deftest testing-simple-filter
  (is (= {:data {:details {:status 200 :success true}
                 :result {:foo ["abcdef"]}}}
         (select/from-result [:data {:select [^{:matches "^abc.*"} [:foo]]}]
                             {:data {:details {:status 200 :success true}
                                     :result {:foo ["bla" "abcdef" "bar"] :bar 1}}})))
  (is (= {:data {:details {:status 200 :success true}
                 :result {:foo ["abcdef"]}}}
         (select/from-result [:data {:select [^{:matches "^abc"} [:foo]]}]
                             {:data {:details {:status 200 :success true}
                                     :result {:foo ["bla" "abcdef" "bar"] :bar 1}}})))
  (is (= {:data {:details {:status 200 :success true}
                 :result [[{:foo ["abcdef"]}] [{:foo ["abc123"]}]]}}
         (select/from-result [:data {:select [^{:matches "^abc"} [:foo]]}]
                             {:data {:details {:status 200 :success true}
                                     :result [[{:foo ["bla" "abcdef" "bar"] :bar 1}] [{:foo ["bla" "abc123" "bar"]}]]}}))))

(deftest testing-simple-filter-and-others
  (is (= {:data {:details {:status 200 :success true}
                 :result {:foo ["abcdef"] :bar 1}}}
         (select/from-result [:data {:select [^{:matches "^abc.*"} [:foo] [:*]]}]
                             {:data {:details {:status 200 :success true}
                                     :result {:foo ["bla" "abcdef" "bar"] :bar 1}}})))
  (is (= {:data {:details {:status 200 :success true}
                 :result {:foo ["abcdef"] :bar 1}}}
         (select/from-result [:data {:select [^{:matches "^abc"} [:foo] [:*]]}]
                             {:data {:details {:status 200 :success true}
                                     :result {:foo ["bla" "abcdef" "bar"] :bar 1}}}))))

(deftest testing-nested-filter
  (is (= {:data {:details {:status 200 :success true}
                 :result {:foo [{:text "abc"}]}}}

         (select/from-result [:data {:select [^{:matches "^abc.*"} [:foo :text]]}]
                             {:data {:details {:status 200 :success true}
                                     :result {:foo [{:text "abc"}
                                                    {:text "xyz"}]}}}))))

(deftest bla
  (is (= {:heroes {:details {:status 200 :success true}
                   :result [{:id "B10"} {:id "B20"}]}
          :hero {:details [{:status 200 :success true}
                           {:success true :status 200}]
                 :result [[{:name "Batman" :id "B10"}]
                          [{:name "Robin" :id "B20"}]]}}
         (select/from-result [:heroes {:from :heroes
                                       :method :get}
                              :hero   {:from :hero
                                       :method :get
                                       :with {:id [:heroes :id]}
                                       :select [[:name] [:id]]}]
                             {:heroes {:details {:status 200 :success true}
                                       :result [{:id "B10"} {:id "B20"}]}
                              :hero {:details [{:status 200 :success true}
                                               {:success true, :status 200}]
                                     :result [[{:name "Batman", :id "B10"}] [{:name "Robin", :id "B20"}]]}}))))
