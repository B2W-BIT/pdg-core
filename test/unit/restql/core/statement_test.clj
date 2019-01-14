(ns restql.core.statement-test
  (:require [clojure.test :refer :all]
            [restql.core.statement :as statement])
)

(deftest expand-test
  (testing "Do nothing if theres no with"
    (is (= [{:from :resource-name}]
           (statement/expand {:from :resource-name})
        )
    )
  )

  (testing "Single value with returns self"
    (is (= [{:from :resource-name :with {:id 1}}]
           (statement/expand {:from :resource-name :with {:id 1}})
        )
    )
  )

  (testing "List value with returns one statement for each value"
    (is (= [{:from :resource-name :with {:id 1} :multiplexed true}, {:from :resource-name :with {:id 2} :multiplexed true}]
           (statement/expand {:from :resource-name :with {:id [1, 2]}})
        )
    )
    (is (= [{:from :resource-name :with {:name "a"} :multiplexed true}, {:from :resource-name :with {:name "b"} :multiplexed true}]
           (statement/expand {:from :resource-name :with {:name ["a", "b"]}})
        )
    )
  )

  (testing "Expand list value with keeps non list values"
    (is (= [{:from :resource-name :with {:id 1 :name "a"} :multiplexed true}, {:from :resource-name :with {:id 2 :name "a"} :multiplexed true}]
           (statement/expand {:from :resource-name :with {:id [1, 2] :name "a"}})
      )
    )
  )

  (testing "Expand multiple list value with keeps non list values"
    (is (= [{:from :resource-name :with {:id 1 :name "a" :job "clojurist"} :multiplexed true}, {:from :resource-name :with {:id 2 :name "b" :job "clojurist"} :multiplexed true}]
           (statement/expand {:from :resource-name :with {:id [1, 2] :name ["a", "b"] :job "clojurist"}})
        )
    )
  )

  (testing "Expand multiple list value with keeps non list values"
    (is (= [{:from :resource-name :with {:id [1, 2]}}]
           (statement/expand {:from :resource-name :with {:id ^{:expand false} [1, 2]}})
        )
    )
  )

  (testing "Expand multiple list value with keeps non list values"
    (is (= [{:from :resource-name :with {:id 1} :multiplexed true}]
           (statement/expand {:from :resource-name :with {:id [1]}})
        )
    )
  )

  (testing "Expand multiple list value with keeps non list values"
    (is (= [[{:from :resource-name :with {:id 1} :multiplexed true}] [{:from :resource-name :with {:id 2} :multiplexed true}]]
           (statement/expand {:from :resource-name :with {:id [[1] [2]]}})
        )
    )
  )

  (testing "Expand multiple list value with keeps non list values"
    (is (= [{:from :resource-name :with {:id 1 :name "a" :job "clojurist"} :multiplexed true} {:from :resource-name :with {:id nil :name "b" :job "clojurist"} :multiplexed true}]
           (statement/expand {:from :resource-name :with {:id [1 nil] :name ["a", "b"] :job "clojurist"}})
        )
    )
  )
)

