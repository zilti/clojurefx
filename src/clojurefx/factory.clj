(ns clojurefx.factory 
  (:require [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [clojurefx.clojurefx :as fx]
            [clojurefx.protocols :refer :all])
  (:import (javafx.scene Scene Node Parent)
           (javafx.scene.layout Region)))

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
         :text        (with-meta [#'value #'set-value!] {:argument String :parent FXValue})
         :value       (with-meta [#'value #'set-value!] {:argument Object :parent FXValue})
         ;;; FXId
         :id          (with-meta [#'id #'set-id!] {:argument String :parent FXId})
         ;;; FXGraphic
         :graphic     (with-meta [#'graphic #'set-graphic!] {:argument Node :parent FXGraphic})
         ;;; FXContainer
         :content     (with-meta [#'content #'set-content!] {:argument Node :parent FXContainer})
         ;;; FXParent
         :children    (with-meta [#'subnodes #'set-subnodes!] {:argument java.util.List :parent FXParent})
         ;;; FXRegion
         ;;         :width       (with-meta [#'width] {:argument Region :parent FXRegion})
         :min-width   (with-meta [#'min-width #'set-min-width!] {:argument Region :parent FXRegion})
         :max-width   (with-meta [#'max-width #'set-max-width!] {:argument Region :parent FXRegion})
         :pref-width  (with-meta [#'pref-width #'set-pref-width!] {:argument Region :parent FXRegion})
         ;;         :height      (with-meta [#'height] {:argument Region :parent FXRegion})
         :min-height  (with-meta [#'min-height #'set-min-height!] {:argument Region :parent FXRegion})
         :max-height  (with-meta [#'max-height #'set-max-height!] {:argument Region :parent FXRegion})
         :pref-height (with-meta [#'pref-height #'set-pref-height!] {:argument Region :parent FXRegion})
         ;;; FXStyleSetter / FXStyleable
         :style       (with-meta [#'style #'set-style!] {:argument String :parent FXStyleable})
         ;;; FXOnAction
         :action      (with-meta [#'action #'set-action!] {:argument clojure.lang.IFn :parent FXOnAction})
         ;;; FXStage
         :title       (with-meta [#'title #'set-title!] {:argument String :parent FXStage})
         :scene       (with-meta [#'scene #'set-scene!] {:argument Scene :parent FXStage})
         ;;; FXScene
         :root        (with-meta [#'root #'set-root!] {:argument Parent :parent FXScene})
         ;;; FXRectangle
         :arc-height  (with-meta [#'arc-height #'set-arc-height!] {:argument Double :parent FXRectangle})
         :arc-width   (with-meta [#'arc-width #'set-arc-width!] {:argument Double :parent FXRectangle})
         :height      (with-meta [#'height #'set-width!] {:argument Double :parent FXRectangle})
         :width       (with-meta [#'height #'set-height!] {:argument Double :parent FXRectangle})
         :x           (with-meta [#'x #'set-x!] {:argument Double :parent FXRectangle})
         :y           (with-meta [#'y #'set-y!] {:argument Double :parent FXRectangle})
         }))

(def constructor-args
  (atom {javafx.scene.Scene [:root]
         javafx.stage.Stage [:style]}))

(declare compile-o-matic)
(defn apply-props-to-node [node props]
  (debug "Applying" (count props) "properties to" node)
  (doseq [[k v] props]
    (let [translation (get @translation-map k)
          {:keys [argument parent]} (meta translation)
          v (compile-o-matic v)]
      (debug "Key:" k "Value:" v " " (type v) "Translation:" translation)
      (when (nil? translation)
        (error (str "Property" k "not available in translation map."))
        ;;(throw (Exception. (str "Property" k "not available in translation map.")))
        )
      (try ((setter translation) node v)
           (catch Exception e (error e)))))
  (debug "Done applying properties for" node)
  node)

(defn build-node [object props]
  (debug "build-node:" object props)
  (let [cargs (get @constructor-args object)
        form `(~object new)]
    (debug "Constructor args for" (class object) ":" cargs "->" props)
    (apply-props-to-node
      (->> (reduce (fn [form mandatory]
                     (if-let [entry (compile-o-matic (get props mandatory))]
                       (cons entry form)
                       form)) form cargs)
           reverse
           eval)
      (apply dissoc props cargs))))

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
