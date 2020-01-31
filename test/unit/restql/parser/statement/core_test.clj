(ns restql.parser.statement.core-test
  (:require [clojure.test :refer :all]
            [restql.parser.statement.core :as statement]))

(deftest resource-methods-rule
  (testing "from resource all methods"
    (is (= [:heroes {:from :heroes :method :get}]
           (statement/from-query {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")})})})})})))
    (is (= [:heroes  {:from :heroes  :method :get}
            :heroes2 {:from :heroes2 :method :post}
            :heroes3 {:from :heroes3 :method :put}
            :heroes4 {:from :heroes4 :method :delete}
            :heroes5 {:from :heroes5 :method :patch}]
           (statement/from-query {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")})})}
                                                        {:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("to")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes2")})})}
                                                        {:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("into")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes3")})})}
                                                        {:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("delete")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes4")})})}
                                                        {:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("update")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes5")})})})})})))))

(deftest resource-alias-rule
  (testing "from resource all methods"
    (is (= [:heroes-alias {:from :heroes :method :get}]
           (statement/from-query {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")}
                                                                              {:tag :ActionRuleAlias
                                                                               :content ("heroes-alias")})})})})})))))

(deftest resource-in-rule
  (testing "resource in another"
    (is (= [:heroes {:from :heroes :method :get :in :outter-hero}]
           (statement/from-query {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")}
                                                                              {:tag :ActionRuleIn
                                                                               :content ("outter-hero")})})})})})))))

(deftest resource-use-rule
  (testing "use timeout"
    (is (= (binding [*print-meta* true]
             (pr-str ^{:timeout 100} [:heroes {:from :heroes :method :get}]))
           (binding [*print-meta* true]
             (pr-str (statement/from-query {:tag :Query
                                            :content '({:tag :UseBlock
                                                        :content ({:tag :UseRule
                                                                   :content ({:tag :ModifierBlock
                                                                              :content ({:tag :TimeoutRule
                                                                                         :content ({:tag :Integer
                                                                                                    :content ("100")})})})})}
                                                       {:tag :QueryBlock
                                                        :content ({:tag :QueryItem
                                                                   :content ({:tag :ActionRule
                                                                              :content ({:tag :ActionRuleKey
                                                                                         :content ("from")}
                                                                                        {:tag :ActionRuleValue
                                                                                         :content ("heroes")})})})})}))))))
  (testing "use max-age s-max-age"
    (is (= (binding [*print-meta* true]
             (pr-str ^{:max-age 80 :s-max-age 50} [:heroes {:from :heroes :method :get}]))
           (binding [*print-meta* true]
             (pr-str (statement/from-query {:tag :Query
                                            :content '({:tag :UseBlock
                                                        :content ({:tag :UseRule
                                                                   :content ({:tag :ModifierBlock
                                                                              :content ({:tag :MaxAgeRule
                                                                                         :content ({:tag :Integer
                                                                                                    :content ("20")})}
                                                                                        {:tag :SMaxAgeRule
                                                                                         :content ({:tag :Integer
                                                                                                    :content ("50")})}
                                                                                        {:tag :MaxAgeRule
                                                                                         :content ({:tag :Integer
                                                                                                    :content ("80")})})})})}
                                                       {:tag :QueryBlock
                                                        :content ({:tag :QueryItem
                                                                   :content ({:tag :ActionRule
                                                                              :content ({:tag :ActionRuleKey
                                                                                         :content ("from")}
                                                                                        {:tag :ActionRuleValue
                                                                                         :content ("heroes")})})})})}))))))
  (testing "use headers"
    (is (= (binding [*print-meta* true]
             (pr-str ^{:with-headers {"Authorization" "XPTO" "X-Bla" "XPTO"}} [:heroes {:from :heroes :method :get}]))
           (binding [*print-meta* true]
             (pr-str (statement/from-query {:tag :Query
                                            :content '({:tag :UseBlock
                                                        :content ({:tag :UseRule
                                                                   :content ({:tag :ModifierBlock
                                                                              :content ({:tag :HeaderRule
                                                                                         :content ({:tag :HeaderRuleItem
                                                                                                    :content ({:tag :HeaderName
                                                                                                               :content ("Authorization")}
                                                                                                              {:tag :HeaderValue
                                                                                                               :content ({:tag :String
                                                                                                                          :content ("\"XPTO\"")})})}
                                                                                                   {:tag :HeaderRuleItem
                                                                                                    :content ({:tag :HeaderName
                                                                                                               :content ("X-Bla")}
                                                                                                              {:tag :HeaderValue
                                                                                                               :content ({:tag :String
                                                                                                                          :content ("\"XPTO\"")})})})})})})}
                                                       {:tag :QueryBlock
                                                        :content ({:tag :QueryItem
                                                                   :content ({:tag :ActionRule
                                                                              :content ({:tag :ActionRuleKey
                                                                                         :content ("from")}
                                                                                        {:tag :ActionRuleValue
                                                                                         :content ("heroes")})})})})}))))))
  (testing "use timeout \n use max-age"
    (is (= (binding [*print-meta* true]
             (pr-str ^{:timeout 100 :max-age 200} [:heroes {:from :heroes :method :get}]))
           (binding [*print-meta* true]
             (pr-str (statement/from-query {:tag :Query
                                            :content '({:tag :UseBlock
                                                        :content ({:tag :UseRule
                                                                   :content ({:tag :ModifierBlock
                                                                              :content ({:tag :TimeoutRule
                                                                                         :content ({:tag :Integer
                                                                                                    :content ("100")})})})}
                                                                  {:tag :UseRule
                                                                   :content ({:tag :ModifierBlock
                                                                              :content ({:tag :MaxAgeRule
                                                                                         :content ({:tag :Integer
                                                                                                    :content ("200")})})})})}
                                                       {:tag :QueryBlock
                                                        :content ({:tag :QueryItem
                                                                   :content ({:tag :ActionRule
                                                                              :content ({:tag :ActionRuleKey
                                                                                         :content ("from")}
                                                                                        {:tag :ActionRuleValue
                                                                                         :content ("heroes")})})})})})))))))

