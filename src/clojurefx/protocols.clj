(ns clojurefx.protocols
  (:refer-clojure :exclude [atom doseq let fn defn ref dotimes defprotocol loop for send])
  (:require [clojure.core.typed :refer :all]))

(declare-protocols FXValue FXId FXParent)
(defprotocol [[A :variance :covariant]
              [B :variance :covariant]]
  FXValue
  (get-value [this :- A] :- B)
  (set-value! [this :- A value :- B] :- A))

(defprotocol [[A :variance :covariant]
              [x :variance :covariant]]
  FXId
  (get-id [this :- A] :- (U nil String))
  (set-id! [this :- A id :- String] :- A))

(defalias FXElement (U FXValue FXId))

(defprotocol [[A :variance :covariant]
              [B :variance :covariant]]
  FXParent
  "The ClojureFX extension to javafx.scene.Parent."
  (get-subnodes [this :- A] :- B)
  (set-subnodes! [this :- A nodes :- B] :- A))

(defprotocol [[A :variance :covariant]
              [B :variance :covariant]]
  FXContainer
  (get-content [this :- A] :- B)
  (set-content! [this :- A node :- B] :- A))

(defprotocol [[A :variance :covariant]
              [B :variance :covariant :< javafx.scene.Node]]
  FXGraphic
  (get-graphic [this :- A] :- B)
  (set-graphic! [this :- A graphic :- B] :- A))
