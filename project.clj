(defproject clojurefx "0.0.2"
  :description "Helper functions and probably a wrapper to simplify usage of JavaFX in Clojure.

You'll need to have jfxrt.jar in your local maven repository. See [this coderwall protip](https://coderwall.com/p/4yjy1a) for how to make this happen.

**Installation: `[clojurefx \"0.0.2\"]`**"
  :url "https://www.github.com/zilti/clojurefx"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.oracle/javafx-runtime "2.2.0"]]
  :plugins [[lein-marginalia "0.7.1"]])
