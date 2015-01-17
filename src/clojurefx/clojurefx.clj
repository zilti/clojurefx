(ns clojurefx.clojurefx
  (:refer-clojure :exclude [atom doseq let fn defn ref dotimes defprotocol loop for send meta with-meta])
  (:require [clojure.core.typed :refer :all]
            [clojure.core.typed.unsafe :refer [ignore-with-unchecked-cast]]
            [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [clojure.zip :as zip]
            [clojurefx.protocols :refer :all]
            [clojure.java.io :refer :all]))

(defonce force-toolkit-init (javafx.embed.swing.JFXPanel.))

;; ## Threading helpers

(defn run-later*"
Simple wrapper for Platform/runLater. You should use run-later.
" [f]
(javafx.application.Platform/runLater f))

(defmacro run-later [& body]
  `(run-later* (fn [] ~@body)))

  (defn run-now*"
  A modification of run-later waiting for the running method to return. You should use run-now.
" [f]
(if (javafx.application.Platform/isFxApplicationThread)
  (apply f [])
  (let [result (promise)]
    (run-later
     (deliver result (try (f) (catch Throwable e e))))
    @result)))

  (defmacro run-now "
  Runs the code on the FX application thread and waits until the return value is delivered.
" [& body]
`(run-now* (fn [] ~@body)))

(tc-ignore (timbre/refer-timbre))

(import (javafx.scene.control Labeled Label TextField TextArea CheckBox ComboBox Menu MenuItem MenuBar
                              MenuButton ContextMenu ToolBar SplitPane ScrollPane Accordion
                              TitledPane TabPane Tab TableColumnBase Labeled)
        (javafx.scene Node Scene Parent)
        (javafx.scene.layout Pane VBox)
        (javafx.stage Stage)
        (javafx.collections FXCollections ObservableList)
        (javafx.css Styleable)
        (java.util Collection))

;; TODO Use pred-substitute for tc-assert?
(defn tc-assert [clazz :- Class value :- Any & [message :- String]]
  (try (assert (instance? clazz value))
       (catch AssertionError e (tc-ignore (error (if message message "") e)
                                          (error "Expected:" clazz "Actual:" (type value))
                                          (throw e)))))

(ann pred-substitute [Class -> (Fn [Any -> Boolean])])
(defn pred-substitute [clazz]
  (clojure.core.typed/pred* (quote clazz) 'clojurefx.clojurefx
                            (fn [arg] (boolean (instance? clazz arg)))))

(defn pred-protocol [proto check]
  (let [impls (keys (proto :impls))
        check (type check)]
    (reduce #(or %1 (isa? check %2)) false impls)))

;;## Shadows

(extend-protocol FXMeta
  clojure.lang.IObj
  (meta [this] (clojure.core/meta this))
  (with-meta [this metadata] (clojure.core/with-meta this metadata))
  Node
  (meta [this] (.getUserData ^Node this))
  (with-meta [this metadata] (.setUserData ^Node this metadata) this)
  MenuItem
  (meta [this] (.getUserData ^MenuItem this))
  (with-meta [this metadata] (.setUserData ^MenuItem this metadata) this))

;;## Standard

(tc-ignore
 (extend-protocol FXValue
   Labeled
   (get-value [this] (.getText ^Label this))
   (set-value! [this value] (tc-assert String value) (.setText ^Label this ^String value) this)
   TextField
   (get-value [this] (.getText ^TextField this))
   (set-value! [this value] (tc-assert String value) (.setText ^TextField this ^String value) this)
   TextArea
   (get-value [this] (.getText ^TextArea this))
   (set-value! [this value] (tc-assert String value) (.setText ^TextArea this ^String value) this)
   CheckBox
   (get-value [this] (.isSelected ^CheckBox this))
   (set-value! [this value] (tc-assert Boolean value) (.setSelected ^CheckBox this ^Boolean value) this)
   ComboBox
   (get-value [this] (let [selection-model (.getSelectionModel ^ComboBox this)
                           _ (assert (not (nil? selection-model)))
                           index (.getSelectedIndex ^javafx.scene.control.SingleSelectionModel selection-model)]
                       (if (>= index 0)
                         (nth (.getItems ^ComboBox this) index)
                         (.getSelectedItem ^javafx.scene.control.SingleSelectionModel selection-model))))
   (set-value! [this value] (let [sel-model (.getSelectionModel ^ComboBox this)
                                  item (first (filter #(= value %) (.getItems ^ComboBox this)))]
                              (if-not (nil? item)
                                (tc-ignore (.select ^javafx.scene.control.SingleSelectionModel sel-model item)))) this)
   Menu
   (get-value [this] (.getText ^Menu this))
   (set-value! [this value] (tc-assert String value) (.setText ^Menu this ^String value) this)
   MenuItem
   (get-value [this] (.getText ^MenuItem this))
   (set-value! [this value] (tc-assert String value) (.setText ^MenuItem this ^String value) this)))

(tc-ignore
 (extend-protocol FXId
   Styleable
   (get-id [this] (.getId ^Styleable this))
   (set-id! [this id] (tc-assert String id) (.setId ^Styleable this ^String id) this)))

(tc-ignore
 (extend-protocol FXParent
   Pane
   (get-subnodes [this] (.getChildren ^Pane this))
   (set-subnodes! [this nodes] (.setAll ^ObservableList (.getChildren ^Pane this) ^Collection nodes) this)
   TabPane
   (get-subnodes [this] (.getTabs ^TabPane this))
   (set-subnodes! [this nodes] (.setAll ^ObservableList (.getTabs ^TabPane this) ^Collection nodes) this)
   MenuBar
   (get-subnodes [this] (.getMenus ^MenuBar this))
   (set-subnodes! [this nodes] (.setAll ^ObservableList (.getMenus ^MenuBar this) ^Collection nodes) this)
   Menu
   (get-subnodes [this] (.getItems ^Menu this))
   (set-subnodes! [this nodes] (.setAll ^ObservableList (.getItems ^Menu this) ^Collection nodes) this)
   MenuButton
   (get-subnodes [this] (.getItems ^MenuButton this))
   (set-subnodes! [this nodes] (.setAll ^ObservableList (.getItems ^MenuButton this) ^Collection nodes) this)
   ContextMenu
   (get-subnodes [this] (.getItems ^ContextMenu this))
   (set-subnodes! [this nodes] (.setAll ^ObservableList (.getItems ^ContextMenu this) ^Collection nodes) this)
   ToolBar
   (get-subnodes [this] (.getItems ^ToolBar this))
   (set-subnodes! [this nodes] (.setAll ^ObservableList (.getItems ^ToolBar this) ^Collection nodes) this)
   SplitPane
   (get-subnodes [this] (.getItems ^SplitPane this))
   (set-subnodes! [this nodes] (.setAll ^ObservableList (.getItems ^SplitPane this) ^Collection nodes) this)
   Accordion
   (get-subnodes [this] (.getPanes ^Accordion this))
   (set-subnodes! [this nodes] (.setAll ^ObservableList (.getPanes ^Accordion this) ^Collection nodes) this)))

(tc-ignore
 (extend-protocol FXContainer
   Tab
   (get-content [this] (.getContent ^Tab this))
   (set-content! [this node] (.setContent ^Tab this ^Node node) this)
   TitledPane
   (get-content [this] (.getContent ^TitledPane this))
   (set-content! [this node] (.setContent ^TitledPane this ^Node node) this)
   ScrollPane
   (get-content [this] (.getContent ^ScrollPane this))
   (set-content! [this node] (.setContent ^ScrollPane this ^Node node) this)))

(tc-ignore
 (extend-protocol FXGraphic
   Labeled
   (get-graphic [this] (.getGraphic ^Labeled this))
   (set-graphic! [this graphic] (.setGraphic ^Labeled this ^Node graphic))
   MenuItem
   (get-graphic [this] (.getGraphic ^Menu this))
   (set-graphic! [this graphic] (.setGraphic ^Menu this ^Node graphic))))

(extend-protocol FXStyleSetter
  Node
  (set-style! [this style] (.setStyle ^Node this ^String style) this)
  MenuItem
  (set-style! [this style] (.setStyle ^MenuItem this ^String style) this))

(extend-type Styleable
  FXStyleable
  (get-css-meta [this] (.getCssMetaData ^Styleable this))
  (get-pseudo-class-styles [this] (.getPseudoClassStyles ^Styleable this))
  (get-style [this] (.getStyle ^Styleable this))
  (get-style-classes [this] (.getStyleClass ^Styleable this))
  (set-style-classes! [this classes] (.setAll ^ObservableList (.getStyleClass ^Styleable this) classes) this)
  (get-styleable-parent [this] (.getStyleableParent ^Styleable this))
  (get-type-selector [this] (.getTypeSelector ^Styleable this)))

;;## Special Types

(extend-type Stage
  FXStage
  (get-title [this] (.getTitle ^Stage this))
  (set-title! [this title] (.setTitle ^Stage this ^String title))
  (get-scene [this] (.getScene ^Stage this))
  (set-scene! [this scene] (.setScene ^Stage this ^Scene scene)))

(extend-type Scene
  FXScene
  (get-root [this] (.getRoot ^Scene this))
  (set-root! [this root] (.setRoot ^Scene this ^Parent root) this))

;;## IdMapper
(defn fxzipper [root]
  (zip/zipper (fn branch? [node]
                (or (pred-protocol FXParent node) (pred-protocol FXContainer node)))
              (fn children [node]
                (if (pred-protocol FXParent node)
                  (into [] (get-subnodes node))
                  [(get-content node)]))
              (fn make-node [node children]
                (if (pred-protocol FXParent node)
                  (set-subnodes! node children)
                  (set-content! node children)))
              root))

(defn get-node-by-id [graph id]
  (loop [zipper (fxzipper graph)]
    (cond (zip/end? zipper) nil
          (= (get-id (zip/node zipper)) (name id)) (zip/node zipper)
          :else (recur (zip/next zipper)))))

(defn get-id-map [graph]
  (loop [zipper (fxzipper graph)
         ids {}]
    (if (zip/end? zipper)
      ids
      (recur (zip/next zipper)
             (assoc ids (keyword (get-id (zip/node zipper))) (zip/node zipper))))))
