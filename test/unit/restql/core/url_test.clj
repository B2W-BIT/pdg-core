(ns restql.core.url-test
  (:require [clojure.test :refer :all])
  (:use restql.core.url))

(deftest extract-url-parameters-test
  (is (=
       {:path #{:id}
        :query #{:customer-id}}
       (extract-url-parameters "http://cart/:id?:customer-id")))
  (is (=
       {:path #{:id}
        :query #{}}
       (extract-url-parameters "http://cart/:id")))
  (is (=
       {:path #{:customer-id :address-id}
        :query #{:size :active}}
       (extract-url-parameters "http://customer/:customer-id/address/:address-id?:size&:active")))
  (is (=
       {:path #{}
        :query #{:size :active}}
       (extract-url-parameters "http://customer/1?:size&:active")))
  (is (=
       {:path #{}
        :query #{}}
       (extract-url-parameters "http://customer/1"))))

(deftest filter-explicit-query-params-test
  (is (= {:customer "Joker"}
         (filter-explicit-query-params "http://cart/:id?:customer" {:id 1 :customer "Joker" :active true})))
  (is (= {:customer "Joker"}
         (filter-explicit-query-params "http://cart/1?:customer" {:id 1 :customer "Joker" :active true}))))

;;then, we need a way to interpolate these parameters with real
;;values, received from a map
(deftest interpolate-test
  (is (= "http://cart/123"
         (interpolate "http://cart/:id" {:id "123"})))
  (is (= "http://customer/900/address/800"
         (interpolate "http://customer/:customer-id/address/:address-id" {:customer-id "900" :address-id "800"})))
  (is (= "http://customer/900/address/800"
         (interpolate "http://customer/:customer-id/address/:address-id?size" {:customer-id "900" :address-id "800" :size 2}))))

;;finally, the interpolated parameters must be removed from the parameters
;;map, so they will not pop up in the query strings
(deftest dissoc-params-test
  (is (= {:name "restql_core"}
         (dissoc-params "http://cart/:id" {:id "123"
                                           :name "restql_core"})))
  (is (= {:name "restql_core"}
         (dissoc-params "http://customer/:customer-id/address/:address-id" {:customer-id "900"
                                                                            :address-id "800"
                                                                            :name "restql_core"})))
  (is (= {:name "restql_core"}
         (dissoc-params "http://customer/:customer-id/address/:address-id?:id&:size" {:customer-id "900"
                                                                                      :address-id "800"
                                                                                      :name "restql_core"
                                                                                      :id 1
                                                                                      :size 5}))))
