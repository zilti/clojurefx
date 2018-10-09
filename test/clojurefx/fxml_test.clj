(ns clojurefx.fxml-test
  (:require [clojurefx.fxml :as sut]
            [clojure.core.async :as async :refer [<! >! chan go go-loop]]
            [clojure.test :as t]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]))
(timbre/refer-timbre)

(defonce force-toolkit-init (javafx.embed.swing.JFXPanel.))

(def test1-fxml (io/resource "fxml/exampleWindow.fxml"))

(t/deftest fxml-loading
  (debug "FXML loading")
  (t/is (instance? javafx.scene.Node (sut/load-fxml test1-fxml))))

(def test2-fxml (io/resource "fxml/exampleControllerWindow.fxml"))

(t/deftest controller-generation
  (t/is (instance? java.lang.Class (sut/generate-controller test2-fxml "a.b/c"))))



(def instance (atom nil))
(def clicked (atom false))

(defn initialize [inst]
  (reset! instance inst))

(defn test-1-click [_ e]
  (reset! clicked true))

(sut/load-fxml-with-controller test2-fxml "clojurefx.fxml-test/initialize")

(t/deftest proper-init
  (t/is (instance? ch.lyrion.Test1 @instance)))

(.fire (.simpleButton @instance))

(t/deftest testfire-result
  (t/is @clicked))
