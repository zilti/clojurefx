(ns clojurefx.clojurefx
  (:require [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [clojure.zip :as zip]
            [clojure.reflect :as reflect]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [clojure.pprint :refer :all])
  (:import (javafx.scene.layout Region)
           (javafx.scene.shape Rectangle)
           (clojurefx.ApplicationInitializer)
           (java.lang.reflect Method)
           (javafx.scene.control Button)))

(timbre/refer-timbre)

;; ## Specs
(s/def ::node (partial instance? javafx.scene.Node))
(s/def ::scene (partial instance? javafx.scene.Scene))
(s/def ::stage (partial instance? javafx.stage.Stage))
(s/def ::scenegraph (s/or :node ::node
                          :scene ::scene
                          :stage ::stage))
(s/def ::args (s/coll-of symbol?))
(s/def ::code (s/coll-of any?))

;; ## Functional interfaces

(defn fi* [interface args code]
  (debug "fi*")
  (let [iface-ref (reflect/type-reflect interface)
        methods (filter #(instance? clojure.reflect.Method %) (:members iface-ref))
        functional-method (filter (fn [x] (some #(= % :abstract) (:flags x))) methods)
        method-sym (:name (first functional-method))]
    (eval `(proxy [~interface] []
             (~method-sym ~args
               ~@code)))))

(defmacro fi
  "This macro is used to make use of functional interfaces. The class name of the functional interface has to be given."
  [interface args & code]
  (debug "fi")
  ;;`(fi* ~interface '~args '~code)
  (let [iface-ref (reflect/type-reflect interface)
        methods (filter #(instance? clojure.reflect.Method %) (:members iface-ref))
        functional-method (filter (fn [x] (some #(= % :abstract) (:flags x))) methods)
        method-sym (:name (first functional-method))]
    (when-not (= (count functional-method) 1)
      (throw (new Exception (str "can't take an interface with more than one method:" (pr-str functional-method)))))
    (debug "Writing proxy.")
    `(proxy [~interface] []
       (~method-sym ~args
         ~@code)))
  )

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

(declare camelcase-low)

(def instancecache (atom {}))
(defmacro objwrap [obj sym]
  (do
    (swap! instancecache assoc sym obj)
    `(let [obj# (get @instancecache ~sym)]
       (debug "Calling back object" obj# "with hash" ~sym)
       (swap! instancecache dissoc ~sym)
       obj#)))

(defmacro connect
  [instance method args & code]
  `(let [instance# ~instance
         dbg# (debug "instance#" instance#)
         method# '~(if (instance? clojure.lang.Cons method) (second method) method)
         dbg# (debug "method#" method#)
         functional-method# (first (clojure.lang.Reflector/getMethods (class instance#) 1 (camelcase-low (name method#)) false))
         dbg# (debug "functional-method#" functional-method#)
         functional-para# (symbol (.getName (first (.getParameterTypes ^Method functional-method#))))
         dbg# (debug "functional-para#" functional-para#)
         code# '~(if (= 1 (count code)) (first code) code)
         dbg# (debug "code#" code#)]
     (eval `(~(symbol (str "." (camelcase-low (name method#)))) ~`(objwrap ~instance# ~(hash instance#))
              (fi ~functional-para# ~'~args '~@code#))))
  )

(defn start-app [app-init app-start app-stop]
  (clojurefx.ApplicationInitializer/initApp app-init app-start app-stop))

;; ## Data

(def constructor-args
  (atom {javafx.scene.Scene {:root javafx.scene.Parent}
         javafx.stage.Stage {:style javafx.stage.StageStyle}}))

(defn camelcase [kebabcase]
  (let [splitted (str/split (name kebabcase) #"-")]
    (reduce #(str %1 (str/capitalize %2)) "" splitted)))

(defn camelcase-low [kebabcase]
  (let [splitted (str/split (name kebabcase) #"-")]
    (reduce #(str %1 (str/capitalize %2)) (first splitted) (rest splitted))))

;; ## Threading helpers

(defn run-later* "
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
  (clojure.lang.Reflector/invokeConstructor clazz (into-array args)))

;; ## Scene graph walker
(defn- has-method? [node method]
  (and (not (nil? node)) (not (empty? (clojure.lang.Reflector/getMethods (class node) 0 method false)))))

(defn- graph-node-has-children? [node]
  (or (has-method? node "getChildren")
      (has-method? node "getGraphic")
      (has-method? node "getMenus")
      (has-method? node "getColumns")
      (has-method? node "getContent")
      (has-method? node "getTabs")
      (has-method? node "getItems")
      (has-method? node "getRoot")
      (has-method? node "getScene"))
  )

(defn- graph-node-get-children [node]
  {:post [coll?]}
  (cond (has-method? node "getChildren") (.getChildren node)
        (has-method? node "getGraphic") [(.getGraphic node)]
        (has-method? node "getMenus") (.getMenus node)
        (has-method? node "getContent") [(.getContent node)]
        (has-method? node "getTabs") (.getTabs node)
        (has-method? node "getColumns") (.getColumns node)
        (has-method? node "getItems") (.getItems node)
        (has-method? node "getRoot") [(.getRoot node)]
        (has-method? node "getScene") [(.getScene node)])
  )

;; (def struct (compile [Scene {:root [VBox {:children [Label {:text "Hi!" :style-class ["test"]}]}]}]))

(defn scenegraph-zipper [node]
  (zip/zipper graph-node-has-children? graph-node-get-children nil node))

(defn- flat-zipper [zipper]
  (if (or (zip/end? (zip/next zipper)) (nil? (zip/next zipper)))
    (zip/node zipper)
    (lazy-seq (cons (zip/node zipper) (flat-zipper (zip/next zipper))))))

(defn- has-id? [node id]
  {:pre  [any? (string? id)]
   :post [boolean?]}
  (if (s/valid? ::node node)
    (= id (.getId node))
    false))

(defn find-child-by-id [node id]
  {:pre  [(s/valid? ::scenegraph node)
          (string? id)]
   :post [#(or (s/valid? ::scenegraph node) nil?)]}
  (let [zipper (scenegraph-zipper node)]
    (first (filter #(has-id? % id) (flat-zipper zipper)))))

(defn- contains-class? [node clazz]
  {:pre  [(s/valid? ::scenegraph node) (string? clazz)]
   :post [boolean?]}
  (if (s/valid? ::node node)
    (< 0 (count (filter #(= % clazz) (.getStyleClass node))))
    false))

(defn find-child-by-class [node clazz]
  {:pre  [(s/valid? ::scenegraph node)
          (string? clazz)]
   :post [#(or (s/valid? ::scenegraph node) nil?)]}
  (let [zipper (scenegraph-zipper node)]
    (filter #(contains-class? % clazz) (flat-zipper zipper))))

;; ## Properties

(defn find-property [obj prop]
  (clojure.lang.Reflector/invokeInstanceMethod obj (str (camelcase prop) "Property") (into-array [])))

(defn get-property-value
  ([obj prop]
   (clojure.lang.Reflector/invokeInstanceMethod obj (str "get" (camelcase prop)) (into-array []))))

(defn set-property-value
  ([obj prop val]
   (clojure.lang.Reflector/invokeInstanceMethod obj (str "set" (camelcase prop)) (into-array [val]))))

;; ## In-code scenegraph

(declare compile-o-matic)

(defn- apply-connects [nodeobj [[method args & code] & conncoll]]
  {:pre [(s/valid? ::node nodeobj)
         (symbol? method)
         (s/valid? ::args args)
         (s/valid? ::code code)]}
  (eval `(connect ~`(objwrap ~nodeobj ~(hash nodeobj)) '~method '~args ~code))
  (debug "Heyy!")
  (if (not (or (nil? conncoll) (empty? conncoll)))
    (recur nodeobj conncoll)))

(defn- apply-props-to-node [nodeobj propmap]
  (doseq [[k v] propmap]
    (cond
      (= k :children) (.add (.getChildren nodeobj) (compile-o-matic v))
      (= k :style-class) (.addAll (.getStyleClass nodeobj) (compile-o-matic v))
      (= k :connect) (apply-connects nodeobj v)
      :else (set-property-value nodeobj k (compile-o-matic v)))
    )
  nodeobj)

(defn- propmap-splitter [clazz propmap]
  (let [constructor-args (keys (get @constructor-args clazz))]
    [(map propmap constructor-args) (apply dissoc propmap constructor-args)]))

(defn- build-node [clazz propmap]
  (let [[cargs props] (propmap-splitter clazz propmap)
        nodeobj (invoke-constructor clazz (map compile-o-matic cargs))]
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
