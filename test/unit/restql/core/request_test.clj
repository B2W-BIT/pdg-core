(ns restql.core.request-test
  (:require [clojure.test :refer :all]
            [restql.core.request :as request])
)

(deftest from-statements-test
  (testing "Returns a request config from a single statement"
    (is
      (= [{:from   :resource-name
           :url    "http://resource-url"
           :method :get}]
         (request/from-statements {:resource-name "http://resource-url"}
                                  [{:from :resource-name}])
      )
    )
  )

  (testing "With method"
    (is
      (= [{:from    :resource-name
           :url     "http://resource-url"
           :method  :post
           :headers {:content-type "application/json"}}]
        (request/from-statements {:resource-name "http://resource-url"}
                                 [{:from    :resource-name
                                   :method  :post
                                   :headers {:content-type "application/json"}}])
      )
    )
  )

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
        (request/from-statements {:resource-name "http://resource-url"}
                                 [{:from :resource-name
                                   :with {:id 1}
                                   :method :get}
                                  {:from :resource-name
                                   :with {:id 2}
                                   :method :get}])
      )
    )
  )

  (testing "Returns a request config from a statement with query params"
    (is
      (= [{:from         :resource-name
           :url          "http://resource-url"
           :method       :get
           :query-params {:id 1 :name "clojurist"}}]
        (request/from-statements {:resource-name "http://resource-url"}
                                 [{:from :resource-name
                                   :with {:id 1 :name "clojurist"}
                                   :method :get}])
      )
    )
  )

  (testing "Returns a request config from a statement with a list query params"
    (is
      (= [{:from         :resource-name
           :url          "http://resource-url"
           :method       :get
           :query-params {:id [1, 2]}}]
          (request/from-statements {:resource-name "http://resource-url"}
                                  [{:from :resource-name
                                    :with {:id [1, 2]}
                                    :method :get}])
      )
    )
  )

  (testing "With interpolated url"
    (is
      (= [{:from   :resource-name
           :url    "http://resource-url/1"
           :method :get}]
        (request/from-statements {:resource-name "http://resource-url/:id"}
                                 [{:from :resource-name
                                   :with {:id 1}}])
      )
    )
  )

  (testing "With interpolated url and params"
    (is
      (= [{:from         :resource-name
           :url          "http://resource-url/clojurist"
           :method       :get
           :query-params {:id [1 2]}}]
        (request/from-statements {:resource-name "http://resource-url/:name"}
                                 [{:from :resource-name
                                   :with {:id [1 2] :name "clojurist"}}])
      )
    )
  )


  (testing "Returns a request config from a statement with query params"
    (is
      (= [{:from         :resource-name
           :url          "http://resource-url/param-name"
           :method       :post
           :query-params {:id 1}
           :timeout      1000
           :headers      {:content-type "application/json"}
           :post-body    "post body"}]
         (request/from-statements {:resource-name "http://resource-url/:name"}
                                  [{:from :resource-name
                                    :method :post
                                    :with {:id 1 :name "param-name"}
                                    :timeout 1000
                                    :post-body "post body"
                                    :headers {:content-type "application/json"}}])
      )
    )
  )
)
