(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/info-typer "5.0.0"
  :description "An AMQP based info type detection service for iRODS"
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "info-typer-standalone.jar"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.novemberain/langohr "3.1.0"]
                 [me.raynes/fs "1.4.6"]
                 [org.iplantc/clj-jargon "5.0.0"
                   :exclusions [[org.slf4j/slf4j-api]
                                [org.slf4j/slf4j-log4j12]
                                [log4j]]]
                 [org.iplantc/clojure-commons "5.0.0" :exclusions [commons-logging]]
                 [org.iplantc/common-cli "5.0.0"]
                 [org.iplantc/heuristomancer "5.0.0"]
                 [org.iplantc/service-logging "5.0.0"]]
  :plugins [[org.iplantc/lein-iplant-rpm "5.0.0"]]
  :main ^:skip-aot info-typer.core
  :profiles {:dev     {:resource-paths ["conf/test"]}
             :uberjar {:aot :all}}
  :iplant-rpm {:summary "iPlant iRODS info type detection service"
               :provides "info-typer"
               :dependencies ["iplant-service-config >= 0.1.0-5" "java-1.7.0-openjdk"]
               :exe-files []
               :config-files ["log4j.properties"]
               :config-path "conf/main"}
  :uberjar-exclusions [#"LICENSE" #"NOTICE"])