(deftest resource-with-rule
  (testing "simple param"
    (is (= [:heroes {:from :heroes :method :get :with {:id 1}}]
           (statement/from-query {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")})}
                                                                   {:tag :WithRule
                                                                    :content ({:tag :WithRuleItem
                                                                               :content ({:tag :WithParamName
                                                                                          :content ("id")}
                                                                                         {:tag :WithParamValue
                                                                                          :content ({:tag :Integer
                                                                                                     :content ("1")}
                                                                                                    {:tag :ModifierList
                                                                                                     :content nil})})})})})})}))))
  (testing "null param"
    (is (= [:heroes {:from :heroes :method :get :with {:id 1 :skill nil}}]
           (statement/from-query {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")})}
                                                                   {:tag :WithRule
                                                                    :content ({:tag :WithRuleItem
                                                                               :content ({:tag :WithParamName
                                                                                          :content ("id")}
                                                                                         {:tag :WithParamValue
                                                                                          :content ({:tag :Integer
                                                                                                     :content ("1")}
                                                                                                    {:tag :ModifierList
                                                                                                     :content nil})})}
                                                                              {:tag :WithRuleItem
                                                                               :content ({:tag :WithParamName
                                                                                          :content ("skill")}
                                                                                         {:tag :WithParamValue
                                                                                          :content ({:tag :Null
                                                                                                     :content nil}
                                                                                                    {:tag :ModifierList
                                                                                                     :content nil})})})})})})}))))
  (testing "boolean param"
    (is (= [:heroes {:from :heroes :method :get :with {:id 1 :skill true}}]
           (statement/from-query {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")})}
                                                                   {:tag :WithRule
                                                                    :content ({:tag :WithRuleItem
                                                                               :content ({:tag :WithParamName
                                                                                          :content ("id")}
                                                                                         {:tag :WithParamValue
                                                                                          :content ({:tag :Integer
                                                                                                     :content ("1")}
                                                                                                    {:tag :ModifierList
                                                                                                     :content nil})})}
                                                                              {:tag :WithRuleItem
                                                                               :content ({:tag :WithParamName
                                                                                          :content ("skill")}
                                                                                         {:tag :WithParamValue
                                                                                          :content ({:tag :True
                                                                                                     :content nil}
                                                                                                    {:tag :ModifierList
                                                                                                     :content nil})})})})})})})))
    (is (= [:heroes {:from :heroes :method :get :with {:id 1 :skill false}}]
           (statement/from-query {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")})}
                                                                   {:tag :WithRule
                                                                    :content ({:tag :WithRuleItem
                                                                               :content ({:tag :WithParamName
                                                                                          :content ("id")}
                                                                                         {:tag :WithParamValue
                                                                                          :content ({:tag :Integer
                                                                                                     :content ("1")}
                                                                                                    {:tag :ModifierList
                                                                                                     :content nil})})}
                                                                              {:tag :WithRuleItem
                                                                               :content ({:tag :WithParamName
                                                                                          :content ("skill")}
                                                                                         {:tag :WithParamValue
                                                                                          :content ({:tag :False
                                                                                                     :content nil}
                                                                                                    {:tag :ModifierList
                                                                                                     :content nil})})})})})})}))))
  (testing "list param"
    (is (= [:heroes {:from :heroes :method :get :with {:id ["warrior" "magician"]}}]
           (statement/from-query {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")})}
                                                                   {:tag :WithRule
                                                                    :content ({:tag :WithRuleItem
                                                                               :content ({:tag :WithParamName
                                                                                          :content ("id")}
                                                                                         {:tag :WithParamValue
                                                                                          :content ({:tag :ListParam
                                                                                                     :content ({:tag :WithParamValue
                                                                                                                :content ({:tag :String
                                                                                                                           :content ("\"warrior\"")}
                                                                                                                          {:tag :ModifierList
                                                                                                                           :content nil})}
                                                                                                               {:tag :WithParamValue
                                                                                                                :content ({:tag :String
                                                                                                                           :content ("\"magician\"")}
                                                                                                                          {:tag :ModifierList
                                                                                                                           :content nil})})}
                                                                                                    {:tag :ModifierList
                                                                                                     :content nil})})})})})})}))))
  (testing "list param with modifier"
    (is (= (binding [*print-meta* true]
             (pr-str [:heroes {:from :heroes :method :get :with {:id ^{:maches "abc" :expand false :encoder :json} ["warrior" "magician"]}}]))
           (binding [*print-meta* true]
             (pr-str (statement/from-query {:tag :Query
                                            :content '({:tag :QueryBlock
                                                        :content ({:tag :QueryItem
                                                                   :content ({:tag :ActionRule
                                                                              :content ({:tag :ActionRuleKey
                                                                                         :content ("from")}
                                                                                        {:tag :ActionRuleValue
                                                                                         :content ("heroes")})}
                                                                             {:tag :WithRule
                                                                              :content ({:tag :WithRuleItem
                                                                                         :content ({:tag :WithParamName
                                                                                                    :content ("id")}
                                                                                                   {:tag :WithParamValue
                                                                                                    :content ({:tag :ListParam
                                                                                                               :content ({:tag :WithParamValue
                                                                                                                          :content ({:tag :String
                                                                                                                                     :content ("\"warrior\"")}
                                                                                                                                    {:tag :ModifierList
                                                                                                                                     :content nil})}
                                                                                                                         {:tag :WithParamValue
                                                                                                                          :content ({:tag :String
                                                                                                                                     :content ("\"magician\"")}
                                                                                                                                    {:tag :ModifierList
                                                                                                                                     :content nil})})}
                                                                                                              {:tag :ModifierList
                                                                                                               :content ({:tag :Modifier
                                                                                                                          :content ({:tag :ModifierFunction
                                                                                                                                     :content ({:tag :ModifierFunctionName
                                                                                                                                                :content ("maches")}
                                                                                                                                               {:tag :ModifierFunctionArgList
                                                                                                                                                :content ({:tag :String
                                                                                                                                                           :content ("\"abc\"")})})})}
                                                                                                                         {:tag :Modifier
                                                                                                                          :content ({:tag :ModifierAlias
                                                                                                                                     :content ("flatten")})}
                                                                                                                         {:tag :Modifier
                                                                                                                          :content ({:tag :ModifierAlias
                                                                                                                                     :content ("json")})})})})})})})})}))))))
  (testing "complex param"
    (is (= [:heroes {:from :heroes :method :get :with {:equip {:sword {:foo "bar"} :shield [1 2 3]}}}]
           (statement/from-query {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")})}
                                                                   {:tag :WithRule
                                                                    :content ({:tag :WithRuleItem
                                                                               :content ({:tag :WithParamName
                                                                                          :content ("equip")}
                                                                                         {:tag :WithParamValue
                                                                                          :content ({:tag :ComplexParam
                                                                                                     :content ({:tag :ComplexParamItem
                                                                                                                :content ({:tag :ComplexParamKey
                                                                                                                           :content ("sword")}
                                                                                                                          {:tag :WithParamValue
                                                                                                                           :content ({:tag :ComplexParam
                                                                                                                                      :content ({:tag :ComplexParamItem
                                                                                                                                                 :content ({:tag :ComplexParamKey
                                                                                                                                                            :content ("foo")}
                                                                                                                                                           {:tag :WithParamValue
                                                                                                                                                            :content ({:tag :String
                                                                                                                                                                       :content ("\"bar\"")}
                                                                                                                                                                      {:tag :ModifierList
                                                                                                                                                                       :content nil})})})}
                                                                                                                                     {:tag :ModifierList
                                                                                                                                      :content nil})})}
                                                                                                               {:tag :ComplexParamItem
                                                                                                                :content ({:tag :ComplexParamKey
                                                                                                                           :content ("shield")}
                                                                                                                          {:tag :WithParamValue
                                                                                                                           :content ({:tag :ListParam
                                                                                                                                      :content ({:tag :WithParamValue
                                                                                                                                                 :content ({:tag :Integer
                                                                                                                                                            :content ("1")}
                                                                                                                                                           {:tag :ModifierList
                                                                                                                                                            :content nil})}
                                                                                                                                                {:tag :WithParamValue
                                                                                                                                                 :content ({:tag :Integer
                                                                                                                                                            :content ("2")}
                                                                                                                                                           {:tag :ModifierList
                                                                                                                                                            :content nil})}
                                                                                                                                                {:tag :WithParamValue
                                                                                                                                                 :content ({:tag :Integer
                                                                                                                                                            :content ("3")}
                                                                                                                                                           {:tag :ModifierList
                                                                                                                                                            :content nil})})}
                                                                                                                                     {:tag :ModifierList
                                                                                                                                      :content nil})})})}
                                                                                                    {:tag :ModifierList
                                                                                                     :content nil})})})})})})}))))
  (testing "complex param with modifier"
    (is (= (binding [*print-meta* true]
             (pr-str [:sidekick {:from :sidekick :method :get :with {:source [:weapons :source]
                                                                     :weapon ^{:expand false} {:name ^{:expand false} [:weapons :swords :name]
                                                                                               :typeId [:weapons :types :typeId]
                                                                                               :invisible false}}}]))
           (binding [*print-meta* true]
             (pr-str (statement/from-query {:tag :Query
                                            :content '({:tag :QueryBlock
                                                        :content ({:tag :QueryItem
                                                                   :content ({:tag :ActionRule
                                                                              :content ({:tag :ActionRuleKey
                                                                                         :content ("from")}
                                                                                        {:tag :ActionRuleValue
                                                                                         :content ("sidekick")})}
                                                                             {:tag :WithRule
                                                                              :content ({:tag :WithRuleItem
                                                                                         :content ({:tag :WithParamName
                                                                                                    :content ("source")}
                                                                                                   {:tag :WithParamValue
                                                                                                    :content ({:tag :Chaining
                                                                                                               :content ({:tag :PathItem
                                                                                                                          :content ("weapons")}
                                                                                                                         {:tag :PathItem
                                                                                                                          :content ("source")})}
                                                                                                              {:tag :ModifierList
                                                                                                               :content nil})})}
                                                                                        {:tag :WithRuleItem
                                                                                         :content ({:tag :WithParamName
                                                                                                    :content ("weapon")}
                                                                                                   {:tag :WithParamValue
                                                                                                    :content ({:tag :ComplexParam
                                                                                                               :content ({:tag :ComplexParamItem
                                                                                                                          :content ({:tag :ComplexParamKey
                                                                                                                                     :content ("name")}
                                                                                                                                    {:tag :WithParamValue
                                                                                                                                     :content ({:tag :Chaining
                                                                                                                                                :content ({:tag :PathItem
                                                                                                                                                           :content ("weapons")}
                                                                                                                                                          {:tag :PathItem
                                                                                                                                                           :content ("swords")}
                                                                                                                                                          {:tag :PathItem
                                                                                                                                                           :content ("name")})}
                                                                                                                                               {:tag :ModifierList
                                                                                                                                                :content ({:tag :Modifier
                                                                                                                                                           :content ({:tag :ModifierAlias
                                                                                                                                                                      :content ("flatten")})})})})}
                                                                                                                         {:tag :ComplexParamItem
                                                                                                                          :content ({:tag :ComplexParamKey
                                                                                                                                     :content ("typeId")}
                                                                                                                                    {:tag :WithParamValue
                                                                                                                                     :content ({:tag :Chaining
                                                                                                                                                :content ({:tag :PathItem
                                                                                                                                                           :content ("weapons")}
                                                                                                                                                          {:tag :PathItem
                                                                                                                                                           :content ("types")}
                                                                                                                                                          {:tag :PathItem
                                                                                                                                                           :content ("typeId")})}
                                                                                                                                               {:tag :ModifierList
                                                                                                                                                :content nil})})}
                                                                                                                         {:tag :ComplexParamItem
                                                                                                                          :content ({:tag :ComplexParamKey
                                                                                                                                     :content ("invisible")}
                                                                                                                                    {:tag :WithParamValue
                                                                                                                                     :content ({:tag :False
                                                                                                                                                :content nil}
                                                                                                                                               {:tag :ModifierList
                                                                                                                                                :content nil})})})}
                                                                                                              {:tag :ModifierList
                                                                                                               :content ({:tag :Modifier
                                                                                                                          :content ({:tag :ModifierAlias
                                                                                                                                     :content ("flatten")})})})})})})})})}))))))
  (testing "variable param"
    (is (= [:heroes {:from :heroes :method :get :with {:id 123}}]
           (statement/from-query {"id" 123}
                                 {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")})}
                                                                   {:tag :WithRule
                                                                    :content ({:tag :WithRuleItem
                                                                               :content ({:tag :WithParamName
                                                                                          :content ("id")}
                                                                                         {:tag :WithParamValue
                                                                                          :content ({:tag :Variable
                                                                                                     :content ("id")}
                                                                                                    {:tag :ModifierList
                                                                                                     :content nil})})})})})})}))))
  (testing "complex variable param"
    (is (= [:heroes {:from :heroes :method :get :with {:name "Jiraiya" :age 45}}]
           (statement/from-query {"hero" {:name "Jiraiya" :age 45}}
                                 {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")})}
                                                                   {:tag :WithRule
                                                                    :content ({:tag :WithRuleItem
                                                                               :content ({:tag :Variable
                                                                                          :content ("hero")})})})})})}))))
  (testing "chaining param"
    (is (= [:heroes {:from :heroes :method :get :with {:id [:villain :id]}}]
           (statement/from-query {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")})}
                                                                   {:tag :WithRule
                                                                    :content ({:tag :WithRuleItem
                                                                               :content ({:tag :WithParamName
                                                                                          :content ("id")}
                                                                                         {:tag :WithParamValue
                                                                                          :content ({:tag :Chaining
                                                                                                     :content ({:tag :PathItem
                                                                                                                :content ("villain")}
                                                                                                               {:tag :PathItem
                                                                                                                :content ("id")})}
                                                                                                    {:tag :ModifierList
                                                                                                     :content nil})})})})})})}))))
  (testing "chaining param with variable"
    (is (= [:heroes {:from :heroes :method :get :with {:id [:villain :Venon]}}]
           (statement/from-query {"villain" "Venon"}
                                 {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")})}
                                                                   {:tag :WithRule
                                                                    :content ({:tag :WithRuleItem
                                                                               :content ({:tag :WithParamName
                                                                                          :content ("id")}
                                                                                         {:tag :WithParamValue
                                                                                          :content ({:tag :Chaining
                                                                                                     :content ({:tag :PathItem
                                                                                                                :content ("villain")}
                                                                                                               {:tag :PathVariable
                                                                                                                :content ("villain")})}
                                                                                                    {:tag :ModifierList
                                                                                                     :content nil})})})})})})}))))
  (testing "chaining param with variable"
    (is (= [:heroes {:from :heroes :method :get :with {:id [:bla :1 :bli], :name "Spider-Man"}}]
           (statement/from-query {"variable" 1 "name" "Spider-Man"}
                                 {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")})}
                                                                   {:tag :WithRule
                                                                    :content ({:tag :WithRuleItem
                                                                               :content ({:tag :WithParamName
                                                                                          :content ("id")}
                                                                                         {:tag :WithParamValue
                                                                                          :content ({:tag :Chaining
                                                                                                     :content ({:tag :PathItem
                                                                                                                :content ("bla")}
                                                                                                               {:tag :PathVariable
                                                                                                                :content ("variable")}
                                                                                                               {:tag :PathItem
                                                                                                                :content ("bli")})}
                                                                                                    {:tag :ModifierList
                                                                                                     :content nil})})}
                                                                              {:tag :WithRuleItem
                                                                               :content ({:tag :WithParamName
                                                                                          :content ("name")}
                                                                                         {:tag :WithParamValue
                                                                                          :content ({:tag :Variable
                                                                                                     :content ("name")}
                                                                                                    {:tag :ModifierList
                                                                                                     :content nil})})})})})})}))))
  (testing "chaining param with variable and modifiers"
    (is (= (binding [*print-meta* true]
             (pr-str [:heroes {:from :heroes
                               :method :get
                               :with {:name "Spider-Man"
                                      :id ^{:maches "abc" :expand false :encoder :json} [:villain :Venon]}}]))
           (binding [*print-meta* true]
             (pr-str (statement/from-query {"name" "Spider-Man" "villain" "Venon"}
                                           {:tag :Query
                                            :content '({:tag :QueryBlock
                                                        :content ({:tag :QueryItem
                                                                   :content ({:tag :ActionRule
                                                                              :content ({:tag :ActionRuleKey
                                                                                         :content ("from")}
                                                                                        {:tag :ActionRuleValue
                                                                                         :content ("heroes")})}
                                                                             {:tag :WithRule
                                                                              :content ({:tag :WithRuleItem
                                                                                         :content ({:tag :WithParamName
                                                                                                    :content ("name")}
                                                                                                   {:tag :WithParamValue
                                                                                                    :content ({:tag :Variable
                                                                                                               :content ("name")})})}
                                                                                        {:tag :WithRuleItem
                                                                                         :content ({:tag :WithParamName
                                                                                                    :content ("id")}
                                                                                                   {:tag :WithParamValue
                                                                                                    :content ({:tag :Chaining
                                                                                                               :content ({:tag :PathItem
                                                                                                                          :content ("villain")}
                                                                                                                         {:tag :PathVariable
                                                                                                                          :content ("villain")})}
                                                                                                              {:tag :ModifierList
                                                                                                               :content ({:tag :Modifier
                                                                                                                          :content ({:tag :ModifierFunction
                                                                                                                                     :content ({:tag :ModifierFunctionName
                                                                                                                                                :content ("maches")}
                                                                                                                                               {:tag :ModifierFunctionArgList
                                                                                                                                                :content ({:tag :String
                                                                                                                                                           :content ("\"abc\"")})})})}
                                                                                                                         {:tag :Modifier
                                                                                                                          :content ({:tag :ModifierAlias
                                                                                                                                     :content ("flatten")})}
                                                                                                                         {:tag :Modifier
                                                                                                                          :content ({:tag :ModifierAlias
                                                                                                                                     :content ("json")})})})})})})})})})))))))

