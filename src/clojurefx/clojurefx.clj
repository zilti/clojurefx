(ns clojurefx.clojurefx
  (:require [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [clojure.zip :as zip]
            [clojure.reflect :as reflect]
            [clojure.string :as str]
            [swiss.arrows :refer :all])
  (:import (javafx.scene.layout Region)
           (javafx.scene.shape Rectangle)))

(timbre/refer-timbre)

;; (defonce force-toolkit-init (javafx.embed.swing.JFXPanel.))

;; ## Scenegraph

(defmacro fi
  [interface args & code]
  (debug "interface:" interface) 
  (let [iface-ref (reflect/type-reflect interface)
        bogus (debug "iface-ref:" iface-ref)
        methods (filter #(instance? clojure.reflect.Method %) (:members iface-ref))
        bogus (debug "methods:" (pr-str methods))
        method-sym (:name (first methods))]
    (debug "method-sym:" method-sym)

    (when-not (= (count methods) 1) 
      (throw (new Exception (str "can't take an interface with more than one method:" (pr-str methods)))))

    (debug (pr-str `(proxy [~interface] []
                      (~method-sym ~args ~@code))))
    
    `(proxy [~interface] []
       (~method-sym ~args
        ~@code))))

(defmacro handle [obj prop fun]
  (let [argument (->> fun (drop 1) first)
        code (drop 2 fun)]
    `(.setValue (~(symbol (str (name obj) "/" (name prop)))) (fi javafx.event.ActionEvent ~argument ~@code))))

;; (defn branch? [obj]
;;   (or (and (instance? javafx.scene.Parent obj)
;;            (not (instance? org.controlsfx.control.StatusBar obj)))
;;       (instance? javafx.scene.control.MenuBar obj)
;;       (instance? javafx.scene.control.Menu obj)))

;; (defn make-node [node children]
;;   nil)

;; (defn down [x]
;;   (cond
;;     (instance? javafx.scene.control.Label x) (.getGraphic x)
;;     (instance? javafx.scene.control.ProgressIndicator x) (.getContextMenu x)
;;     (instance? javafx.scene.control.ScrollPane x) (.getContent x)
;;     (instance? javafx.scene.control.MenuBar x) (.getMenus x)
;;     (instance? javafx.scene.control.Menu x) (.getItems x)
;;     (instance? javafx.scene.Parent x) (.getChildren x)
;;     :else nil))

;; (defn sgzipper [root]
;;   (zip/zipper branch? down make-node root))

;; (defn by-id [root id] 
;;   (try
;;     (cond 
;;       (not (instance? clojure.lang.IFn root)) (do (trace "Raw input confirmed. Starting.")
;;                                                   (by-id (sgzipper root) id)) 
;;       (zip/end? root) (do (trace "Search ended without result.")
;;                           nil)
;;       (nil? (zip/node root)) (by-id (zip/next root) id)
;;       (= id (.getId (zip/node root))) (do (debug "Found item:" (zip/node root))
;;                                           (zip/node root))
;;       :else (do (trace "id of" (zip/node root) "does not match, proceeding to" (zip/node (zip/next root)))
;;                 (by-id (zip/next root) id)))
;;     (catch Exception e (error e))))

;; ## Data

(def constructor-args
  (atom {javafx.scene.Scene {:root javafx.scene.Parent}
         javafx.stage.Stage {:style javafx.stage.StageStyle}}))

(defn camelcase [kebabcase]
  (let [splitted (str/split (name kebabcase) #"-")]
    (reduce #(str %1 (str/capitalize %2)) "" splitted)))

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

;; ## Constructors

(defn find-constructor [clazz cargs]
  (->> (reflect/reflect clazz)
       :members
       (filter #(= clojure.reflect.Constructor (class %)))
       (filter #(= cargs (:parameter-types %)))
       first))

(defn invoke-constructor [clazz args]
  (info "Constructing" clazz "with" (first args))
  (clojure.lang.Reflector/invokeConstructor clazz (into-array args)))

;; ## Properties

(defn find-property [obj prop]
  (info obj prop)
  (clojure.lang.Reflector/invokeInstanceMethod obj (str (camelcase prop) "Property") (into-array [])))

(defn get-property-value
  ([obj prop]
   (clojure.lang.Reflector/invokeInstanceMethod obj (str "get" (camelcase prop)) (into-array []))))

(defn set-property-value
  ([obj prop val]
   (info obj ": Setting property" prop "to" val)
   (clojure.lang.Reflector/invokeInstanceMethod obj (str "set" (camelcase prop)) (into-array [val]))))

;; ## In-code scenegraph

(declare compile-o-matic)

(defn- apply-props-to-node [nodeobj propmap]
  (doseq [[k v] propmap]
    (case k
      :children (.add (.getChildren nodeobj) (compile-o-matic v))
      (set-property-value nodeobj k (compile-o-matic v))))
  nodeobj)

(defn- propmap-splitter [clazz propmap]
  (let [constructor-args (keys (get @constructor-args clazz))]
    (info "Constructor args for" clazz "are" constructor-args)
    [(map propmap constructor-args) (apply dissoc propmap constructor-args)]))

(defn- build-node [clazz propmap]
  (let [[cargs props] (propmap-splitter clazz propmap)
        nodeobj (invoke-constructor clazz (map compile-o-matic cargs))]
    (info cargs " " props)
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
