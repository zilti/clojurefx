(defproject clojurefx "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.taoensso/timbre "4.7.4" :exclusions [com.taoensso/carmine]]
                 [org.clojure/core.typed "0.3.26"]
                 [clojure-jsr-223 "0.1.0"]]
  :injections [(require 'clojure.core.typed)
               (clojure.core.typed/install)]
  :profiles {:uberjar {:aot :all}})
