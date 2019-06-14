(defproject b2wdigital/restql-core "2.9.2"
  :description "Microservice query language"
  :url "https://github.com/B2W-Digital/restQL-clojure"
  :license {:name "MIT"
            :url "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/core.async "0.4.490"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.clojure/tools.reader "1.3.2"]
                 [aleph "0.4.6"]
                 [r0man/environ "1.1.0"]
                 [instaparse "1.4.10"]
                 [prismatic/schema "1.1.10"]
                 [ring/ring-codec "1.1.1"]
                 [se.haleby/stub-http "0.2.7"]
                 [funcool/httpurr "1.1.0"]
                 [funcool/promesa "2.0.1"]
                 [slingshot "0.12.2"]
                 [metosin/jsonista "0.2.2"]]
  :hooks [leiningen.cljsbuild]
  :aot [restql.core.api.RestQLJavaApi]
  :profiles {:test {:dependencies [[se.haleby/stub-http "0.2.7"]]}
             :uberjar {:aot :all}
             :auth {#"clojars" {:username :env :password :env}}}
  :plugins [[lein-ancient "0.6.15"]
            [lein-cloverage "1.1.0"]
            [lein-cljsbuild "1.1.7"]]
  :cljsbuild {:builds [{:id "prod"
                        :source-paths ["src"]
                        :compiler {:main restql.core.api.restql
                                   :output-to "dist/restql.js"
                                   :output-dir "dist/out"
                                   :target :nodejs
                                   :install-deps true
                                   :infer-externs true
                                   :optimizations :simple
                                   :pretty-print false
                                   :npm-deps {"uuid" "3.3.2"
                                              "lru-cache" "5.1.1"}}}]}
  :deploy-repositories [["clojars"  {:url "https://repo.clojars.org"
                                     :username :env/clojars_username
                                     :password :env/clojars_password
                                     :sign-releases false}]]
  :source-paths ["src/main"]
  :resource-paths ["src/resources"]
  :test-paths ["test/integration" "test/unit"])
