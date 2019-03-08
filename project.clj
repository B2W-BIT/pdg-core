(defproject b2wdigital/restql-core "2.8.14"
  :description "Microservice query language"
  :url "https://github.com/B2W-Digital/restQL-clojure"
  :license {:name "MIT"
            :url "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "0.4.490"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.clojure/tools.reader "1.3.2"]
                 [aleph "0.4.6"]
                 [cheshire "5.8.1"]
                 [environ "1.1.0"]
                 [instaparse "1.4.10"]
                 [prismatic/schema "1.1.10"]
                 [ring/ring-codec "1.1.1"]
                 [se.haleby/stub-http "0.2.7"]
                 [slingshot "0.12.2"]]
  :aot [restql.core.api.RestQLJavaApi]
  :profiles {:test {:dependencies [[se.haleby/stub-http "0.2.7"]]}
             :uberjar {:aot :all}
             :auth {#"clojars" {:username :env :password :env}}}
  :plugins [[lein-ancient "0.6.15"]
            [lein-cloverage "1.1.0"]]
  :deploy-repositories [["clojars"  {:url "https://repo.clojars.org"
                                     :username :env/clojars_username
                                     :password :env/clojars_password
                                     :sign-releases false}]]
  :source-paths ["src/main"]
  :resource-paths ["src/resources"]
  :test-paths ["test/integration" "test/unit"])
