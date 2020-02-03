(ns restql.core.runner.debugging-test
  (:require [clojure.test :refer :all]
            [restql.core.runner.debugging :as debugging]))

(deftest build-debugging-map
  (testing "Returns a debug map with information from request and response for a get call"
    (is
      (= {:debug {:url             "http://test.internal.com?id=1"
                  :timeout 5000
                  :request-headers {:Accept "application/json"}
                  :response-haders {:x-test "test"}
                  :response-time   1000
                  :params          {:id 1}}}
         (debugging/build-map {:response-time 1000 :headers {:x-test "test"}}
                              {:url "http://test.internal.com"
                               :method :get
                               :query-params {:id 1}
                               :request-timeout 5000
                               :headers {:Accept "application/json"}}
                              {}))))


  (testing "Returns a debug map for a get call with forwarded query params"
    (is
      (= {:debug {:url             "http://test.internal.com?id=1&context=test"
                  :timeout 5000
                  :request-headers {:Accept "application/json"}
                  :response-haders {:x-test "test"}
                  :response-time   1000
                  :params          {:id 1
                                    :context "test"}}}
         (debugging/build-map {:response-time 1000 :headers {:x-test "test"}}
                              {:url "http://test.internal.com"
                               :method :get
                               :query-params {:id 1}
                               :request-timeout 5000
                               :headers {:Accept "application/json"}}
                              {:forward-params {:context "test"}}))))

  (testing "Return a debug map with information from request and response for a post call"
    (is
      (= {:debug {:url             "http://test.internal.com?id=1"
                  :timeout         5000
                  :request-headers {:Accept "application/json"}
                  :response-haders {:x-test "test"}
                  :request-body    {:id 1 :name "some-resource"}
                  :response-time   1000
                  :params          {:id 1}}}
         (debugging/build-map {:response-time 1000
                               :headers {:x-test "test"}}
                              {:url "http://test.internal.com"
                               :method :post
                               :query-params {:id 1}
                               :body {:id 1
                                      :name "some-resource"}
                               :request-timeout 5000
                               :headers {:Accept "application/json"}}
                              {}))))

  (testing "Return a debug map with information from request and response for a put call"
    (is
      (= {:debug {:url             "http://test.internal.com?id=1"
                  :timeout         5000
                  :request-headers {:Accept "application/json"}
                  :response-haders {:x-test "test"}
                  :request-body    {:id 1 :name "some-resource"}
                  :response-time   1000
                  :params          {:id 1}}}
         (debugging/build-map {:response-time 1000
                               :headers {:x-test "test"}}
                              {:url "http://test.internal.com"
                               :method :put
                               :query-params {:id 1}
                               :body {:id 1
                                      :name "some-resource"}
                               :request-timeout 5000
                               :headers {:Accept "application/json"}}
                              {}))))

  (testing "Return a debug map with information from request and response for a patch call"
    (is
      (= {:debug {:url             "http://test.internal.com?id=1"
                  :timeout         5000
                  :request-headers {:Accept "application/json"}
                  :response-haders {:x-test "test"}
                  :request-body    {:id 1 :name "some-resource"}
                  :response-time   1000
                  :params          {:id 1}}}
         (debugging/build-map {:response-time 1000
                               :headers {:x-test "test"}}
                              {:url "http://test.internal.com"
                               :method :patch
                               :query-params {:id 1}
                               :body {:id 1
                                      :name "some-resource"}
                               :request-timeout 5000
                               :headers {:Accept "application/json"}}
                              {})))))
