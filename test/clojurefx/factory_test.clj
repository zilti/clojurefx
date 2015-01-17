(ns clojurefx.factory-test
  (:refer-clojure :exclude [compile meta with-meta])
  (:require [clojurefx.protocols :refer :all])
  (:use midje.sweet
        clojurefx.factory)
  (:import (javafx.scene.control Button Label ScrollPane)
           (javafx.scene.layout VBox)))

(def fxml-node (atom nil))

(facts "FXML loading"
       (fact "Load the fxml file"
             (type (reset! fxml-node (load-fxml "resources/test.fxml"))) => javafx.scene.layout.VBox)
       (fact "Get VBox id"
             (.getId @fxml-node) => "topBox"))

(def example-graph
  [VBox {:id "topBox"
         :children [Button {:id "button"
                            :text "Close"}
                    ScrollPane {:content [Label {:id "label"
                                                 :text "This rocks."}]}]}])

(def scene-graph (atom nil))

(facts "Vector compilation"
       (fact "Simple element"
             (type (compile [Label {:text "Hello ClojureFX"}])) => javafx.scene.control.Label)
       (fact "Nested structure"
             (type (reset! scene-graph (compile example-graph))) => javafx.scene.layout.VBox
             (get-id @scene-graph) => "topBox"))
