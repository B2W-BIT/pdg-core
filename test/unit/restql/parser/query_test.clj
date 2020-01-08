(ns restql.parser.query-test
  (:require [clojure.test :refer :all]
            [restql.parser.query :as query]))

(deftest action-rule
  (testing "from resource"
    (is (= (query/from-text "from heroes")
           {:tag :Query
            :content '({:tag :QueryBlock
                        :content ({:tag :QueryItem
                                   :content ({:tag :ActionRule
                                              :content ({:tag :ActionRuleKey
                                                         :content ("from")}
                                                        {:tag :ActionRuleValue
                                                         :content ("heroes")})})})})})))
  (testing "to resource"
    (is (= (query/from-text "to heroes")
           {:tag :Query
            :content '({:tag :QueryBlock
                        :content ({:tag :QueryItem
                                   :content ({:tag :ActionRule
                                              :content ({:tag :ActionRuleKey
                                                         :content ("to")}
                                                        {:tag :ActionRuleValue
                                                         :content ("heroes")})})})})})))
  (testing "into resource"
    (is (= (query/from-text "into heroes")
           {:tag :Query
            :content '({:tag :QueryBlock
                        :content ({:tag :QueryItem
                                   :content ({:tag :ActionRule
                                              :content ({:tag :ActionRuleKey
                                                         :content ("into")}
                                                        {:tag :ActionRuleValue
                                                         :content ("heroes")})})})})})))
  (testing "delete resource"
    (is (= (query/from-text "delete heroes")
           {:tag :Query
            :content '({:tag :QueryBlock
                        :content ({:tag :QueryItem
                                   :content ({:tag :ActionRule
                                              :content ({:tag :ActionRuleKey
                                                         :content ("delete")}
                                                        {:tag :ActionRuleValue
                                                         :content ("heroes")})})})})})))
  (testing "update resource"
    (is (= (query/from-text "update heroes")
           {:tag :Query
            :content '({:tag :QueryBlock
                        :content ({:tag :QueryItem
                                   :content ({:tag :ActionRule
                                              :content ({:tag :ActionRuleKey
                                                         :content ("update")}
                                                        {:tag :ActionRuleValue
                                                         :content ("heroes")})})})})}))))

(deftest alias-rule
  (testing "resource alias"
    (is (= (query/from-text "from heroes as heroes-alias")
           {:tag :Query
            :content '({:tag :QueryBlock
                       :content ({:tag :QueryItem
                                  :content ({:tag :ActionRule
                                             :content ({:tag :ActionRuleKey
                                                        :content ("from")}
                                                       {:tag :ActionRuleValue
                                                        :content ("heroes")}
                                                       {:tag :ActionRuleAlias
                                                        :content ("heroes-alias")})})})})}))))

(deftest in-rule
  (testing "resource in"
    (is (= (query/from-text "from heroes in heroes-in")
           {:tag :Query
            :content '({:tag :QueryBlock
                        :content ({:tag :QueryItem
                                   :content ({:tag :ActionRule
                                              :content ({:tag :ActionRuleKey
                                                         :content ("from")}
                                                        {:tag :ActionRuleValue
                                                         :content ("heroes")}
                                                        {:tag :ActionRuleIn
                                                         :content ("heroes-in")})})})})}))))

(deftest use-rule
  (testing "use timeout with equals sign"
    (is (= (query/from-text "use timeout = 100 \n from heroes")
           {:tag :Query
            :content '({:tag :UseBlock
                       :content ({:tag :UseRule
                                  :content ({:tag :ModifierBlock,
                                             :content ({:tag :TimeoutRule
                                                        :content ({:tag :Integer
                                                                   :content ("100")})})})})}
                      {:tag :QueryBlock
                       :content ({:tag :QueryItem
                                  :content ({:tag :ActionRule
                                             :content ({:tag :ActionRuleKey
                                                        :content ("from")}
                                                       {:tag :ActionRuleValue
                                                        :content ("heroes")})})})})})))
  (testing "use timeout without equals sign"
    (is (= (query/from-text "use timeout 100 \n from heroes")
           {:tag :Query
            :content '({:tag :UseBlock
                       :content ({:tag :UseRule
                                  :content ({:tag :ModifierBlock,
                                             :content ({:tag :TimeoutRule
                                                        :content ({:tag :Integer
                                                                   :content ("100")})})})})}
                      {:tag :QueryBlock
                       :content ({:tag :QueryItem
                                  :content ({:tag :ActionRule
                                             :content ({:tag :ActionRuleKey
                                                        :content ("from")}
                                                       {:tag :ActionRuleValue
                                                        :content ("heroes")})})})})})))