(deftest resource-only-rule
  (testing "simple param"
    (is (= [:heroes {:from :heroes :method :get :select [[:id] [:name]]}]
           (statement/from-query {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")})}
                                                                   {:tag :OnlyRule
                                                                    :content ({:tag :OnlyRuleItem
                                                                               :content ({:tag :OnlyRuleItemPath
                                                                                          :content ("id")}
                                                                                         {:tag :ModifierList
                                                                                          :content nil})}
                                                                              {:tag :OnlyRuleItem
                                                                               :content ({:tag :OnlyRuleItemPath
                                                                                          :content ("name")}
                                                                                         {:tag :ModifierList
                                                                                          :content nil})})})})})}))))
  (testing "resource filtering response with deep path"
    (is (= (binding [*print-meta* true]
             (pr-str [:heroes {:from :heroes :method :get :select [[:id] ^{:maches "abc" :encoder :json} [:skills :id] [:skills :name]]}]))
           (binding [*print-meta* true]
             (pr-str (statement/from-query {:tag :Query
                                            :content '({:tag :QueryBlock
                                                        :content ({:tag :QueryItem
                                                                   :content ({:tag :ActionRule
                                                                              :content ({:tag :ActionRuleKey
                                                                                         :content ("from")}
                                                                                        {:tag :ActionRuleValue
                                                                                         :content ("heroes")})}
                                                                             {:tag :OnlyRule
                                                                              :content ({:tag :OnlyRuleItem
                                                                                         :content ({:tag :OnlyRuleItemPath
                                                                                                    :content ("id")}
                                                                                                   {:tag :ModifierList
                                                                                                    :content nil})}
                                                                                        {:tag :OnlyRuleItem
                                                                                         :content ({:tag :OnlyRuleItemPath
                                                                                                    :content ("skills")}
                                                                                                   {:tag :OnlyRuleItemPath
                                                                                                    :content ("id")}
                                                                                                   {:tag :ModifierList
                                                                                                    :content ({:tag :Modifier
                                                                                                               :content ({:tag :ModifierFunction
                                                                                                                          :content ({:tag :ModifierFunctionName
                                                                                                                                     :content ("maches")}
                                                                                                                                    {:tag :ModifierFunctionArgList
                                                                                                                                     :content ({:tag :String
                                                                                                                                                  :content ("\"abc\"")})})})}
                                                                                                              {:tag :Modifier
                                                                                                               :content ({:tag :ModifierAlias
                                                                                                                          :content ("json")})})})}
                                                                                        {:tag :OnlyRuleItem
                                                                                         :content ({:tag :OnlyRuleItemPath
                                                                                                    :content ("skills")}
                                                                                                   {:tag :OnlyRuleItemPath
                                                                                                    :content ("name")}
                                                                                                   {:tag :ModifierList
                                                                                                    :content nil})})})})})}))))))
  (testing "resource filtering response with arg variable"
    (is (= (binding [*print-meta* true]
             (pr-str [:heroes {:from :heroes, :method :get, :select [[:id] ^{:matches "Hero"} [:name]]}]))
           (binding [*print-meta* true]
             (pr-str (statement/from-query {"name" "Hero"}
                                           {:tag :Query
                                            :content '({:tag :QueryBlock
                                                        :content ({:tag :QueryItem
                                                                   :content ({:tag :ActionRule
                                                                              :content ({:tag :ActionRuleKey
                                                                                         :content ("from")}
                                                                                        {:tag :ActionRuleValue
                                                                                         :content ("heroes")})}
                                                                             {:tag :OnlyRule
                                                                              :content ({:tag :OnlyRuleItem
                                                                                         :content ({:tag :OnlyRuleItemPath
                                                                                                    :content ("id")}
                                                                                                   {:tag :ModifierList
                                                                                                    :content nil})}
                                                                                        {:tag :OnlyRuleItem
                                                                                         :content ({:tag :OnlyRuleItemPath
                                                                                                    :content ("name")}
                                                                                                   {:tag :ModifierList
                                                                                                    :content ({:tag :Modifier
                                                                                                               :content ({:tag :ModifierFunction
                                                                                                                          :content ({:tag :ModifierFunctionName
                                                                                                                                     :content ("matches")}
                                                                                                                                    {:tag :ModifierFunctionArgList
                                                                                                                                     :content ({:tag :Variable
                                                                                                                                                :content ("name")})})})})})})})})})}))))))
  (testing "resource hidden"
    (is (= [:heroes {:from :heroes :method :get :select :none}]
           (statement/from-query {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")})}
                                                                   {:tag :HideRule
                                                                    :content nil})})})})))))

