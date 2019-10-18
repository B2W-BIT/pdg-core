(defproject b2wdigital/restql-core "3.5.3"
  :description "Microservice query language"
  :url "https://github.com/B2W-Digital/restQL-clojure"
  :license {:name "MIT"
            :url "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/core.async "0.4.500"]
                 [org.clojure/tools.logging "0.5.0"]
                 [org.clojure/tools.reader "1.3.2"]
                 [aleph "0.4.6"]
                 [r0man/environ "1.1.0"]
                 [instaparse "1.4.10"]
                 [prismatic/schema "1.1.12"]
                 [ring/ring-codec "1.1.2"]
                 [funcool/httpurr "1.1.0"]
                 [funcool/promesa "2.0.1"]
                 [slingshot "0.12.2"]
                 [metosin/jsonista "0.2.4"]]

  :source-paths ["src/main"]
  :test-paths ["test/integration" "test/unit"]
  :resource-paths ["src/resources"]
  :aot [restql.core.api.RestQLJavaApi]
  :hooks [leiningen.cljsbuild]

  :plugins [[lein-ancient "0.6.15"]
            [lein-cloverage "1.1.0"]
            [lein-cljsbuild "1.1.7"]]

  :profiles {:dev [:project/dev]
             :test [:project/dev]
             :uberjar {:omit-source true
                       :aot :all}
             :auth {#"clojars" {:username :env :password :env}}
             :project/dev  {:dependencies [[pjstadig/humane-test-output "0.9.0"]
                                           [se.haleby/stub-http "0.2.7"]]
                            :plugins      [[com.jakemccrary/lein-test-refresh "0.24.1"]]
                            :test-refresh {:quiet true
                                           :changes-only true}
                            :injections [(require 'pjstadig.humane-test-output)
                                         (pjstadig.humane-test-output/activate!)]}}

  :cljsbuild {:builds [{:id "node"
                        :source-paths ["src"]
                        :compiler {:main restql.core.api.restql
                                   :output-to "dist/lib/restql.js"
                                   :output-dir "dist/lib"
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
                                     :sign-releases false}]])