(testing "use timeout use max-age"
  (is (= (query/from-text "use timeout 100 \n use max-age 200 \n from heroes")
         {:tag :Query
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
                                                      :content ("heroes")})})})})}))))

(deftest modifier-rule
  (testing "use timeout"
    (is (= (query/from-text "use timeout = 100 \n from heroes")
           {:tag :Query
            :content '({:tag :UseBlock
                        :content ({:tag :UseRule
                                   :content ({:tag :ModifierBlock,
                                              :content ({:tag :TimeoutRule
                                                         :content ({:tag :Integer
                                                                    :content ("100")})})})})}
                       {:tag :QueryBlock
                        :content ({:tag :QueryItem
                                   :content ({:tag :ActionRule
                                              :content ({:tag :ActionRuleKey
                                                         :content ("from")}
                                                        {:tag :ActionRuleValue
                                                         :content ("heroes")})})})})})))
  (testing "use max-age timeout"
    (is (= (query/from-text "use max-age = 20 \n timeout = 100 \n from heroes")
           {:tag :Query
            :content '({:tag :UseBlock
                        :content ({:tag :UseRule
                                   :content ({:tag :ModifierBlock,
                                              :content ({:tag :MaxAgeRule
                                                         :content ({:tag :Integer
                                                                    :content ("20")})}
                                                        {:tag :TimeoutRule
                                                         :content ({:tag :Integer
                                                                    :content ("100")})})})})}
                       {:tag :QueryBlock
                        :content ({:tag :QueryItem
                                   :content ({:tag :ActionRule
                                              :content ({:tag :ActionRuleKey
                                                         :content ("from")}
                                                        {:tag :ActionRuleValue
                                                         :content ("heroes")})})})})})))
  (testing "use s-max-age timeout max-age"
    (is (= (query/from-text "use max-age = 20 \n timeout = 100 \n s-max-age = 20 \n from heroes")
           {:tag :Query
            :content '({:tag :UseBlock
                        :content ({:tag :UseRule
                                   :content ({:tag :ModifierBlock,
                                              :content ({:tag :MaxAgeRule
                                                         :content ({:tag :Integer
                                                                    :content ("20")})}
                                                        {:tag :TimeoutRule
                                                         :content ({:tag :Integer
                                                                    :content ("100")})}
                                                        {:tag :SMaxAgeRule
                                                         :content ({:tag :Integer
                                                                    :content ("20")})})})})}
                       {:tag :QueryBlock
                        :content ({:tag :QueryItem
                                   :content ({:tag :ActionRule
                                              :content ({:tag :ActionRuleKey
                                                         :content ("from")}
                                                        {:tag :ActionRuleValue
                                                         :content ("heroes")})})})})})))
  (testing "resource modifier timeout"
    (is (= (query/from-text "from heroes timeout = 200")
           {:tag :Query
            :content '({:tag :QueryBlock
                        :content ({:tag :QueryItem
                                   :content ({:tag :ActionRule
                                              :content ({:tag :ActionRuleKey
                                                         :content ("from")}
                                                        {:tag :ActionRuleValue
                                                         :content ("heroes")})}
                                             {:tag :ModifierBlock,
                                              :content ({:tag :TimeoutRule
                                                         :content ({:tag :Integer
                                                                    :content ("200")})})})})})})))
  (testing "resource modifier timeout and header"
    (is (= (query/from-text "from heroes headers Authorization = \"XPTO\" \n timeout = 200")
           {:tag :Query
            :content '({:tag :QueryBlock
                        :content ({:tag :QueryItem
                                   :content ({:tag :ActionRule
                                              :content ({:tag :ActionRuleKey
                                                         :content ("from")}
                                                        {:tag :ActionRuleValue
                                                         :content ("heroes")})}
                                             {:tag :ModifierBlock,
                                              :content ({:tag :HeaderRule
                                                         :content ({:tag :HeaderRuleItem
                                                                    :content ({:tag :HeaderName
                                                                               :content ("Authorization")}
                                                                              {:tag :HeaderValue
                                                                               :content ({:tag :String
                                                                                          :content ("\"XPTO\"")})})})}
                                                        {:tag :TimeoutRule
                                                         :content ({:tag :Integer
                                                                    :content ("200")})})})})})})))
  (testing "resource modifier timeout and header"
    (is (= (query/from-text "from heroes headers Authorization = \"XPTO\" \n timeout = $timeout")
           {:tag :Query
            :content '({:tag :QueryBlock
                        :content ({:tag :QueryItem
                                   :content ({:tag :ActionRule
                                              :content ({:tag :ActionRuleKey
                                                         :content ("from")}
                                                        {:tag :ActionRuleValue
                                                         :content ("heroes")})}
                                             {:tag :ModifierBlock,
                                              :content ({:tag :HeaderRule
                                                         :content ({:tag :HeaderRuleItem
                                                                    :content ({:tag :HeaderName
                                                                               :content ("Authorization")}
                                                                              {:tag :HeaderValue
                                                                               :content ({:tag :String
                                                                                          :content ("\"XPTO\"")})})})}
                                                        {:tag :TimeoutRule
                                                         :content ({:tag :Variable
                                                                    :content ("timeout")})})})})})}))))

