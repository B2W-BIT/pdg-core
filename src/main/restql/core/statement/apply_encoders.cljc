(ns restql.core.statement.apply-encoders
  (:require [restql.core.util.deep-merge :refer [deep-merge]]
            [restql.core.encoders.core :as encoder]))

(defn- post-put-patch? [statement]
  (->> [:post :put :patch]
       (some #(= % (:method statement)))))

(defn- has-encoder? [param-value]
  (-> param-value
      (meta)
      (:encoder)
      (some?)))

(defn- encode-param-value [encoders statement [param-key param-value]]
  (if (or (not (post-put-patch? statement))
          (has-encoder? param-value))
    (assoc {} param-key (encoder/encode encoders param-value))
    (assoc {} param-key param-value)))

(defn- encode-params [encoders statement]
  (->> statement
       (:with)
       (map (partial encode-param-value encoders statement))
       (into {})
       (assoc {} :with)
       (deep-merge statement)))

(defn apply-encoders [encoders expanded-statements]
  (if (sequential? (first expanded-statements))
    (map (partial apply-encoders encoders) expanded-statements)
    (map (partial encode-params encoders) expanded-statements)))
