(ns restql.core.statement.resolve-chained-values-test
  (:require [clojure.test :refer :all]
            [restql.core.statement.resolve-chained-values :refer [resolve-chained-values]]))

(deftest resolve-chained-values-test
  (testing "Do nothing if theres no with chained"
    (is (= {:from :resource-name :with {:id 1}}
           (resolve-chained-values {:from :resource-name :with {:id 1}}
                                   {}))))

  (testing "Returns a statement with single done resource value"
    (is (= {:from :resource-name :with {:id 1}}
           (resolve-chained-values {:from :resource-name :with {:id [:done-resource :id]}}
                                   [[:done-resource {:body {:id 1}}]])))

    (is (= {:from :resource-name :with {:id 1 :name "clojurist"}}
           (resolve-chained-values {:from :resource-name :with {:id 1 :name [:done-resource :resource-id]}}
                                   [[:done-resource {:body {:resource-id "clojurist"}}]])))

    (is (= {:from :resource-name :with {:id 1 :name ["clojurist"]}}
           (resolve-chained-values {:from :resource-name :with {:id 1 :name [:done-resource :resource-id]}}
                                   [[:done-resource {:body {:resource-id ["clojurist"]}}]]))))

  (testing "Returns a statement with multiple done resource value"
    (is (= {:from :resource-name :with {:id [1 2]}}
           (resolve-chained-values {:from :resource-name :with {:id [:done-resource :id]}}
                                   [[:done-resource [{:body {:id 1}} {:body {:id 2}}]]]))))

  (testing "Returns a statement with single list value"
    (is (= {:from :resource-name :with {:id [1 2] :name ["a" "b"]}}
           (resolve-chained-values {:from :resource-name :with {:id [:done-resource :id] :name ["a" "b"]}}
                                   [[:done-resource {:body {:id [1 2]}}]]))))

  (testing "Returns a statement with single list value"
    (is (= {:from :resource-name :with {:id [[1 2] [2 3]] :name ["a" "b"]}}
           (resolve-chained-values {:from :resource-name :with {:id [:done-resource :id] :name ["a" "b"]}}
                                   [[:done-resource [{:body {:id [1 2]}}
                                                     {:body {:id [2 3]}}]]]))))

  (testing "Returns a statement with multiple list value"
    (is (=  {:from :sidekick :with {:id [[1 2] [3 4]]} :method :get}
            (resolve-chained-values {:from :sidekick :with {:id [:heroes :sidekickId]} :method :get}
                                    [[:heroes {:resource :heroes :body [{:id "A" :sidekickId [1 2]}
                                                                        {:id "B" :sidekickId [3 4]}]}]])))
    (is (=  {:from :sidekick :with {:id [[[1 2] [3 4]]]} :method :get}
            (resolve-chained-values {:from :sidekick :with {:id [:heroes :sidekickId]} :method :get}
                                    [[:heroes [{:resource :heroes :body [{:id "A" :sidekickId [1 2]}
                                                                         {:id "B" :sidekickId [3 4]}]}]]]))))

  (testing "Returns a statement with single list value"
    (is (= {:from :resource-name :with {:id [1 nil] :name ["a" "b"]}}
           (resolve-chained-values {:from :resource-name :with {:id [:done-resource :id] :name ["a" "b"]}}
                                   [[:done-resource [{:body {:id 1 :class "rest"}} {:body {:id nil :class "rest"}}]]]))))

  (testing "Returns a statement with empty param"
    (is (= {:from :resource-name :with {:id [1 nil] :name ["a" "b"]}}
           (resolve-chained-values {:from :resource-name :with {:id [:done-resource :id] :name ["a" "b"]}}
                                   [[:done-resource [{:body {:id 1 :class "rest"}} {:body {:class "rest"}}]]]))))

  (testing "Resolve a statement with lists and nested values"
    (is (= {:from :done-resource :with {:name ["clojure" "java"]}}
           (resolve-chained-values {:from :done-resource :with {:name [:resource-id :language :id]}}
                                   [[:resource-id {:body {:language {:id ["clojure" "java"]}}}]])))

    (is (= {:from :done-resource :with {:name "clojure"}}
           (resolve-chained-values {:from :done-resource :with {:name [:resource-id :language :id]}}
                                   [[:resource-id {:body {:language {:id "clojure"}}}]])))


    (is (= {:from :done-resource :with {:name ["clojure"]}}
           (resolve-chained-values {:from :done-resource :with {:name [:resource-id :language :id]}}
                                   [[:resource-id {:body {:language [{:id "clojure"}]}}]])))

    (is (= {:from :done-resource :with {:name ["clojure" "java"]}}
           (resolve-chained-values {:from :done-resource :with {:name [:resource-id :language :id]}}
                                   [[:resource-id {:body {:language [{:id "clojure"} {:id "java"}]}}]])))


    (is (= {:from :done-resource :with {:name ["python" "elixir"]}}
           (resolve-chained-values {:from :done-resource :with {:name [:resource-id :language :xpto :id]}}
                                   [[:resource-id {:body {:language [{:xpto {:id "python"}} {:xpto {:id "elixir"}}]}}]])))

    (is (= {:from :done-resource :with {:name [["python" "123"] ["elixir" "345"]]}}
           (resolve-chained-values {:from :done-resource :with {:name [:resource-id :language :xpto :id]}}
                                   [[:resource-id {:body {:language [{:xpto {:id ["python" "123"]}} {:xpto {:id ["elixir" "345"]}}]}}]])))

    (is (= {:from :done-resource :with {:name [["python" "123"] ["elixir" "345"]]}}
           (resolve-chained-values {:from :done-resource :with {:name [:resource-id :language :xpto :asdf :id]}}
                                   [[:resource-id {:body {:language [{:xpto {:asdf [{:id "python"} {:id "123"}]}} {:xpto {:asdf [{:id "elixir"} {:id "345"}]}}]}}]])))

    (is (= {:from :weapon, :in :villain.weapons, :with {:id [[["DAGGER"] ["GUN"]] [["SWORD"] ["SHOTGUN"]]]}, :method :get}
           (resolve-chained-values {:from :weapon, :in :villain.weapons, :with {:id [:villain :weapons]}, :method :get}
                                   [[:villain [[{:body {:name "1", :weapons ["DAGGER"]}} {:body {:name "2", :weapons ["GUN"]}}]
                                               [{:body {:name "3", :weapons ["SWORD"]}} {:body {:name "4", :weapons ["SHOTGUN"]}}]]]]))))

  (testing "Resolve with encoder"
    (is (= (binding [*print-meta* true]
             (pr-str {:from :resource-name :with {:id ^{:encoder :json} [1 nil] :name ["a" "b"]}}))
           (binding [*print-meta* true]
             (pr-str (resolve-chained-values {:from :resource-name :with {:id ^{:encoder :json} [:done-resource :id] :name ["a" "b"]}}
                                             [[:done-resource [{:body {:id 1 :class "rest"}} {:body {:class "rest"}}]]]))))))

  (testing "Resolve with encoder on single return value"
    (is (= (binding [*print-meta* true]
             (pr-str {:from :resource-name :with {:id ^{:encoder :json} {} :name ["a" "b"]}}))
           (binding [*print-meta* true]
             (pr-str (resolve-chained-values {:from :resource-name :with {:id ^{:encoder :json} [:done-resource :id] :name ["a" "b"]}}
                                             [[:done-resource {:body {:id {} :class "rest"}}]])))))))