(deftest with-rule
  (testing "resource with param"
    (is (= (query/from-text "from heroes with id = 1")
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
                                                                    :content ({:tag :Integer
                                                                               :content ("1")}
                                                                              {:tag :ModifierList
                                                                               :content nil})})})})})})})))
  (testing "resource with brackets param "
    (is (= (query/from-text "from heroes with id[] = [1, 2]")
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
                                                                    :content ("id[]")}
                                                                   {:tag :WithParamValue,
                                                                    :content
                                                                    ({:tag :ListParam,
                                                                      :content
                                                                      ({:tag :WithParamValue,
                                                                        :content
                                                                        ({:tag :Integer, :content ("1")}
                                                                         {:tag :ModifierList, :content nil})}
                                                                       {:tag :WithParamValue,
                                                                        :content
                                                                        ({:tag :Integer, :content ("2")}
                                                                         {:tag :ModifierList, :content nil})})}
                                                                     {:tag :ModifierList, :content nil})})})})})})})))

  (testing "resource a list param with flatten modifier"
    (is (= (query/from-text "from heroes with id = [weapons.id -> flatten, weapons.name -> flatten]")
           {:tag :Query,
           :content
           '({:tag :QueryBlock,
             :content
             ({:tag :QueryItem,
               :content
               ({:tag :ActionRule,
                 :content
                 ({:tag :ActionRuleKey, :content ("from")}
                  {:tag :ActionRuleValue, :content ("heroes")})}
                {:tag :WithRule,
                 :content
                 ({:tag :WithRuleItem,
                   :content
                   ({:tag :WithParamName, :content ("id")}
                    {:tag :WithParamValue,
                     :content
                     ({:tag :ListParam,
                       :content
                       ({:tag :WithParamValue,
                         :content
                         ({:tag :Chaining,
                           :content
                           ({:tag :PathItem, :content ("weapons")}
                            {:tag :PathItem, :content ("id")})}
                          {:tag :ModifierList,
                           :content
                           ({:tag :Modifier,
                             :content
                             ({:tag :ModifierAlias,
                               :content ("flatten")})})})}
                        {:tag :WithParamValue,
                         :content
                         ({:tag :Chaining,
                           :content
                           ({:tag :PathItem, :content ("weapons")}
                            {:tag :PathItem, :content ("name")})}
                          {:tag :ModifierList,
                           :content
                           ({:tag :Modifier,
                             :content
                             ({:tag :ModifierAlias,
                               :content ("flatten")})})})})}
                      {:tag :ModifierList, :content nil})})})})})})})))

  (testing "resource with param null"
    (is (= (query/from-text "from heroes with id = 1, skill = null")
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
                                                                               :content nil})})})})})})})))
  (testing "resource with param boolean"
    (is (= (query/from-text "from heroes with id = 1, skill = false")
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
                                                                               :content nil})})})})})})}))
    (is (= (query/from-text "from heroes with id = 1, rate = 1.5, skill = true")
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
                                                                    :content ({:tag :Integer
                                                                               :content ("1")}
                                                                              {:tag :ModifierList
                                                                               :content nil})})}
                                                        {:tag :WithRuleItem
                                                         :content ({:tag :WithParamName
                                                                    :content ("rate")}
                                                                   {:tag :WithParamValue
                                                                    :content ({:tag :Float
                                                                               :content ("1.5")}
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
  (testing "resource with vector"
    (is (= (query/from-text "from heroes with id = [\"warrior\", \"magician\"]")
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
                                                                               :content nil})})})})})})})))
  (testing "resource with compex param"
    (is (= (query/from-text "from heroes with equip = {sword: {foo: \"bar\"}, shield: [1, 2, 3]}")
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
                                                                               :content nil})})})})})})})))
  (testing "resource with compex param with inner modifier"
    (is (= (query/from-text "from sidekick\n
                                       with\n
                                         source = weapons.source\n
                                         weapon = {\n
                                           name: weapons.swords.name -> flatten,\n
                                           typeId: weapons.types.typeId,\n
                                           invisible: false\n
                                         } -> flatten")
           {:tag :Query
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
                                                                                                     :content ("flatten")})})})})})})})})})))
  (testing "resource with param variable"
    (is (= (query/from-text "from heroes with id = $id")
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
                                                                               :content nil})})})})})})})))
  (testing "resource with param complex variable"
    (is (= (query/from-text "from heroes with $hero")
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
                                                                    :content ("hero")})})})})})})))
  (testing "resource with param variable and modifier"
    (is (= (query/from-text "from heroes with id = $id -> flatten")
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
                                                                               :content ({:tag :Modifier
                                                                                          :content ({:tag :ModifierAlias
                                                                                                     :content ("flatten")})})})})})})})})})))
  (testing "resource with param variable and modifier with arg"
    (is (= (query/from-text "from heroes with id = $id -> flatten(\"abc\")")
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
                                                                               :content ({:tag :Modifier
                                                                                          :content ({:tag :ModifierFunction
                                                                                                     :content ({:tag :ModifierFunctionName
                                                                                                                :content ("flatten")}
                                                                                                               {:tag :ModifierFunctionArgList
                                                                                                                :content ({:tag :String
                                                                                                                           :content ("\"abc\"")})})})})})})})})})})})))
  (testing "resource with param chaining"
    (is (= (query/from-text "from heroes with id = villain.id")
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
                                                                                         {:tag :PathItem
                                                                                          :content ("id")})}
                                                                              {:tag :ModifierList
                                                                               :content nil})})})})})})})))
  (testing "resource with param chaining with variable"
    (is (= (query/from-text "from heroes with id = villain.$id")
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
                                                                                          :content ("id")})}
                                                                              {:tag :ModifierList
                                                                               :content nil})})})})})})})))
  (testing "resource with param chaining with variable"
    (is (= (query/from-text "from heroes with id = villain[$id]")
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
                                                                                          :content ("id")})}
                                                                              {:tag :ModifierList
                                                                               :content nil})})})})})})})))
  (testing "resource with param chaining with variable"
    (is (= (query/from-text "from heroes with id = bla[$variable].ble, name = $name")
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
                                                                                          :content ("ble")})}
                                                                              {:tag :ModifierList
                                                                               :content nil})})}
                                                        {:tag :WithRuleItem
                                                         :content ({:tag :WithParamName
                                                                    :content ("name")}
                                                                   {:tag :WithParamValue
                                                                    :content ({:tag :Variable
                                                                               :content ("name")}
                                                                              {:tag :ModifierList
                                                                               :content nil})})})})})})})))
  (testing "resource with param chaining with variable"
    (is (= (query/from-text "from heroes with id = bla.$variable.ble, name = $name")
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
                                                                                          :content ("ble")})}
                                                                              {:tag :ModifierList
                                                                               :content nil})})}
                                                        {:tag :WithRuleItem
                                                         :content ({:tag :WithParamName
                                                                    :content ("name")}
                                                                   {:tag :WithParamValue
                                                                    :content ({:tag :Variable
                                                                               :content ("name")}
                                                                              {:tag :ModifierList
                                                                               :content nil})})})})})})})))
  (testing "resource with param chaining with variable and modifier"
    (is (= (query/from-text "from heroes with id = villain.$id -> flatten -> json")
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
                                                                                          :content ("id")})}
                                                                              {:tag :ModifierList
                                                                               :content ({:tag :Modifier
                                                                                          :content ({:tag :ModifierAlias
                                                                                                     :content ("flatten")})}
                                                                                         {:tag :Modifier
                                                                                          :content ({:tag :ModifierAlias
                                                                                                     :content ("json")})})})})})})})})}))))

