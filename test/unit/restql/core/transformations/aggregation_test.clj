(ns restql.core.transformations.aggregation-test
  (:require [clojure.test :refer :all]
            [restql.core.transformations.aggregation :as aggregation]))

(deftest aggregation
  (let [query [[:hero {:from :hero :with {:id 123 :sidekickId 456}}]
               [:sidekick {:from :sidekick :in :hero.sidekickId :with {:id [:hero :sidekickId]}}]]
        result {:hero {:result {:id 123 :sidekickId 456} :details {:status 200}}
                :sidekick {:result {:hi "I'm sidekick"} :details {:status 200}}}]
    (is (=  {:hero {:details {:status 200} :result {:id 123 :sidekickId {:hi "I'm sidekick"}}}
             :sidekick {:details {:status 200}}}
            (aggregation/aggregate query result)))))

(deftest resource-does-not-exist
  (let [query [[:product {:from :product :with {:id 123}}]
               [:description.price {:from :product-price :with {:productId [:product :id]}}]]
        result {:product           {:result {:id 123} :details {:status 200}}
                :description.price {:result {:price 99} :details {:status 200}}}]
    (is (= (aggregation/aggregate query result) {:product           {:details {:status 200} :result {:id 123}}
                                                 :description.price {:result {:price 99} :details {:status 200}}}))))