(ns restql.aggregation-test
  (:require [clojure.test :refer :all]
            [restql.test-util :as test-util]
            [restql.parser.core :as parser]
            [restql.core.api.restql :as restql]
            [stub-http.core :refer :all]
            [restql.parser.json :as json]
            [restql.test-util :refer [route-response route-request route-header]]))

(defn execute-query
  ([base-url query]
   (execute-query base-url query {} {}))
  ([base-url query params]
   (execute-query base-url query params {}))
  ([base-url query params options]
   (restql/execute-query :mappings {:hero                (str base-url "/hero")
                                    :heroes              (str base-url "/heroes")
                                    :weapons             (str base-url "/weapons")
                                    :sidekick            (str base-url "/sidekick")
                                    :villain             (str base-url "/villain/:id")
                                    :weapon              (str base-url "/weapon/:id")
                                    :product             (str base-url "/product/:id")
                                    :product-price       (str base-url "/price/:productId")
                                    :product-description (str base-url "/description/:productId")
                                    :fail                "http://not.a.working.endpoint"}
                         :query query
                         :params params
                         :options options)))

(deftest base
  (testing "Simple case"
    (with-routes!
      {"/hero" (test-util/route-response {:id 1})
       {:path "/sidekick" :query-params {:id "1"}} (test-util/route-response {:name "Papaleguas" :id 1})}
      (let [result (execute-query uri "from hero\n from sidekick in hero.sidekick with id = 1")]
        (is (= 200 (get-in result [:hero :details :status])))
        (is (= 200 (get-in result [:sidekick :details :status])))

        (is (= {:sidekick {:name "Papaleguas" :id 1} :id 1}
               (get-in result [:hero :result])))))))

