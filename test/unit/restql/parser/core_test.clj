(ns restql.parser.core-test
  (:require [clojure.test :refer :all]
            [restql.parser.core :refer :all]))

(deftest testing-edn-string-production
  (testing "Testing simple query"
    (is (= (parse-query "from heroes as hero")
           [:hero {:from :heroes :method :get}])))

  (testing "Testing simple query without alias"
    (is (= (parse-query "from heroes")
           [:heroes {:from :heroes :method :get}])))

  (testing "Testing post method"
    (is (= (parse-query "to heroes")
           [:heroes {:from :heroes :method :post}])))

  (testing "Testing put method"
    (is (= (parse-query "into heroes")
           [:heroes {:from :heroes :method :put}])))

  (testing "Testing delete method"
    (is (= (parse-query "delete heroes")
           [:heroes {:from :heroes :method :delete}])))

  (testing "Testing patch method"
    (is (= (parse-query "update heroes")
           [:heroes {:from :heroes :method :patch}])))

  (testing "Testing simple query params a use clause"
    (is (= (parse-query "use cache-control = 900
                                      from heroes as hero")
           ^{:cache-control 900} [:hero {:from :heroes :method :get}])))

  (testing "Testing simple query params ignore errors"
    (is (= (parse-query "from heroes as hero ignore-errors")
           [:hero ^{:ignore-errors "ignore"} {:from :heroes :method :get}])))

  (testing "Testing multiple query"
    (is (= (parse-query "from heroes as hero
                                      from monsters as monster")
           [:hero {:from :heroes :method :get}
            :monster {:from :monsters :method :get}])))

  (testing "Testing query params one numeric parameter"
    (is (= (parse-query "from heroes as hero params id = 123")
           [:hero {:from :heroes :with {:id 123} :method :get}])))

  (testing "Testing query params one string parameter"
    (is (= (parse-query "from heroes as hero params id = \"123\"")
           [:hero {:from :heroes :with {:id "123"} :method :get}])))

  (testing "Testing query params variable parameter"
    (is (= (parse-query "from heroes as hero params id = $id" :context {"id" "123"})
           [:hero {:from :heroes :with {:id "123"} :method :get}])))

  (testing "Testing query params one null parameter"
    (is (= (parse-query "from heroes as hero params id = 123, spell = null")
           [:hero {:from :heroes :with {:id 123 :spell nil} :method :get}])))

  (testing "Testing query params one boolean parameter"
    (is (= (parse-query "from heroes as hero params magician = true")
           [:hero {:from :heroes :with {:magician true} :method :get}])))

  (testing "Testing query params one array parameter"
    (is (= (parse-query "from heroes as hero params class = [\"warrior\", \"magician\"]")
           [:hero {:from :heroes :with {:class ["warrior" "magician"]} :method :get}])))

  (testing "Testing query params one complex parameter"
    (is (= (parse-query "from heroes as hero params equip = {sword: 1, shield: 2}")
           [:hero {:from :heroes :with {:equip {:sword 1 :shield 2}} :method :get}])))

  (testing "Testing query params one complex parameter params subitems"
    (is (= (parse-query "from heroes as hero params equip = {sword: {foo: \"bar\"}, shield: [1, 2, 3]}")
           [:hero {:from :heroes :with {:equip {:sword {:foo "bar"} :shield [1 2 3]}} :method :get}])))

  (testing "Testing query params one chained parameter"
    (is (= (parse-query "from heroes as hero params id = player.id")
           [:hero {:from :heroes :method :get :with {:id [:player :id]}}])))

  (testing "Testing query params one chained parameter and metadata"
    (is (= (parse-query "from heroes as hero params id = player.id -> json")
           [:hero {:from :heroes :method :get :with {:id ^{:encoder :json} [:player :id]}}])))

  (testing "Testing query params one chained parameter and metadata"
    (is (= (binding [*print-meta* true]
             (pr-str (parse-query "from heroes as hero params id = player.id -> base64")))
           (binding [*print-meta* true]
             (pr-str [:hero {:from :heroes :method :get :with {:id ^{:encoder :base64} [:player :id]}}])))))

  (testing "Testing query params headers"
    (is (= (parse-query "from heroes as hero headers Content-Type = \"application/json\" params id = 123")
           [:hero {:from :heroes :with-headers {"Content-Type" "application/json"} :with {:id 123} :method :get}])))

  (testing "Testing query params headers and parameters"
    (is (= (parse-query "from heroes as hero headers Authorization = $auth params id = 123" :context {"auth" "abc123"})
           [:hero {:from :heroes :with-headers {"Authorization" "abc123"} :with {:id 123} :method :get}])))

  (testing "Testing query params hidden selection"
    (is (= (parse-query "from heroes as hero params id = 1 hidden")
           [:hero {:from :heroes :with {:id 1} :select :none :method :get}])))

  (testing "Testing query params only selection"
    (is (= (parse-query "from heroes as hero params id = 1 only id, name")
           [:hero {:from :heroes :with {:id 1} :select [[:id] [:name]] :method :get}])))

  (testing "Testing query params only selection of inner elements"
    (is (= (parse-query "from heroes as hero params id = 1 only skills.id, skills.name, name")
           [:hero {:from :heroes :method :get :with {:id 1} :select [[:skills :id] [:skills :name] [:name]]}])))

  (testing "Testing query params paramater params dot and chaining"
    (is (= (parse-query "from heroes as hero params weapon.id = weapon.id")
           [:hero {:from :heroes :with {:weapon.id [:weapon :id]} :method :get}])))

  (testing "Testing simple with chaining variable"
    (is (= (parse-query "from heroes with id = bla[$variable].ble, name = $name" :context {"variable" 1 "name" 2})
           [:heroes {:from :heroes, :with {:id [:bla :1 :ble], :name 2}, :method :get}]))
    (is (= (parse-query "from heroes with id = bla.$variable.ble, name = $name" :context {"variable" 1 "name" 2})
           [:heroes {:from :heroes, :with {:id [:bla :1 :ble], :name 2}, :method :get}])))

  (testing "Testing query params only selection and a filter"
    (binding [*print-meta* true]
      (is (= (parse-query "from heroes as hero params id = 1 only id, name -> matches(\"foo\")")
             [:hero {:from :heroes :method :get :with {:id 1} :select [[:id] ^{:matches "foo"} [:name]]}]))))

  (testing "Testing query params only selection and a filter params wildcard"
    (binding [*print-meta* true]
      (is (= (pr-str  (parse-query "from heroes as hero params id = 1 only id -> equals(1), *"))
             (pr-str  [:hero {:from :heroes :method :get :with {:id 1} :select [^{:equals 1} [:id] [:*]]}])))))

  (testing "Testing filter with variable"
    (binding [*print-meta* true]
      (is (= (pr-str (parse-query "from heroes as hero params id = 1 only id, name -> matches($name)" :context {"name" "Hero"}))
             (pr-str [:hero {:from :heroes :method :get :with {:id 1} :select [[:id] ^{:matches "Hero"} [:name]]}])))))

  (testing "Testing full featured query"
    (binding [*print-meta* true]
      (is (= (pr-str (parse-query "from product as products
                                                 headers
                                                     content-type = \"application/json\"
                                                 with
                                                     limit = product.id -> flatten -> json
                                                     fields = [\"rating\", \"tags\", \"images\", \"groups\"]
                                                 only
                                                     id, name, cep, phone"))
             (pr-str [:products {:from         :product
                                 :method :get
                                 :with         {:limit  ^{:expand false :encoder :json}
                                                [:product :id]
                                                :fields ["rating" "tags" "images" "groups"]}
                                 :select       [[:id] [:name] [:cep] [:phone]]
                                 :with-headers {"content-type" "application/json"}}]))))))

(deftest testing-cache
  (testing "Will not cache when ad-hoc query"
    (let [query-parser-cache-counter (atom 0)
          query-parser-counter (atom 0)]
      (with-redefs [restql.parser.core/parse-with-cache (fn [_] (do (swap! query-parser-cache-counter inc) []))
                    restql.parser.query/from-text       (fn [_] (do (swap! query-parser-counter inc) []))]
        (parse-query "from heroes as hero" :query-type :ad-hoc)
        (parse-query "from heroes as hero" :query-type :ad-hoc)
        (is (= 0 @query-parser-cache-counter))
        (is (= 2 @query-parser-counter)))))

  (testing "Will cache when is NOT ad-hoc query"
    (let [query-parser-cache-counter (atom 0)
          query-parser-counter (atom 0)]
      (with-redefs [restql.parser.core/parse-with-cache (fn [_] (do (swap! query-parser-cache-counter inc) []))
                    restql.parser.query/from-text       (fn [_] (do (swap! query-parser-counter inc) []))]
        (parse-query "from heroes as hero")
        (parse-query "from heroes as hero")
        (is (= 2 @query-parser-cache-counter))
        (is (= 0 @query-parser-counter))))))
