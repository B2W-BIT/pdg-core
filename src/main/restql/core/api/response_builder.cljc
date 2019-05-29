(ns restql.core.api.response-builder)

(defn- status-code-ok [query-response]
  (and
   (not (nil? (:status query-response)))
   (< (:status query-response) 300)))

(defn- is-success [query-response]
  (and
   (status-code-ok query-response)
   (nil? (:parse-error query-response))))

(defn stringify-values [a-map]
  (reduce-kv (fn [m k v] (assoc m k (str v))) {} a-map))

(defn- append-metadata [response query-response]
  (let [metadata (:metadata query-response)]
    (if (nil? metadata)
      (assoc response :metadata {})
      (assoc response :metadata (stringify-values metadata)))))

(defn append-debug-data [response query-response]
  (if (:debug query-response)
    (assoc response :debug (:debug query-response))
    response))

(defn get-details [query-response]
  (if (sequential? query-response)
    (map get-details query-response)
    (-> {}
        (assoc :success (is-success query-response)
               :status (:status query-response)
               :headers (:headers query-response))
        (append-metadata query-response)
        (append-debug-data query-response))))

(defn get-body-response [query-response]
  (if (sequential? query-response)
    (map get-body-response query-response)
    (:body query-response)))

(defn prepare-response [query-response]
    ; Sequential means it's a multiplexed call.
  {:details (if (sequential? query-response)
              (map get-details query-response)
              (get-details query-response))
   :result  (if (sequential? query-response)
              (map get-body-response query-response)
              (:body query-response))})

(defn build [query-responses]
  (reduce-kv (fn [response resource query-response]
               (assoc response resource (prepare-response query-response))) {} query-responses))