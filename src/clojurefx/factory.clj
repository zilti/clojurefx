(ns clojurefx.factory
  (:refer-clojure :exclude [atom doseq let fn defn ref dotimes defprotocol loop for send compile])
  (:require [clojure.core.typed :refer :all]
            [clojure.core.typed.unsafe :refer [ignore-with-unckecked-cast]]
            [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [clojurefx.clojurefx :refer :all]
            [clojurefx.protocols :refer :all]))

(tc-ignore (timbre/refer-timbre))

;;## FXMLLoader

(defn load-fxml [filename :- String] :- javafx.scene.Node
  (let [loader (new javafx.fxml.FXMLLoader)]
    (.setLocation loader (io/resource ""))
    (.load loader (-> filename io/resource io/input-stream))))

;;## VectorBuilder

(def getter first)
(def setter second)

(def translation-map
  (atom {:text (with-meta [#'get-value #'set-value!] {:argument String :parent FXValue})
         :value (with-meta [#'get-value #'set-value!] {:argument Object :parent FXValue})
         :id (with-meta [#'get-id #'set-id!] {:argument String :parent FXId})
         :graphic (with-meta [#'get-graphic #'set-graphic!] {:argument Node :parent FXGraphic})
         :content (with-meta [#'get-content #'set-content!] {:argument Node :parent FXContainer})
         :children (with-meta [#'get-subnodes #'set-subnodes!] {:argument java.util.List :parent FXParent})
         :title (with-meta [#'get-title #'set-title!] {:argument String :parent FXStage})
         :scene (with-meta [#'get-scene #'set-scene!] {:argument Scene :parent FXStage})
         :root (with-meta [#'get-root #'set-root!] {:argument Parent :parent FXScene})}))

(def mandatory-constructor-args
  (atom {javafx.scene.Scene [:root]}))

(declare compile-o-matic)
(ann apply-props-to-node [Any (Map Keyword Any) -> Any])
(defn apply-props-to-node [node props]
  (doseq [[k v] props]
    (let [translation (get @translation-map k)
          {:keys [argument parent]} (meta translation)
          v (compile-o-matic v)]
      (trace "Key:" k " " (type k) "Value:" v " " (type v))
      (when (nil? translation)
        (throw (Exception. (str "Property" k "not available in translation map."))))
      ;; (when-not ((pred-substitute argument) v)
      ;;   (throw (Exception. (str "Input type" v "is not compatible with expected type for" k))))
      ;; (when-not ((pred-substitute parent) node)
      ;;   (throw (Exception. (str "Property" k "not available for class" (class node)))))
      ((setter translation) node v)))
  node)

(ann build-node [Any (Map Keyword Any) -> Any])
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

