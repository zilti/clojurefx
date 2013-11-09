(defproject clojurefx "0.0.7"
  :description "Helper functions and probably a wrapper to simplify usage of JavaFX in Clojure.

  This is meant to be used with Java 8. If you add JavaFX 2.2 to your classpath it might still work, but that isn't tested.
  
  [This Project On GitHub](https://www.github.com/zilti/clojurefx)

**Installation: `[clojurefx \"0.0.7\"]`**"
  :url "https://www.github.com/zilti/clojurefx"
  :lein-release {:deploy-via :clojars}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :plugins [[lein-marginalia "0.7.1"]
            [lein-midje "3.1.3-RC2"]
            [lein-release "1.0.5"]]
  :profiles {:dev {:dependencies [[midje "1.6-beta1"]]}})
