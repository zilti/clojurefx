1(ns clojurefx.clojurefx
   (:refer-clojure :exclude [atom doseq let fn defn ref dotimes defprotocol loop for send meta with-meta])
   (:require [clojure.core.typed :refer :all]
             [clojure.core.typed.unsafe :refer [ignore-with-unchecked-cast]]
             [taoensso.timbre :as timbre]
             [clojure.java.io :as io]
             [clojure.zip :as zip]
             [clojurefx.protocols :as p]
             [clojure.java.io :refer :all]))

(defonce force-toolkit-init (javafx.embed.swing.JFXPanel.))

;; ## Threading helpers

(ann run-later* [(Fn [-> Any]) -> nil])
  (defn run-later*"
  Simple wrapper for Platform/runLater. You should use run-later.
" [f]
(tc-ignore (assert (instance? Runnable f))
           (javafx.application.Platform/runLater f))
nil)

(defmacro run-later [& body]
  `(run-later* (fn [] ~@body)))

(ann run-now* (All [x] [[-> x] -> x]))
  (defn run-now* "
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
                              TitledPane TabPane Tab TableColumnBase Labeled ButtonBase)
        (javafx.scene Node Scene Parent)
        (javafx.scene.layout Pane VBox)
        (javafx.stage Stage)
        (javafx.collections FXCollections ObservableList)
        (javafx.css Styleable)
        (javafx.event Event ActionEvent EventTarget)
        (java.util Collection))

(defn tc-assert [clazz :- Class value :- Any & [message :- String]]
  (try (assert (instance? clazz value))
       (catch AssertionError e (tc-ignore (error (if message message "") e)
                                          (error "Expected:" clazz "Actual:" (type value))
                                          (throw e)))))

(defn pred-protocol [proto :- (HMap :mandatory {:impls (Map Keyword Class)}) check :- Any] :- Boolean
  (let [impls (keys (proto :impls))
        check (type check)]
    (reduce #(or %1 (isa? check %2)) false impls)))

;;## Shadows

(tc-ignore
 (extend-protocol p/FXMeta
   clojure.lang.IObj
   (meta [this] (clojure.core/meta this))
   (with-meta [this metadata] (clojure.core/with-meta this metadata))
   Node
   (meta [this] (.getUserData ^Node this))
   (with-meta [this metadata] (.setUserData ^Node this metadata) this)
   MenuItem
   (meta [this] (.getUserData ^MenuItem this))
   (with-meta [this metadata] (.setUserData ^MenuItem this metadata) this)))

;;## Standard

(tc-ignore
 (extend-protocol p/FXValue
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
 (extend-protocol p/FXId
   Styleable
   (get-id [this] (.getId ^Styleable this))
   (set-id! [this id] (tc-assert String id) (.setId ^Styleable this ^String id) this)))

(tc-ignore
 (extend-protocol p/FXParent
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
 (extend-protocol p/FXContainer
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
 (extend-protocol p/FXGraphic
   Labeled
   (get-graphic [this] (.getGraphic ^Labeled this))
   (set-graphic! [this graphic] (.setGraphic ^Labeled this ^Node graphic))
   MenuItem
   (get-graphic [this] (.getGraphic ^Menu this))
   (set-graphic! [this graphic] (.setGraphic ^Menu this ^Node graphic))))

(tc-ignore
 (extend-protocol p/FXStyleSetter
   Node
   (set-style! [this style] (.setStyle ^Node this ^String style) this)
   MenuItem
   (set-style! [this style] (.setStyle ^MenuItem this ^String style) this)))

(tc-ignore
 (extend-type Styleable
   p/FXStyleable
   (get-css-meta [this] (.getCssMetaData ^Styleable this))
   (get-pseudo-class-styles [this] (.getPseudoClassStyles ^Styleable this))
   (get-style [this] (.getStyle ^Styleable this))
   (get-style-classes [this] (.getStyleClass ^Styleable this))
   (set-style-classes! [this classes] (.setAll ^ObservableList (.getStyleClass ^Styleable this) classes) this)
   (get-styleable-parent [this] (.getStyleableParent ^Styleable this))
   (get-type-selector [this] (.getTypeSelector ^Styleable this))))

(declare bind-event)
(tc-ignore
 (extend-protocol p/FXOnAction
   ButtonBase
   (get-action [this] (.getOnAction ^ButtonBase this))
   (set-action! [this action] (.setOnAction ^ButtonBase this (bind-event action)) this)
   (fire! [this] (.fire this))
   MenuItem
   (get-action [this] (.getOnAction ^MenuItem this))
   (set-action! [this action] (.setOnAction ^ButtonBase this (bind-event action)) this)
   (fire! [this] (.fire this))))

;;## Special Types

;;### javafx.event

(tc-ignore
 (extend-type Event
   p/FXEvent
   (source [this] (.getSource ^Event this))
   (consume! [this] (.consume ^Event this) this)
   (copy [this new-src new-target] (.copy ^Event this new-src new-target))
   (event-type [this] (.getEventType this))
   (target [this] (.getTarget this))
   (consumed? [this] (.isConsumed this))))

;;### javafx.stage

(tc-ignore
 (extend-type Stage
   p/FXStage
   (get-title [this] (.getTitle ^Stage this))
   (set-title! [this title] (.setTitle ^Stage this ^String title))
   (get-scene [this] (.getScene ^Stage this))
   (set-scene! [this scene] (.setScene ^Stage this ^Scene scene))))

;;### javafx.scene

(tc-ignore
 (extend-type Scene
   p/FXScene
   (get-root [this] (.getRoot ^Scene this))
   (set-root! [this root] (.setRoot ^Scene this ^Parent root) this)))

;;## Event handling helper
(tc-ignore
 (defn bind-event
   [handler :- (All [[A :variance :covariant :< Event]] (Fn [A -> Any]))] :- javafx.event.EventHandler
   (reify javafx.event.EventHandler
     (handle [_ event] (handler event)))))

;;## IdMapper
(defn fxzipper [root]
  (zip/zipper (fn branch? [node :- Any] :- Boolean
                (or (pred-protocol p/FXParent node) (pred-protocol p/FXContainer node)))
              (fn children [node :- (U p/FXParent p/FXContainer)] :- java.util.List
                (if (pred-protocol p/FXParent node)
                  (into [] (p/get-subnodes node))
                  [(p/get-content node)]))
              (fn make-node [node :- (U p/FXParent p/FXContainer) children :- Any] :- (U p/FXParent p/FXContainer)
                (if (pred-protocol p/FXParent node)
                  (p/set-subnodes! node children)
                  (p/set-content! node children)))
              root))

(tc-ignore
 (defn get-node-by-id [graph id]
   (loop [zipper (fxzipper graph)]
     (cond (zip/end? zipper) nil
           (= (p/get-id (zip/node zipper)) (name id)) (zip/node zipper)
           :else (recur (zip/next zipper))))))

(tc-ignore
 (defn get-id-map [graph]
   (loop [zipper (fxzipper graph)
          ids {}]
     (if (zip/end? zipper)
       ids
       (recur (zip/next zipper)
              (assoc ids (keyword (p/get-id (zip/node zipper))) (zip/node zipper)))))))
