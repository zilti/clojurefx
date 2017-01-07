(ns clojurefx.factory 
  (:require [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [clojurefx.clojurefx :as fx]
            [clojurefx.protocols :refer :all])
  (:import (javafx.scene Scene Node Parent)))

(timbre/refer-timbre)

;;## FXMLLoader

(defn load-fxml [filename]
  (let [loader (new javafx.fxml.FXMLLoader)]
    (.setLocation loader (io/resource ""))
    (.load loader (-> filename io/resource io/input-stream))))

;;## VectorBuilder

(def getter first)
(def setter second)

(def translation-map
  (atom {;;; FXValue
         :text (with-meta [#'value #'set-value!] {:argument String :parent FXValue})
         :value (with-meta [#'value #'set-value!] {:argument Object :parent FXValue})
         ;;; FXId
         :id (with-meta [#'id #'set-id!] {:argument String :parent FXId})
         ;;; FXGraphic
         :graphic (with-meta [#'graphic #'set-graphic!] {:argument Node :parent FXGraphic})
         ;;; FXContainer
         :content (with-meta [#'content #'set-content!] {:argument Node :parent FXContainer})
         ;;; FXParent
         :children (with-meta [#'subnodes #'set-subnodes!] {:argument java.util.List :parent FXParent})
         ;;; FXStyleSetter / FXStyleable
         :style (with-meta [#'style #'set-style!] {:argument String :parent FXStyleable})
         ;;; FXOnAction
         :action (with-meta [#'action #'set-action!] {:argument clojure.lang.IFn :parent FXOnAction})
         ;;; FXStage
         :title (with-meta [#'title #'set-title!] {:argument String :parent FXStage})
         :scene (with-meta [#'scene #'set-scene!] {:argument Scene :parent FXStage})
         ;;; FXScene
         :root (with-meta [#'root #'set-root!] {:argument Parent :parent FXScene})}))

(def mandatory-constructor-args
  (atom {javafx.scene.Scene [:root]}))

(declare compile-o-matic)
(defn apply-props-to-node [node props]
  (doseq [[k v] props]
    (let [translation (get @translation-map k)
          {:keys [argument parent]} (meta translation)
          v (compile-o-matic v)]
      (trace "Key:" k " " (type k) "Value:" v " " (type v))
      (when (nil? translation)
        (throw (Exception. (str "Property" k "not available in translation map."))))
      ((setter translation) node v)))
  node)

(defn build-node [object props]
  (debug "build-node:" object props)
  (let [mandatory (get mandatory-constructor-args object)
        form `(~object new)]
    (apply-props-to-node
     (-> (reduce (fn [form mandatory]
                   (if-let [entry (get props mandatory)]
                     (cons entry form)
                     form)) form mandatory)
         reverse
         eval)
     (apply dissoc props mandatory))))

(defn compile
  ([args] (compile args []))
  ([[obj & other] accu]
   (cond
     (nil? obj) accu
     (and (empty? other) (empty? accu)) obj
     (and (empty? (rest other)) (empty? accu)) (build-node obj (first other))
     (class? obj) (recur (rest other) (conj accu (build-node obj (first other))))
     :else (recur other (conj accu obj)))))

(defn compile-o-matic [thing]
  (if (instance? java.util.List thing)
    (if (and (not (coll? (first thing))) (map? (second thing)))
      (compile thing)
      thing)
    thing))

(comment
  (import (javafx.scene.layout VBox)
          (javafx.scene.control Button ScrollPane Label))
  
  (def example-graph
    [VBox {:id "topBox"
           :children [Button {:id "button"
                              :text "Close"}
                      ScrollPane {:content [Label {:id "label"
                                                   :text "This rocks."}]}]}])

  (def example-graph2
    [VBox {:id "topBox"
           :children [Button {:id "button"
                              :text "Close"}
                      (new javafx.scene.control.Label "Precompiled")
                      Button {:id "button2"
                              :text "OK"}
                      ScrollPane {:content [Label {:id "label"
                                                   :text "This rocks."}]}]}]))
