(defproject b2wdigital/restql-core "2.8.11"
  :description "Microservice query language"
  :url "https://github.com/B2W-Digital/restQL-clojure"
  :license {:name "MIT"
            :url "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [aleph "0.4.6"]
                 [environ "1.0.0"]
                 [ring/ring-codec "1.1.0"]
                 [slingshot "0.12.2"]
                 [instaparse "1.4.8"]
                 [prismatic/schema "1.1.7"]
                 [org.clojure/core.async "0.3.443"]
                 [cheshire "5.8.1"]
                 [se.haleby/stub-http "0.2.3"]
                 [adzerk/bootlaces "0.1.13"]
                 [org.clojure/tools.reader "1.0.5"]]
  :aot [restql.core.api.RestQLJavaApi]
  :profiles {:test {:dependencies [[se.haleby/stub-http "0.2.3"]]}
             :uberjar {:aot :all}
             :auth {#"clojars" {:username :env :password :env}}}
  :plugins [[lein-cloverage "1.0.7-SNAPSHOT"]
            [lein-ancient "0.6.15"]]
  :deploy-repositories [["clojars"  {:url "https://repo.clojars.org"
                                     :username :env/clojars_username
                                     :password :env/clojars_password
                                     :sign-releases false}]]
  :source-paths ["src/main"]
  :resource-paths ["src/resources"]
  :test-paths ["test/integration" "test/unit"])
