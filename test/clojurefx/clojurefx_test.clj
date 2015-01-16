(ns clojurefx.clojurefx-test
  (:refer-clojure :exclude [compile])
  (:use midje.sweet
        clojurefx.clojurefx))

(def example-hierarchy
  [:VBox {:id "VBox"
          :children [:Label {:text "Hi JavaFX!"}
                     :Label {:text "Hi Clojure!"}]}])

(fact "This compiles."
      (resolv-o-matic :Label) => javafx.scene.control.Label
      (type (compile example-hierarchy)) => javafx.scene.layout.VBox)
