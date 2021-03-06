(defproject org.iplantc/clj-jargon "5.0.0"
  :description "Clojure API on top of iRODS's jargon-core."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.irods.jargon/jargon-core "4.0.2.3-RELEASE"
                  :exclusions [[org.jglobus/JGlobus-Core]
                               [org.slf4j/slf4j-api]
                               [org.slf4j/slf4j-log4j12]]]
                 [org.irods.jargon/jargon-data-utils "4.0.2.3-RELEASE"
                  :exclusions [[org.slf4j/slf4j-api]
                               [org.slf4j/slf4j-log4j12]]]
                 [org.irods.jargon/jargon-ticket "4.0.2.3-RELEASE"
                  :exclusions [[org.slf4j/slf4j-api]
                               [org.slf4j/slf4j-log4j12]]]
                 [org.slf4j/slf4j-api "1.7.12"]
                 [org.slf4j/slf4j-log4j12 "1.7.12"]
                 [slingshot "0.12.2"]
                 [org.iplantc/clojure-commons "5.0.0"]]
  :repositories [["dice.repository"
                  {:url "https://raw.github.com/DICE-UNC/DICE-Maven/master/releases"}]
                 ["iplant.repository"
                  {:url "https://everdene.iplantcollaborative.org/archiva/repository/internal/"}]])