(deftest resolve-chained-values-test
  (testing "Do nothing if theres no with chained"
    (is (= {:from :resource-name :with {:id 1}}
           (statement/resolve-chained-values {:from :resource-name :with {:id 1}}
                                             {})
        )
    )
  )

  (testing "Returns a statement with single done resource value"
    (is (= {:from :resource-name :with {:id 1}}
           (statement/resolve-chained-values {:from :resource-name :with {:id [:done-resource :id]}}
                                             {:done [[:done-resource {:body {:id 1}}]]})
        )
    )

    (is (= {:from :resource-name :with {:id 1 :name "clojurist"}}
           (statement/resolve-chained-values {:from :resource-name :with {:id 1 :name [:done-resource :resource-id]}}
                                             {:done [[:done-resource {:body {:resource-id "clojurist"}}]]})
        )
    )

    (is (= {:from :resource-name :with {:id 1 :name ["clojurist"]}}
           (statement/resolve-chained-values {:from :resource-name :with {:id 1 :name [:done-resource :resource-id]}}
                                             {:done [[:done-resource {:body {:resource-id ["clojurist"]}}]]})
        )
    )
  )

  (testing "Returns a statement with multiple done resource value"
    (is (= {:from :resource-name :with {:id [1 2]}}
           (statement/resolve-chained-values {:from :resource-name :with {:id [:done-resource :id]}}
                                             {:done [[:done-resource [{:body {:id 1}}
                                                                      {:body {:id 2}}]]]})
        )
    )
  )

  (testing "Returns a statement with single list value"
    (is (= {:from :resource-name :with {:id [1 2] :name ["a" "b"]}}
           (statement/resolve-chained-values {:from :resource-name :with {:id [:done-resource :id] :name ["a" "b"]}}
                                             {:done [[:done-resource {:body {:id [1 2]}}]]})
        )
    )
  )

  (testing "Returns a statement with single list value"
    (is (= {:from :resource-name :with {:id [[1 2] [2 3]] :name ["a" "b"]}}
           (statement/resolve-chained-values {:from :resource-name :with {:id [:done-resource :id] :name ["a" "b"]}}
                                             {:done [[:done-resource [{:body {:id [1 2]}}
                                                                      {:body {:id [2 3]}}]]]})
        )
    )
  )

    (testing "Returns a statement with multiple list value"
      (is (=  {:from :sidekick :with {:id [ [1 2] [3 4] ] } :method :get}
              (statement/resolve-chained-values {:from :sidekick :with {:id [:heroes :sidekickId]} :method :get}
                                                {:done [[:heroes {:resource :heroes :body [{:id "A" :sidekickId [1 2]}
                                                                                            {:id "B" :sidekickId [3 4]}]}]]
                                                :requested []
                                                :to-do [[:sidekick {:from :sidekick :with {:id [:heroes :sidekickId]} :method :get}]]})
              )
          )
      (is (=  {:from :sidekick :with {:id [[ [1 2] [3 4] ]] } :method :get}
          (statement/resolve-chained-values {:from :sidekick :with {:id [:heroes :sidekickId]} :method :get}
                                            {:done [[:heroes [{:resource :heroes :body [{:id "A" :sidekickId [1 2]}
                                                                                          {:id "B" :sidekickId [3 4]}]}]]]
                                              :requested []
                                              :to-do [[:sidekick {:from :sidekick :with {:id [:heroes :sidekickId]} :method :get}]]})
          )
      )
  )

  (testing "Returns a statement with single list value"
    (is (= {:from :resource-name :with {:id [1 nil] :name ["a" "b"]}}
           (statement/resolve-chained-values {:from :resource-name :with {:id [:done-resource :id] :name ["a" "b"]}}
                                             {:done [[:done-resource [{:body {:id 1 :class "rest"}}
                                                                      {:body {:id nil :class "rest"}}]]]})
        )
    )
  )

  (testing "Returns a statement with empty param"
    (is (= {:from :resource-name :with {:id [1 nil] :name ["a" "b"]}}
           (statement/resolve-chained-values {:from :resource-name :with {:id [:done-resource :id] :name ["a" "b"]}}
                                             {:done [[:done-resource [{:body {:id 1 :class "rest"}}
                                                                      {:body {:class "rest"}}]]]})
        )
    )
  )

  (testing "Resolve with encoder"
    (is (= (binding [*print-meta* true]
                    (pr-str {:from :resource-name :with {:id ^{:encoder :json} [1 nil] :name ["a" "b"]}}))
           (binding [*print-meta* true]
                    (pr-str (statement/resolve-chained-values {:from :resource-name
                                                               :with {:id ^{:encoder :json} [:done-resource :id] :name ["a" "b"]}}
                                                              {:done [[:done-resource [{:body {:id 1 :class "rest"}}
                                                                                       {:body {:class "rest"}}]]]})))
        )
    )
  )

  (testing "Resolve with encoder on single return value"
    (is (= (binding [*print-meta* true]
                    (pr-str {:from :resource-name :with {:id ^{:encoder :json} {} :name ["a" "b"]}}))
           (binding [*print-meta* true]
                    (pr-str (statement/resolve-chained-values {:from :resource-name
                                                               :with {:id ^{:encoder :json} [:done-resource :id] :name ["a" "b"]}}
                                                              {:done [[:done-resource {:body {:id {} :class "rest"}}]]})))
        )
    )
  )
)

(deftest apply-encoders-test
  (testing "Resolve with encoder on single return value"
    (is (= [{:from :resource-name :with {:bag "{\"capacity\":10}" :name "[\"a\" \"b\"]"}}]
           (statement/apply-encoders nil
                                     [{:from :resource-name
                                      :with {:bag {:capacity 10} :name ["a" "b"]}}])
        )
    )
  )
)
