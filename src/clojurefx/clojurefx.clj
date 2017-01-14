(ns clojurefx.clojurefx
  (:require [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [clojure.zip :as zip]
            [clojure.reflect :as reflect]
            [clojure.string :as str]
            [swiss.arrows :refer :all])
  (:import (javafx.scene.layout Region)
           (javafx.scene.shape Rectangle)))

;; Fuck you, whoever made that API design.
(defonce force-toolkit-init (javafx.embed.swing.JFXPanel.))

(timbre/refer-timbre)

;; (import '(clojurefx AppWrap)
;;         '(javafx.scene.control Labeled Label TextField TextArea CheckBox ComboBox Menu MenuItem MenuBar
;;                                MenuButton ContextMenu ToolBar SplitPane ScrollPane Accordion
;;                                TitledPane TabPane Tab TableColumnBase Labeled ButtonBase)
;;         '(javafx.scene Node Scene Parent)
;;         '(javafx.scene.layout Pane VBox)
;;         '(javafx.stage Stage)
;;         '(javafx.collections FXCollections ObservableList)
;;         '(javafx.css Styleable)
;;         '(javafx.event Event ActionEvent EventTarget)
;;         '(java.util Collection))

;; ## Data

(def constructor-args
  (atom {javafx.scene.Scene {:root javafx.scene.Parent}
         javafx.stage.Stage {:style javafx.stage.StageStyle}}))

(defn camelcase [kebabcase]
  (let [splitted (str/split kebabcase #"-")]
    (reduce #(str %1 (str/capitalize %2)) (first splitted) (rest splitted))))

;; ## Threading helpers

(defn run-later*"
  Simple wrapper for Platform/runLater. You should use run-later.
  " [f]
  (assert (instance? Runnable f))
  (javafx.application.Platform/runLater f)
  nil)

(defmacro run-later [& body]
  `(run-later* (fn [] ~@body)))

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

(defn collize "
  Turns the input into a collection, if it isn't already.
  " [input]
  (if (coll? input)
    input
    (list input)))

(defn pred-protocol [proto check]
  (let [impls (keys (proto :impls))
        check (type check)]
    (reduce #(or %1 (isa? check %2)) false impls)))

;; ## FXMLLoader

(defn load-fxml [filename]
  (let [loader (new javafx.fxml.FXMLLoader)]
    (.setLocation loader (io/resource ""))
    (.load loader (-> filename io/resource io/input-stream))))

;; ## Constructors

(defn find-constructor [clazz cargs]
  (->> (reflect/reflect clazz)
       :members
       (filter #(= clojure.reflect.Constructor (class %)))
       (filter #(= cargs (:parameter-types %)))
       first))

(defn invoke-constructor [clazz args]
  (clojure.lang.Reflector/invokeConstructor clazz (into-array args)))

;; ## Properties

(defn find-property [obj prop]
  (clojure.lang.Reflector/invokeInstanceMethod obj (str (camelcase prop) "Property") []))

(defn get-property-value
  ([obj prop]
   (.getValue (find-property obj (name prop)))))

(defn set-property-value
  ([obj prop val]
   (.setValue (find-property obj (name prop)) val)))

;; ## In-code scenegraph

(declare compile-o-matic)
(defn- apply-props-to-node [nodeobj propmap]
  (doseq [[k v] propmap]
    (set-property-value nodeobj k v))
  nodeobj)

(defn- propmap-splitter [clazz propmap]
  (let [constructor-args (get @constructor-args clazz)] 
    [(map propmap constructor-args) (apply dissoc propmap constructor-args)]))

(defn- build-node [clazz propmap]
  (let [[cargs props] (propmap-splitter clazz propmap)
        nodeobj (invoke-constructor clazz cargs)]
    (apply-props-to-node nodeobj props)
    nodeobj))

(defn compile
  ([args] (run-now (compile args [])))
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

;;## Event handling helper
(defn bind-event
  [handler]
  (reify javafx.event.EventHandler
    (handle [_ event] (handler event))))