(deftest only-rule
  (testing "resource filtering response"
    (is (= (query/from-text "from heroes only id")
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
                                                                    :content nil})})})})})})))
  (testing "resource filtering response with modifier"
    (is (= (query/from-text "from heroes only id -> matches(\"abc\")")
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
                                                                    :content ({:tag :Modifier
                                                                               :content ({:tag :ModifierFunction
                                                                                          :content ({:tag :ModifierFunctionName
                                                                                                     :content ("matches")}
                                                                                                    {:tag :ModifierFunctionArgList
                                                                                                     :content ({:tag :String
                                                                                                                :content ("\"abc\"")})})})})})})})})})})))
  (testing "resource filtering response with modifier"
    (is (= (query/from-text "from heroes only id -> equals(1)")
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
                                                                    :content ({:tag :Modifier
                                                                               :content ({:tag :ModifierFunction
                                                                                          :content ({:tag :ModifierFunctionName
                                                                                                     :content ("equals")}
                                                                                                    {:tag :ModifierFunctionArgList
                                                                                                     :content ({:tag :Integer
                                                                                                                :content ("1")})})})})})})})})})})))
  (testing "resource filtering response with modifier arg variable"
    (is (= (query/from-text "from heroes only id, name -> matches($name)")
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
                                                                                                                :content ("name")})})})})})})})})})})))
  (testing "resource filtering response with deep path"
    (is (= (query/from-text "from heroes only id, skills.id, skills.name")
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
                                                                    :content ("skills")}
                                                                   {:tag :OnlyRuleItemPath
                                                                    :content ("id")}
                                                                   {:tag :ModifierList
                                                                    :content nil})}
                                                        {:tag :OnlyRuleItem
                                                         :content ({:tag :OnlyRuleItemPath
                                                                    :content ("skills")}
                                                                   {:tag :OnlyRuleItemPath
                                                                    :content ("name")}
                                                                   {:tag :ModifierList
                                                                    :content nil})})})})})})))
  (testing "resource hidden"
    (is (= (query/from-text "from heroes hidden")
           {:tag :Query
            :content '({:tag :QueryBlock
                        :content ({:tag :QueryItem
                                   :content ({:tag :ActionRule
                                              :content ({:tag :ActionRuleKey
                                                         :content ("from")}
                                                        {:tag :ActionRuleValue
                                                         :content ("heroes")})}
                                             {:tag :HideRule
                                              :content nil})})})}))))

(deftest ignore-errors
  (testing "resource ignoring error"
    (is (= (query/from-text "from heroes ignore-errors")
           {:tag :Query
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
                                                                   :content nil})})})})})}))))
