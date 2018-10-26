(ns clojurefx.clojurefx
  (:require [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [clojure.zip :as zip]
            [clojure.reflect :as reflect]
            [clojure.string :as str]
            [swiss.arrows :refer :all]
            [clojure.spec.alpha :as s])
  (:import (javafx.scene.layout Region)
           (javafx.scene.shape Rectangle)
           (clojurefx.ApplicationInitializer)))

(timbre/refer-timbre)

;; ## Specs
(s/def ::node (fn [x] (or (instance? javafx.scene.Node x) (instance? javafx.scene.Scene x))))

;; ## Functional interfaces

(defmacro fi
  "This macro is used to make use of functional interfaces. The class name of the functional interface has to be given."
  [interface args & code]
  (let [iface-ref (reflect/type-reflect interface) 
        methods (filter #(instance? clojure.reflect.Method %) (:members iface-ref))
        functional-method (filter (fn [x] (some #(= % :abstract) (:flags x))) methods) 
        method-sym (:name (first functional-method))]
    (debug "method-sym:" method-sym)

    (when-not (= (count functional-method) 1)
      (throw (new Exception (str "can't take an interface with more than one method:" (pr-str functional-method)))))

    (debug (pr-str `(proxy [~interface] []
                      (~method-sym ~args ~@code))))
    
    `(proxy [~interface] []
       (~method-sym ~args
        ~@code))))

(defn- map2
  "Like map, but takes two elements at a time."
  ([fun a b] (list (fun a b)))
  ([fun [a b & coll]]
    (cons (fun a b) (map2 fun coll))))

(defn typematcher
  [arg-types methods]
  (let [method (first methods)]
    (cond (or (nil? method) (empty? method)) nil

          (and (= (count arg-types) (count (:parameter-types method)))
               (every? #(isa? (first %) (second %)) (interleave arg-types (:parameter-types method))))
          method

          :else
          (recur arg-types (rest methods)))))

(defmacro connect
  "This macro is used to make use of functional interfaces. The args list has to be provided with the arg types, like in Java: [Type name1 Type name2]."
  [instance args & code]
  (let [class-ref (reflect/type-reflect (class instance))
        ifaces (flatten (map reflect/type-reflect (into #{} (:bases class-ref))))
        methods (filter #(instance? clojure.reflect.Method %) (:members ifaces))
        functional-methods (filter (fn [x] (some #(= % :abstract) (:flags x))) methods)
        arg-types (map2 (fn [a _] a) args)
        method (typematcher arg-types functional-methods)]
    (debug "method-sym:" (:name method))

    `(proxy [~(:declaring-class method)] []
       (~(:name method) ~(map2 (fn [_ b] b) args)
         ~@code))
    ))

(defmacro handle
  ""
  [obj prop fun]
  (let [argument (->> fun (drop 1) first)
        code (drop 2 fun)]
    `(.setValue (~(symbol (str (name obj) "/" (name prop)))) (fi javafx.event.ActionEvent ~argument ~@code))))

(defn start-app [app-init app-start app-stop]
  (clojurefx.ApplicationInitializer/initApp app-init app-start app-stop))

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

;; ## Scene graph walker
(defn- has-method? [node method]
  (not (empty? (clojure.lang.Reflector/getMethods (class node) 0 method false))))

(defn- graph-node-has-children? [node]
  {:pre [(s/valid? ::node node)]
   :post [boolean?]}
  (or (has-method? node "getChildren")
      (has-method? node "getGraphic")
      (has-method? node "getMenus")
      (has-method? node "getColumns")
      (has-method? node "getContent")
      (has-method? node "getTabs")
      (has-method? node "getItems")
      (has-method? node "getRoot"))
  )

(defn- graph-node-get-children [node]
  {:pre [(s/valid? ::node node)]
   :post [coll?]}
  (debug "Getting children from" node)
  (cond (has-method? node "getChildren") (.getChildren node)
        (has-method? node "getGraphic")  [(.getGraphic node)]
        (has-method? node "getMenus")    (.getMenus node)
        (has-method? node "getContent")  [(.getContent node)]
        (has-method? node "getTabs")     (.getTabs node)
        (has-method? node "getColumns")  (.getColumns node)
        (has-method? node "getItems")    (.getItems node)
        (has-method? node "getRoot")     [(.getRoot node)])
  )

;; (def struct (compile [Scene {:root [VBox {:children [Label {:text "Hi!" :style-class ["test"]}]}]}]))

(defn scenegraph-zipper [node]
  (zip/zipper graph-node-has-children? graph-node-get-children nil node))

(defn- flat-zipper [zipper]
  (if (or (zip/end? (zip/next zipper)) (nil? (zip/next zipper)))
    (zip/node zipper)
    (lazy-seq (cons (zip/node zipper) (flat-zipper (zip/next zipper))))))

(defn find-child-by-id [node id]
  {:pre [(s/valid? ::node node)
         (string? id)]
   :post [#(or (s/valid? ::node node) nil?)]}
  (let [zipper (scenegraph-zipper node)]
    (filter #(= id (.getId %)) (flat-zipper zipper))))

(defn- contains-class? [node clazz]
  {:pre [(s/valid? ::node node) (string? clazz)]
   :post [boolean?]}
  (debug "NODETEST:" node)
  (s/valid? ::node node)
  (if (instance? javafx.scene.Scene node)
    false
    (> 0 (count (filter #(= % clazz) (.getStyleClass node))))))

(defn find-child-by-class [node clazz]
  {:pre [(s/valid? ::node node)
         (string? clazz)]
   :post [#(or (s/valid? ::node node) nil?)]}
  (debug "NODE:" node)
  (let [zipper (scenegraph-zipper node)]
    (debug "ZIPPER:" zipper)
    (filter #(contains-class? % clazz) (flat-zipper zipper))))

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
      :style-class (.addAll (.getStyleClass nodeobj) (compile-o-matic v))
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
