(ns restql.core.runner.core-test
  (:require [clojure.core.async :refer [<!! >! chan go]]
            [clojure.test :refer [deftest is]]))

(def is-done? #'restql.core.runner.core/is-done?)
(def can-request? #'restql.core.runner.core/can-request?)
(def all-that-can-request #'restql.core.runner.core/all-that-can-request)

(deftest is-done?-test
  (is (=
    true
    (is-done? [:cart {:with {:id "123"}}]
              {:done [[:cart {:with {:id "123"}}]]
               :requested []
               :to-do []})))

  (is (=
    false 
    (is-done? [:cart {:with {:id "123"}}]
              {:done [[:customer {:with {:id "123"}}]]
               :requested []
               :to-do []}))))

(deftest can-request?-test
  (is (=
    true 
    (can-request? [:cart {:with {:id "123"}}]
                  {:done []})))

  (is (=
    true
    (can-request? [:cart {:with {:id [:checkout :cartId]}}]
                  {:done [[:checkout {:body {:id "321"}}]]})))

  (is (=
    false
    (can-request? [:cart {:with {:id [:checkout :cartId]}}]
                  {:done []}))))

(deftest all-that-can-request-test
  (is (=
    (seq [[:customer {:with {:id [:cart :id]}}] 
          [:article  {:with {:id "123"}}]])
    
    (all-that-can-request {:done [[:cart {:body []}]]
                           :requested []
                           :to-do [[:customer {:with {:id [:cart :id]}}]
                                   [:address  {:with {:customer [:customer :id]}}]
                                   [:article  {:with {:id "123"}}]]}))))
