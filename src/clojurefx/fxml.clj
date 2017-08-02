(ns clojurefx.fxml
  (:require clojurefx.controllergen
            [clojure.java.io :as io]))

(defn load-fxml [filename]
  (let [loader (new javafx.fxml.FXMLLoader)]
    (.setLocation loader (io/resource ""))
    (.load loader (-> filename io/input-stream))))

(def generate-controller clojurefx.controllergen/gen-fx-controller-class)

(defn load-fxml-with-controller [filename init-fn]
  (generate-controller filename init-fn)
  (load-fxml filename))

