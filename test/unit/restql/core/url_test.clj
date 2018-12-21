(ns restql.core.url-test
  (:require [clojure.test :refer [deftest is]])
  (:use restql.core.url))

;;let's define some testing urls first.
(def with-id      "http://cart/:id")
(def with-two-ids "http://customer/:customer-id/address/:address-id")
(def with-composed-ids "http://customer/:customer-id/credit-card/:credit-card-id")

;;Now, to begin with, we must be able to extract the parameters name
;;out of a url pattern string.
(deftest extract-parameters-test
  (is (=
    #{:id}
    (extract-parameters with-id)))

  (is (=
    #{:customer-id :address-id}
    (extract-parameters with-two-ids))))

;;then, we need a way to interpolate these parameters with real
;;values, received from a map
(deftest interpolate-test
  (is (=
    "http://cart/123"
    (interpolate with-id {:id "123"})))

  (is (=
    "http://customer/900/address/800"
    (interpolate with-two-ids {:customer-id "900" :address-id "800"}))))

;;finally, the interpolated parameters must be removed from the parameters
;;map, so they will not pop up in the query strings
(deftest dissoc-params-test
  (is (=
    {:name "restql_core"}
    (dissoc-params with-id {:id "123" :name "restql_core"})))

  (is (=
    {:name "restql_core"}
    (dissoc-params with-two-ids {:customer-id "900" :address-id "800" :name "restql_core"}))))
