(defproject clojurefx "0.0.24-SNAPSHOT"
  :description "A Clojure wrapper for JavaFX."
  :license "Like Clojure."
  :url "https://www.bitbucket.org/zilti/clojurefx"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [swiss-arrows "1.0.0"]
                 [com.taoensso/timbre "4.7.4" :exclusions [com.taoensso/carmine]]
                 [clojure-jsr-223 "0.1.0"]]
  ;; :profiles {:uberjar {:aot :all}}      
  :source-paths ["src"]
  :java-source-paths ["src"])
