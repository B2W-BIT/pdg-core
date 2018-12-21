(ns restql.core.response.headers-test
  (:require [clojure.test :refer [deftest is]])
  (:use restql.core.response.headers))

(defn get-sample-result []
  {:jedis {
    :details {
      :headers {
        :x-type "Jedi"
        :x-weapon "Light Saber"
        :cache-control "max-age=500, s-maxage=1200"
      }
    }
    :result {
      :id 1
      :name "Luke Skywalker"
      :weaponId 2
    }
  }}
)

(defn get-sample-minimal-result []
  {:jedis {
    :details {}
    :result {
      :id 1
      :name "Luke Skywalker"
      :weaponId 2
    }
  }}
)

(defn get-sample-query [] ^{:cache-control 900} [:from "jedis"])

(defn get-sample-minimal-query [] [:from "jedis"])

(deftest map-headers-to-resource-test
  (is
    (= [{:jedis {:x-type "Jedi"
                 :x-weapon "Light Saber"
                 :cache-control "max-age=500, s-maxage=1200"}}]
       (map map-headers-to-resource (get-sample-result))
    )
  )
)

(deftest map-response-headers-by-resources-test
  (is
    (= {:jedis {:x-type "Jedi"
                :x-weapon "Light Saber"
                :cache-control "max-age=500, s-maxage=1200"}}
       (map-response-headers-by-resources (get-sample-result))
    )
  )
)

(deftest get-response-headers-test
  (is
    (= {:x-type-jedis "Jedi" :x-weapon-jedis "Light Saber" :cache-control "max-age=900"}
       (get-response-headers (get-sample-query) (get-sample-result))
    )
  )
  (is
    (= {}
       (get-response-headers (get-sample-minimal-query) (get-sample-minimal-result))
    )
  )
)
