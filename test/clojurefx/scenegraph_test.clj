(ns clojurefx.scenegraph-test
  (:require [clojurefx.clojurefx :refer :all]
            [clojure.core.async :as async :refer [<! >! chan go go-loop]]
            [clojure.test :refer :all]
            [clojure.test :as t]
            [taoensso.timbre :as timbre
             :refer [info]])
  (:import (javafx.scene.control Label Button)
           (javafx.scene Scene)
           (javafx.scene.layout VBox)))

(go (defonce force-toolkit-init (javafx.embed.swing.JFXPanel.)))
(Thread/sleep 500)

(t/deftest basics
  (t/is (instance? Label (compile [Label {:text "Hi!"}]))))

(t/deftest test-find-child-by-class
  (t/is (instance? Label
                   (first (find-child-by-class (compile [Scene {:root [VBox {:children [Label {:text "Hi!" :style-class ["test"]}]}]}])
                                               "test"))
                   )))

(t/deftest test-find-child-by-id
  (t/is (instance? Label
                   (find-child-by-id (compile [Scene {:root [VBox {:children [Label {:text "Hi!" :id "test"}]}]}])
                                     "test")
                   )))

(t/deftest functional-interfaces-fi
  (let [fired (atom false)
        btn (Button.)]
    (.setOnAction btn (fi javafx.event.EventHandler [event] (reset! fired true)))
    (.fire btn)
    (t/is @fired)))

(t/deftest functional-interfaces-connect
  (let [fired (atom false)
        btn (Button.)]
    (connect btn set-on-action [event] (reset! fired true))
    (.fire btn)
    (t/is @fired)))

(t/deftest connect-in-compile
  (let [fired (atom false)
        graph (compile [Scene {:root [VBox {:children [Button {:id "Button" :connect ['(set-on-action [event] (reset! fired true))]}]}]}])]
    (.fire (find-child-by-id graph "Button"))
    (t/is @fired)))