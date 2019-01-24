(ns restql.request-post-test
  (:require [clojure.test :refer :all]
            [restql.parser.core :as parser]
            [restql.core.api.restql :as restql]
            [byte-streams :as bs]
            [cheshire.core :as json]
            [stub-http.core :refer :all]))

(defn hero-route []
  {:status 200 :content-type "application/json"
   :body (json/generate-string {:hi "I'm hero" :enemy "Joker" :weaponId 1})})

(defn execute-query
  ([base-url query]
   (execute-query base-url query {}))
  ([base-url query params]
   (restql/execute-query :mappings {:hero (str base-url "/hero")
                                    :weapon (str base-url "/weapon/:id?:villain")}
                         :query query
                         :params params)))

(defn get-stub-body [request]
  (if-not (nil? (:body request))
          (val (first (:body request)))))

(defn stub-post-route
  ([path]
   (stub-post-route path nil nil))
  ([path body]
   (stub-post-route path body nil))
  ([path body query-params]
    (fn [request]
      (and (= (get request :path) path)
           (= (get request :method) "POST")
           (= (get request :query-params) query-params)
           (= (get-stub-body request) (if-not (nil? body) (json/generate-string body)))))))

(deftest request-with-post-query
  (testing "Post with simple body"
    (with-routes! {(stub-post-route "/hero" {:id 1}) (hero-route)}
      (let [result (execute-query uri "to hero with id = 1")]
        (is (= 200 (get-in result [:hero :details :status]))))))

  (testing "Post with path and query string variables"
    (with-routes! {(stub-post-route "/weapon/1" nil {:villain "Joker"}) (hero-route)}
      (let [result (execute-query uri "to weapon with id = 1, villain = \"Joker\"")]
        (is (= 200 (get-in result [:weapon :details :status]))))))

  (testing "Post with body, path and query string variables"
    (with-routes! {(stub-post-route "/weapon/1" {:kills 4} {:villain "Joker"}) (hero-route)}
      (let [result (execute-query uri "to weapon with id = 1, villain = \"Joker\", kills = 4")]
        (is (= 200 (get-in result [:weapon :details :status]))))))

  (testing "Post with chained params"
    (with-routes! {"/hero" (hero-route)
                   (stub-post-route "/weapon/1" {:kills 1} {:villain "Joker"}) (hero-route)}
      (let [result (execute-query uri "from hero \n to weapon with id = hero.weaponId, villain = hero.enemy, kills = 1")]
        (is (= 200 (get-in result [:hero :details :status])))
        (is (= 200 (get-in result [:weapon :details :status]))))))

  (testing "Post with variable rule"
    (with-routes! {(stub-post-route "/hero" {:kills 1}) (hero-route)}
      (let [result (execute-query uri "to hero with $body" {:body {:kills 1}})]
        (is (= 200 (get-in result [:hero :details :status]))))))

  (testing "Post with variable rule and params"
    (with-routes! {(stub-post-route "/weapon/1" {:kills 1} {:villain "Joker"}) (hero-route)}
      (let [result (execute-query uri "to weapon with $body, id = $id, villain = \"Joker\"" {:body {:kills 1} :id 1})]
        (is (= 200 (get-in result [:weapon :details :status])))))))

