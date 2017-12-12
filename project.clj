(defproject clojurefx/clojurefx "0.5.0-SNAPSHOT"
  :description "A Clojure wrapper for JavaFX."
  :license "Like Clojure."
  :url "https://www.bitbucket.org/zilti/clojurefx"
  :signing {:gpg-key "68484437"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [swiss-arrows "1.0.0"]
                 [camel-snake-kebab "0.4.0"]
                 [com.taoensso/timbre "4.10.0" :exclusions [com.taoensso/carmine]]
                 [net.openhft/compiler "2.3.0"]
                 [org.ow2.asm/asm "6.0"]
                 [org.ow2.asm/asm-util "6.0"] 
                 [clojure-jsr-223 "0.1.0"]
                 ]
  :profiles {:test {:source-paths ["test"]
                    :resource-paths ["test-resources"]}
             :uberjar {:aot :all}}
  :aot :all
  :omit-source true
  :source-paths ["src"]
  :java-source-paths ["src"])
