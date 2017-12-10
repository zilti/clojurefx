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

;; ## Scenegraph

(defmacro fi
  [interface args & code]
  (debug "interface:" interface) 
  (let [iface-ref (reflect/type-reflect interface)
        bogus (debug "iface-ref:" iface-ref)
        methods (filter #(instance? clojure.reflect.Method %) (:members iface-ref))
        functional-method (filter (fn [x] (some #(= % :abstract) (:flags x))) methods)
        bogus (debug "methods:" (pr-str functional-method))
        method-sym (:name (first functional-method))]
    (debug "method-sym:" method-sym)

    (when-not (= (count functional-method) 1)
      (throw (new Exception (str "can't take an interface with more than one method:" (pr-str functional-method)))))

    (debug (pr-str `(proxy [~interface] []
                      (~method-sym ~args ~@code))))
    
    `(proxy [~interface] []
       (~method-sym ~args
        ~@code))))

(defmacro handle [obj prop fun]
  (let [argument (->> fun (drop 1) first)
        code (drop 2 fun)]
    `(.setValue (~(symbol (str (name obj) "/" (name prop)))) (fi javafx.event.ActionEvent ~argument ~@code))))

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
