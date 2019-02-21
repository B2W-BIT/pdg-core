(ns restql.core.request.statement.apply-encoders
  (:require [restql.core.util.deep-merge :refer [deep-merge]]
            [restql.core.encoders.core :as encoder]))

(defn- encode-param-value [encoders [param-key param-value]]
  (assoc {} param-key (encoder/encode encoders param-value)))

(defn- encode-params [encoders statements]
  (map #(->> %
             (:with)
             (map (partial encode-param-value encoders))
             (into {})
             (assoc {} :with)
             (deep-merge  %)) statements))

(defn apply-encoders [encoders expanded-statements]
  (if (sequential? (first expanded-statements))
    (map (partial apply-encoders encoders) expanded-statements)
    (encode-params encoders expanded-statements)))
