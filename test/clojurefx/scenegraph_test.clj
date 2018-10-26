(ns clojurefx.scenegraph-test
  (:require [clojurefx.clojurefx :refer :all]
            [clojure.core.async :as async :refer [<! >! chan go go-loop]]
            [clojure.test :refer :all]
            [clojure.test :as t]
            [taoensso.timbre :as timbre
             :refer [info]])
  (:import (javafx.scene.control Label)
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