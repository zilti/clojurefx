(ns clojurefx.protocols)

;;## Shadows

(defprotocol
    FXMeta
  (meta [this])
  (with-meta [this metadata]))

;;## Standard

;; (declare-protocols FXValue FXId FXParent)
(defprotocol
    FXValue
  (value [this])
  (set-value! [this value]))

(defprotocol
    FXId
  (id [this])
  (set-id! [this id]))

;; (defalias FXElement (U FXValue FXId))

(defprotocol
    FXParent
  "The ClojureFX extension to javafx.scene.Parent."
  (subnodes [this])
  (set-subnodes! [this nodes]))

(defprotocol
    FXContainer
  (content [this])
  (set-content! [this node]))

(defprotocol
    FXGraphic
  (graphic [this])
  (set-graphic! [this graphic]))

(defprotocol
    FXStyleable
  "http://download.java.net/jdk8/jfxdocs/javafx/css/Styleable.html"
  (css-meta [this]) ;; TODO
  (pseudo-class-styles [this])
  (style [this])
  (style-classes [this])
  (set-style-classes! [this classes])
  (styleable-parent [this])
  (type-selector [this]))

(defprotocol
    FXStyleSetter
  (set-style! [this style]))

;; (defalias FXStyled (U FXStyleable FXStyleSetter))

(defprotocol
    FXOnAction
  (action [this])
  (set-action! [this action])
  (fire! [this]))

;;## Special Types

;;### javafx.event

(defprotocol
    FXEvent
  (source [this])
  (consume! [this])
  (copy [this newSource newTarget])
  (event-type [this])
  (target [this])
  (consumed? [this]))

;;### javafx.stage

(defprotocol
    FXStage
  (title [this])
  (set-title! [this title])
  (scene [this])
  (set-scene! [this scene]))

;;### javafx.scene

(defprotocol
    FXScene
  (root [this])
  (set-root! [this root]))
