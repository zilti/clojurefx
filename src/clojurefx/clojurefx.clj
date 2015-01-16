(ns clojurefx.clojurefx
  (:refer-clojure :exclude [atom doseq let fn defn ref dotimes defprotocol loop for send compile])
  (:require [clojure.core.typed :refer :all]
            [clojure.core.typed.unsafe :refer [ignore-with-unchecked-cast]]
            [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [clojurefx.protocols :refer :all]))

(defonce force-toolkit-init (javafx.embed.swing.JFXPanel.))
(tc-ignore (timbre/refer-timbre))

(import (javafx.scene.control Label TextField TextArea CheckBox ComboBox Menu MenuItem MenuBar
                              MenuButton ContextMenu ToolBar SplitPane ScrollPane Accordion
                              TitledPane TabPane Tab TableColumnBase Labeled)
        (javafx.scene Node)
        (javafx.scene.layout Pane VBox)
        (javafx.collections FXCollections ObservableList)
        (java.util Collection))

;; TODO This belongs elsewhere.
(tc-ignore
 (defn load-fxml [filename]
   (.load (javafx.fxml.FXMLLoader.) (-> filename io/resource io/input-stream))))

;; TODO Use pred-substitute for tc-assert
(defn tc-assert [clazz :- Class value :- Any & [message :- String]]
  (try (assert (instance? clazz value))
       (catch AssertionError e (tc-ignore (error (if message message "") e)
                                          (error "Expected:" clazz "Actual:" (type value))
                                          (throw e)))))

(ann pred-substitute [Class -> (Fn [Any -> Boolean])])
(defn pred-substitute [clazz]
  (clojure.core.typed/pred* (quote clazz) 'clojurefx.clojurefx
                            (fn [arg] (boolean (instance? clazz arg)))))

(tc-ignore
 (extend-protocol FXValue
   Label
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
   Node
   (get-id [this] (.getId ^Node this))
   (set-id! [this id] (tc-assert String id) (.setId ^Node this ^String id) this)
   Tab
   (get-id [this] (.getId ^Tab this))
   (set-id! [this id] (tc-assert String id) (.setId ^Tab this ^String id) this)
   TableColumnBase
   (get-id [this] (.getId ^TableColumnBase this))
   (set-id! [this id] (tc-assert String id) (.setId ^TableColumnBase this ^String id) this)
   MenuItem
   (get-id [this] (.getId ^MenuItem this))
   (set-id! [this id] (tc-assert String id) (.setId ^MenuItem this ^String id) this)))

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


;; TODO Code below probably also belongs somewhere else
(def getter first)
(def setter second)

(def translation-map
  (atom {:text (with-meta [#'get-value #'set-value!] {:argument String :parent FXValue})
         :value (with-meta [#'get-value #'set-value!] {:argument Object :parent FXValue})
         :id (with-meta [#'get-id #'set-id!] {:argument String :parent FXId})
         :graphic (with-meta [#'get-graphic #'set-graphic!] {:argument Node :parent FXGraphic})
         :content (with-meta [#'get-content #'set-content!] {:argument Node :parent FXContainer})
         :children (with-meta [#'get-subnodes #'set-subnodes!] {:argument java.util.List :parent FXParent})}))

(declare compile-o-matic)
(ann build-node [Any (Map Keyword Any) -> Any])
(defn build-node [object props]
  (debug "build-node:" object props)
  (let [obj (eval `(new ~object))]
    (doseq [[k v] props]
      (let [translation (get @translation-map k)
            {:keys [argument parent]} (meta translation)
            v (compile-o-matic v)]
        (trace "Key:" k " " (type k) "Value:" v " " (type v))
        (when (nil? translation)
          (throw (Exception. (str "Property" k "not available in translation map."))))
        ;; (when-not ((pred-substitute argument) v)
        ;;   (throw (Exception. (str "Input type" v "is not compatible with expected type for" k))))
        ;; (when-not ((pred-substitute parent) obj)
        ;;   (throw (Exception. (str "Property" k "not available for class" (class obj)))))
        ((setter translation) obj v)))
    obj))

(ann resolv-o-matic [(U String Keyword Symbol Class) -> Class])
(defn resolv-o-matic [thing]
  (cond
    (symbol? thing) (ns-resolve (the-ns 'clojurefx.clojurefx) thing)
    (keyword? thing) (recur (name thing))
    (string? thing) (recur (symbol thing))
    :else thing))

(ann compile [(Vec Any) -> Any])
(defn compile [[obj params & other]]
  (assert (map? params))
  (let [obj (build-node (resolv-o-matic obj) params)]
    (if (empty? other)
      obj
      (flatten (conj (list obj) (compile other))))))

(ann compile-o-matic [Any -> Any])
(defn compile-o-matic [thing]
  (if (instance? java.util.List thing)
    (if (and (not (coll? (first thing))) (map? (second thing)))
      (compile thing)
      thing)
    thing))