(deftest with-chain
  (testing "Simple chained"
    (with-routes!
      {"/heroes"     (route-response {:villain {:id "1"}})
       "/villain/1"  (route-response {:id "1" :name "Lex"})}
      (let [result (execute-query uri "from heroes\n
                                      from villain in heroes.villain with id = heroes.villain.id")]
        (is (= {:villain {:id "1" :name "Lex"}} (get-in result [:heroes :result])))
        (is (nil? (get-in result [:villain :result])))
        (is (get-in result [:heroes :details]))
        (is (get-in result [:villain :details])))))

  (testing "Chained with list"
    (with-routes!
      {"/heroes"     (route-response {:villains ["1" "2"]})
       "/villain/1"  (route-response {:id "1" :name "Lex"})
       "/villain/2"  (route-response {:id "2" :name "Zod"})}
      (let [result (execute-query uri "from heroes\n
                                      from villain in heroes.villains with id = heroes.villains.id")]
        (is (= {:villains [{:id "1" :name "Lex"} {:id "2" :name "Zod"}]} (get-in result [:heroes :result])))
        (is (nil? (get-in result [:villain :result])))
        (is (get-in result [:heroes :details]))
        (is (get-in result [:villain :details])))))

  (testing "Chained with deep list"
    (with-routes!
      {"/heroes"     (route-response [{:villains [{:id "1"} {:id "2"}]} {:villains [{:id "3"} {:id "4"}]}])
       "/weapon/1"   (route-response {:id "DAGGER"})
       "/weapon/2"   (route-response {:id "GUN"})
       "/weapon/3"   (route-response {:id "SHOTGUN"})
       "/weapon/4"   (route-response {:id "SWORD"})}
      (let [result (execute-query uri "from heroes\n
                                      from weapon in heroes.villains.weapons with id = heroes.villains.id")]
        (is (= [{:villains [{:id "1", :weapons {:id "DAGGER"}}
                            {:id "2", :weapons {:id "GUN"}}]}
                {:villains [{:id "3", :weapons {:id "SHOTGUN"}}
                            {:id "4", :weapons {:id "SWORD"}}]}] (get-in result [:heroes :result])))
        (is (nil? (get-in result [:weapon :result])))
        (is (get-in result [:heroes :details]))
        (is (get-in result [:weapon :details])))))

  (with-routes!
    {"/heroes"     (route-response {:villains [{:id "1"} {:id "2"}]})
     "/villain/1"  (route-response {:id "1" :name "Lex"})
     "/villain/2"  (route-response {:id "2" :name "Zod"})}
    (let [result (execute-query uri "from heroes\n
                                      from villain in heroes.villains with id = heroes.villains.id")]
      (is (= {:villains [{:id "1" :name "Lex"} {:id "2" :name "Zod"}]} (get-in result [:heroes :result])))
      (is (nil? (get-in result [:villain :result])))
      (is (get-in result [:heroes :details]))
      (is (get-in result [:villain :details]))))

(with-routes!
  {"/heroes"     (route-response [{:villains [{:id "1"} {:id "2"}]}
                                  {:villains [{:id "3"} {:id "4"}]}])
   "/villain/1"  (route-response {:id "1" :name "Lex"})
   "/villain/2"  (route-response {:id "2" :name "Zod"})
   "/villain/3"  (route-response {:id "3" :name "Elektra"})
   "/villain/4"  (route-response {:id "4" :name "Dracula"})}
  (let [result (execute-query uri "from heroes\n
                                     from villain in heroes.villains with id = heroes.villains.id")]
    (is (= [{:villains [{:id "1" :name "Lex"} {:id "2" :name "Zod"}]}
            {:villains [{:id "3" :name "Elektra"} {:id "4" :name "Dracula"}]}] (get-in result [:heroes :result])))
    (is (nil? (get-in result [:villain :result])))
    (is (get-in result [:heroes :details]))
    (is (get-in result [:villain :details]))))

(with-routes!
  {"/heroes"     (route-response [{:villains [{:id "1" :weapons ["DAGGER"]}
                                              {:id "2" :weapons ["GUN"]}]}
                                  {:villains [{:id "3" :weapons ["SWORD"]}
                                              {:id "4" :weapons ["SHOTGUN"]}]}])
   "/villain/1"  (route-response {:name "Lex"})
   "/villain/2"  (route-response {:name "Zod"})
   "/villain/3"  (route-response {:name "Elektra"})
   "/villain/4"  (route-response {:name "Dracula"})
   "/weapon/DAGGER"   (route-response {:id "DAGGER"})
   "/weapon/GUN"      (route-response {:id "GUN"})
   "/weapon/SWORD"    (route-response {:id "SWORD"})
   "/weapon/SHOTGUN"  (route-response {:id "SHOTGUN"})}
  (let [result (execute-query uri "from heroes\n
                                      from villain in heroes.villains.id with id = heroes.villains.id\n
                                      from weapon in heroes.villains.weapons with id = heroes.villains.weapons")]
    (is (= [{:villains [{:id {:name "Lex"} :weapons [{:id "DAGGER"}]}
                        {:id {:name "Zod"} :weapons [{:id "GUN"}]}]}
            {:villains [{:id {:name "Elektra"} :weapons [{:id "SWORD"}]}
                        {:id {:name "Dracula"} :weapons [{:id "SHOTGUN"}]}]}] (get-in result [:heroes :result])))
    (is (nil? (get-in result [:villain :result])))
    (is (nil? (get-in result [:weapon :result])))
    (is (get-in result [:heroes :details]))
    (is (get-in result [:villain :details]))
    (is (get-in result [:weapon :details]))))

(with-routes!
  {"/heroes"     (route-response [{:villains [{:id "1"}]}])
   "/villain/1"  (route-response {:name "1" :weapons ["DAGGER"]})
   "/villain/2"  (route-response {:name "2" :weapons ["GUN"]})
   "/villain/3"  (route-response {:name "3" :weapons ["SHOTGUN"]})
   "/villain/4"  (route-response {:name "4" :weapons ["SWORD"]})
   "/weapon/DAGGER"   (route-response {:id "DAGGER"})
   "/weapon/GUN"      (route-response {:id "GUN"})
   "/weapon/SHOTGUN"      (route-response {:id "SHOTGUN"})
   "/weapon/SWORD"      (route-response {:id "SWORD"})}
  (let [result (execute-query uri "from heroes\n
                                      from villain with id = [1,2,3,4]\n
                                      from weapon in villain.weapons with id = villain.weapons")]
    (is (= [{:villains [{:id "1"}]}] (get-in result [:heroes :result])))
    (is (= [{:name "1", :weapons [{:id "DAGGER"}]}
            {:name "2", :weapons [{:id "GUN"}]}
            {:name "3", :weapons [{:id "SHOTGUN"}]}
            {:name "4", :weapons [{:id "SWORD"}]}] (get-in result [:villain :result])))
    (is (nil? (get-in result [:weapon :result])))
    (is (get-in result [:heroes :details]))
    (is (get-in result [:villain :details]))
    (is (get-in result [:weapon :details]))))

(with-routes!
  {"/heroes"     (route-response [{:villains [{:id "1"} {:id "2"}]} {:villains [{:id "3"} {:id "4"}]}])
   "/villain/1"  (route-response {:name "1" :weapons ["DAGGER"]})
   "/villain/2"  (route-response {:name "2" :weapons ["GUN"]})
   "/villain/3"  (route-response {:name "3" :weapons ["SHOTGUN"]})
   "/villain/4"  (route-response {:name "4" :weapons ["SWORD"]})
   "/weapon/DAGGER"   (route-response {:id "DAGGER"})
   "/weapon/GUN"      (route-response {:id "GUN"})
   "/weapon/SHOTGUN"      (route-response {:id "SHOTGUN"})
   "/weapon/SWORD"      (route-response {:id "SWORD"})}
  (let [result (execute-query uri "from heroes\n
                                      from villain with id = heroes.villains.id\n
                                      from weapon in villain.weapons with id = villain.weapons")]
    (is (= [{:villains [{:id "1"} {:id "2"}]}
            {:villains [{:id "3"} {:id "4"}]}] (get-in result [:heroes :result])))
    (is (= [[{:name "1", :weapons [{:id "DAGGER"}]}
             {:name "2", :weapons [{:id "GUN"}]}]
            [{:name "3", :weapons [{:id "SHOTGUN"}]}
             {:name "4", :weapons [{:id "SWORD"}]}]] (get-in result [:villain :result])))
    (is (nil? (get-in result [:weapon :result])))
    (is (get-in result [:heroes :details]))
    (is (get-in result [:villain :details]))
    (is (get-in result [:weapon :details]))))

(with-routes!
  {"/heroes"     (route-response [{:villains [{:id "1"} {:id "2"}]} {:villains [{:id "3"} {:id "4"}]}])
   "/villain/1"  (route-response {:name "1" :weapons ["DAGGER"]})
   "/villain/2"  (route-response {:name "2" :weapons ["GUN"]})
   "/villain/3"  (route-response {:name "3" :weapons ["SHOTGUN"]})
   "/villain/4"  (route-response {:name "4" :weapons ["SWORD"]})
   "/weapon/DAGGER"   (route-response {:id "DAGGER"})
   "/weapon/GUN"      (route-response {:id "GUN"})
   "/weapon/SHOTGUN"  (route-response {:id "SHOTGUN"})
   "/weapon/SWORD"    (route-response {:id "SWORD"})}
  (let [result (execute-query uri "from heroes\n
                                      from villain in heroes.villains with id = heroes.villains.id\n
                                      from weapon in heroes.villains.weapons with id = villain.weapons")]
    (is (= [{:villains [{:name "1", :weapons [{:id "DAGGER"}]}
                        {:name "2", :weapons [{:id "GUN"}]}]}
            {:villains [{:name "3", :weapons [{:id "SHOTGUN"}]}
                        {:name "4", :weapons [{:id "SWORD"}]}]}] (get-in result [:heroes :result])))
    (is (nil? (get-in result [:villain :result])))
    (is (nil? (get-in result [:weapon :result])))
    (is (get-in result [:heroes :details]))
    (is (get-in result [:villain :details]))
    (is (get-in result [:weapon :details]))))

(with-routes!
  {"/heroes"     (route-response [{:villains [{:id "1" :weapons ["DAGGER"]} {:id "2" :weapons ["GUN"]}]}])
   "/villain/1"  (route-response {:name "Lex"})
   "/villain/2"  {:status 500 :content-type "application/json" :body (json/generate-string {:error "UNEXPECTED_ERROR"})}
   "/weapon/GUN"      (route-response {:id "GUN"})
   "/weapon/DAGGER"   {:status 404 :content-type "application/json" :body (json/generate-string {:error "NOT_FOUND"})}}
  (let [result (execute-query uri "from heroes\n
                                      from villain in heroes.villains.id with id = heroes.villains.id\n
                                      from weapon in heroes.villains.weapons with id = heroes.villains.weapons")]
    (is (= [{:villains [{:id {:name "Lex"} :weapons [{:error "NOT_FOUND"}]}
                        {:id {:error "UNEXPECTED_ERROR"} :weapons [{:id "GUN"}]}]}] (get-in result [:heroes :result])))
    (is (nil? (get-in result [:villain :result])))
    (is (nil? (get-in result [:weapon :result])))
    (is (get-in result [:heroes :details]))
    (is (get-in result [:villain :details]))
    (is (get-in result [:weapon :details])))))

(deftest with-list
  (testing "With deep list"
    (with-routes!
      {"/hero" (test-util/route-response {:weapons [{:sidekicks [{:history {:id 1}}]}]})
       {:path "/sidekick" :query-params {:id "1"}} (test-util/route-response {:book "sidekick book" :chapter 1})}
      (let [result (execute-query uri "from hero\n from sidekick in hero.weapons.sidekicks.history.info with id = 1")]
        (is (= 200 (get-in result [:hero :details :status])))
        (is (= 200 (get-in result [:sidekick :details :status])))

        (is (= {:weapons '({:sidekicks ({:history {:info {:book "sidekick book" :chapter 1} :id 1}})})}
               (get-in result [:hero :result])))))))
