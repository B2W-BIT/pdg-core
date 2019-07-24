(ns restql.core.runner.core-test
  (:require [clojure.core.async :refer [<!! >! chan timeout go close!]]
            [clojure.core.async.impl.protocols :refer [closed?] :rename {closed? chan-closed?}]
            [clojure.test :refer [deftest is testing]]
            [restql.core.runner.core :as core]))

(def is-done? #'core/is-done?)
(def can-request? #'core/can-request?)
(def all-that-can-request #'core/all-that-can-request)

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

(deftest do-run-test
  (testing "Set result in output-ch and close all channels"
    (let [chans {:request-ch (chan)
                 :result-ch (go "some-result")
                 :output-ch (chan)
                 :exception-ch (chan)
                 :timeout-ch (chan)}]
      (with-redefs [core/all-that-can-request (fn [_] {:hero {:from :hero, :method :get}})
                    core/update-state (fn [_ _] {:done "Done"})
                    core/all-done? (fn [_] true)]
        (#'restql.core.runner.core/do-run nil chans)
        (is (= "Done" (<!! (:output-ch chans))))
        (is (every? chan-closed? (map #(% chans) [:request-ch :result-ch :output-ch]))))))

  (testing "If exception-ch closes stop recur and close all remaining channels"
    (let [chans {:request-ch (chan)
                 :result-ch (go "some-result")
                 :output-ch (chan)
                 :exception-ch (chan)
                 :timeout-ch (chan)}]
      (with-redefs [core/all-that-can-request (fn [_] {:hero {:from :hero, :method :get}})
                    core/update-state (fn [_ _] nil)
                    core/all-done? (fn [_] false)]
        (close! (:exception-ch chans))
        (<!! (#'restql.core.runner.core/do-run nil chans))
        (is (every? chan-closed? (map #(% chans) [:request-ch :result-ch :output-ch :exception-ch]))))))

  (testing "If timeout-ch closes stop recur and close all remaining channels."
    (let [chans {:request-ch (chan)
                 :result-ch (go "some-result")
                 :output-ch (chan)
                 :exception-ch (chan)
                 :timeout-ch (timeout 1)}]
      (with-redefs [core/all-that-can-request (fn [_] {:hero {:from :hero, :method :get}})
                    core/update-state (fn [_ _] nil)
                    core/all-done? (fn [_] false)]
        (<!! (#'restql.core.runner.core/do-run nil chans))
        (is (every? chan-closed? (map #(% chans) [:request-ch :result-ch :output-ch])))))))