(deftest resource-modifier-rule
  (testing "timeout modifier"
    (is (= [:heroes {:from :heroes :method :get :timeout 200}]
           (statement/from-query {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")})}
                                                                   {:tag :ModifierBlock
                                                                    :content ({:tag :TimeoutRule
                                                                               :content ({:tag :Integer
                                                                                          :content ("200")})})})})})}))))
(testing "timeout modifier and header"
    (is (= [:heroes {:from :heroes :method :get :with-headers {"Authorization" "XPTO"} :timeout 200}]
           (statement/from-query {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")})}
                                                                   {:tag :ModifierBlock
                                                                    :content ({:tag :HeaderRule
                                                                               :content ({:tag :HeaderRuleItem
                                                                                          :content ({:tag :HeaderName
                                                                                                     :content ("Authorization")}
                                                                                                    {:tag :HeaderValue
                                                                                                     :content ({:tag :String
                                                                                                                :content ("\"XPTO\"")})})})}
                                                                              {:tag :TimeoutRule
                                                                               :content ({:tag :Integer
                                                                                          :content ("200")})})})})})})))))

(deftest resource-flags-rule
  (testing "ignoring errors"
    (is (= (binding [*print-meta* true]
             (pr-str [:heroes ^{:ignore-errors "ignore"} {:from :heroes :method :get}]))
           (binding [*print-meta* true]
             (pr-str (statement/from-query {:tag :Query
                                  :content '({:tag :QueryBlock
                                              :content ({:tag :QueryItem
                                                         :content ({:tag :ActionRule
                                                                    :content ({:tag :ActionRuleKey
                                                                               :content ("from")}
                                                                              {:tag :ActionRuleValue
                                                                               :content ("heroes")})}
                                                                   {:tag :FlagsRule
                                                                    :content ({:tag :FlagRule
                                                                               :content ({:tag :IgnoreErrorsFlag
                                                                                          :content nil})})})})})})))))))
