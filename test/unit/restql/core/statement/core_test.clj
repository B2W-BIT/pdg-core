(ns restql.core.statement.core-test
  (:require [clojure.test :refer :all]
            [restql.core.statement.core :as statement])
)

(deftest from-statements-test
  (testing "Returns a request config from a single statement"
    (is
     (= [{:from   :resource-name
          :url    "http://resource-url"
          :method :get}]
        (statement/from-statements {:resource-name "http://resource-url"}
                                 [{:from :resource-name}]))))

  (testing "Returns a request config from a statement with a list of statement"
    (is
     (= [[{:from   :resource-name
           :url    "http://resource-url"
           :method :get
           :query-params {:id 1}}
          {:from   :resource-name
           :url    "http://resource-url"
           :method :get
           :query-params {:id 2}}]]
        (statement/from-statements {:resource-name "http://resource-url"}
                                 [[{:from :resource-name, :with {:id 1}, :method :get}
                                   {:from :resource-name, :with {:id 2}, :method :get}]]))))

  (testing "With method"
    (is
     (= [{:from    :resource-name
          :url     "http://resource-url"
          :method  :post
          :headers {:content-type "application/json"}}]
        (statement/from-statements {:resource-name "http://resource-url"}
                                 [{:from    :resource-name
                                   :method  :post
                                   :headers {:content-type "application/json"}}]))))

  (testing "Returns a request config from a statement with single query param"
    (is
     (= [{:from         :resource-name
          :url          "http://resource-url"
          :method       :get
          :query-params {:id 1}}
         {:from         :resource-name
          :url          "http://resource-url"
          :method       :get
          :query-params {:id 2}}]
        (statement/from-statements {:resource-name "http://resource-url"}
                                 [{:from :resource-name
                                   :with {:id 1}
                                   :method :get}
                                  {:from :resource-name
                                   :with {:id 2}
                                   :method :get}]))))

  (testing "Returns a request config from a statement with query params"
    (is
     (= [{:from         :resource-name
          :url          "http://resource-url"
          :method       :get
          :query-params {:id 1 :name "clojurist"}}]
        (statement/from-statements {:resource-name "http://resource-url"}
                                 [{:from :resource-name
                                   :with {:id 1 :name "clojurist"}
                                   :method :get}]))))

  (testing "Returns a request config from a statement with a list query params"
    (is
     (= [{:from         :resource-name
          :url          "http://resource-url"
          :method       :get
          :query-params {:id [1, 2]}}]
        (statement/from-statements {:resource-name "http://resource-url"}
                                 [{:from :resource-name
                                   :with {:id [1, 2]}
                                   :method :get}]))))

  (testing "With interpolated url"
    (is
     (= [{:from   :resource-name
          :url    "http://resource-url/1"
          :method :get}]
        (statement/from-statements {:resource-name "http://resource-url/:id"}
                                 [{:from :resource-name
                                   :with {:id 1}}]))))

  (testing "With interpolated url and params"
    (is
     (= [{:from         :resource-name
          :url          "http://resource-url/clojurist"
          :method       :get
          :query-params {:id [1 2]}}]
        (statement/from-statements {:resource-name "http://resource-url/:name"}
                                 [{:from :resource-name
                                   :with {:id [1 2] :name "clojurist"}}]))))


  (testing "Returns a request config from a statement with post"
    (is
     (= [{:from         :resource-name
          :url          "http://resource-url/param-name"
          :method       :post
          :body         {:id 1}
          :timeout      1000
          :headers      {:content-type "application/json"}}]
        (statement/from-statements {:resource-name "http://resource-url/:name"}
                                 [{:from :resource-name
                                   :method :post
                                   :with {:id 1 :name "param-name"}
                                   :timeout 1000
                                   :headers {:content-type "application/json"}}]))))

  (testing "Returns a request config from a statement with post and query params"
    (is
     (= [{:from         :resource-name
          :url          "http://resource-url/param-name"
          :method       :post
          :query-params {:id 1}
          :body         {:active true}
          :timeout      1000
          :headers      {:content-type "application/json"}}]
        (statement/from-statements {:resource-name "http://resource-url/:name?:id"}
                                 [{:from :resource-name
                                   :method :post
                                   :with {:id 1 :name "param-name" :active true}
                                   :timeout 1000
                                   :headers {:content-type "application/json"}}])))
    (is
     (= [{:from         :resource-name
          :url          "http://resource-url/name"
          :method       :post
          :query-params {:id 1}
          :body         {:active true}
          :timeout      1000
          :headers      {:content-type "application/json"}}]
        (statement/from-statements {:resource-name "http://resource-url/name?:id"}
                                 [{:from :resource-name
                                   :method :post
                                   :with {:id 1 :active true}
                                   :timeout 1000
                                   :headers {:content-type "application/json"}}])))))
