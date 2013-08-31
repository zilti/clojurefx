(ns clojurefx.core-test
  (:require [clojure.test :refer :all]
            [clojurefx.core :refer :all]))

(deftest runnow
  (testing "Uses correct thread"
    (is (= javafx.scene.Scene (type (build scene {})))))
  (testing "Nested run-nows"
    (is (= javafx.scene.Scene (type (build scene {:root (build v-box {})}))))))
