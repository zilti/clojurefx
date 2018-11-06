(defproject clojurefx/clojurefx "0.5.1-SNAPSHOT"
  :description "A Clojure wrapper for JavaFX."
  :license "Like Clojure."
  :url "https://www.bitbucket.org/zilti/clojurefx"
  :signing {:gpg-key "68484437"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474" :scope "test"]
                 ;;[org.clojars.tristefigure/shuriken "0.14.28" :scope "test"]
                 [org.openjfx/javafx-fxml "11-ea+25" :scope "test"]
                 [org.openjfx/javafx-swing "11-ea+25" :scope "test"]
                 [swiss-arrows "1.0.0"]
                 [camel-snake-kebab "0.4.0"]
                 [com.taoensso/timbre "4.10.0" :exclusions [com.taoensso/carmine]]
                 [net.openhft/compiler "2.3.1"]
                 [org.ow2.asm/asm "6.2.1"]
                 [org.ow2.asm/asm-util "6.2.1"]
                 [clojure-jsr-223 "0.1.0"]
                 ]
  :profiles {:test {:source-paths ["test"]
                    :resource-paths ["test-resources"]
                    :aot :all}
             :uberjar {:aot :all}}
  :aot :all
  :omit-source true
  :source-paths ["src"]
  :java-source-paths ["src"])
