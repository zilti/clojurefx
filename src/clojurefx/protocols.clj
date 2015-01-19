(ns clojurefx.protocols
  (:refer-clojure :exclude [atom doseq let fn defn ref dotimes defprotocol loop for send meta with-meta])
  (:require [clojure.core.typed :refer :all]))

;;## Shadows

(defprotocol [[A :variance :covariant]]
  FXMeta
  (meta [this :- A] :- (Map Any Any))
  (with-meta [this :- A metadata :- (Map Any Any)] :- A))

;;## Standard

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
              [B :variance :covariant :< Seqable]]
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

(defprotocol [[A :variance :covariant :< javafx.css.Styleable]
              [B :variance :covariant :< javafx.css.Styleable]]
  FXStyleable
  "http://download.java.net/jdk8/jfxdocs/javafx/css/Styleable.html"
  (get-css-meta [this :- A] :- (java.util.List javafx.css.CssMetaData)) ;; TODO
  (get-pseudo-class-styles [this :- A] :- (javafx.collections.ObservableSet javafx.css.PseudoClass))
  (get-style [this :- A] :- String)
  (get-style-classes [this :- A] :- (javafx.collections.ObservableList String))
  (set-style-classes! [this :- A classes :- java.util.Collection] :- A)
  (get-styleable-parent [this :- A] :- (U nil B))
  (get-type-selector [this :- A] :- String))

(defprotocol [[A :variance :covariant]]
  FXStyleSetter
  (set-style! [this :- A style :- String] :- A))

(defalias FXStyled (U FXStyleable FXStyleSetter))

(defprotocol [[A :variance :covariant]]
  FXOnAction
  (get-action [this :- A] :- [javafx.event.EventHandler -> Any])
  (set-action! [this :- A action :- [javafx.event.EventHandler -> Any]] :- A)
  (fire! [this :- A] :- nil))

;;## Special Types

;;### javafx.event

(defprotocol [[A :variance :covariant :< javafx.event.Event]]
  FXEvent
  (source [this :- A] :- Any)
  (consume! [this :- A] :- A)
  (copy [this :- A newSource :- Object newTarget :- javafx.event.EventTarget] :- A)
  (event-type [this :- A] :- javafx.event.EventType)
  (target [this :- A] :- javafx.event.EventTarget)
  (consumed? [this :- A] :- Boolean))

;;### javafx.stage

(defprotocol [[A :variance :covariant :< javafx.stage.Stage]
              [B :variance :covariant :< javafx.scene.Scene]]
  FXStage
  (get-title [this :- A] :- String)
  (set-title! [this :- A title :- String] :- A)
  (get-scene [this :- A] :- B)
  (set-scene! [this :- A scene :- B] :- A))

;;### javafx.scene

(defprotocol [[A :variance :covariant :< javafx.scene.Scene]
              [B :variance :covariant :< javafx.scene.Parent]]
  FXScene
  (get-root [this :- A] :- B)
  (set-root! [this :- A root :- B] :- A))
