(ns clojurefx.core
  (:require [clojure.string :as str]))

(defn run-later*
  [f]
  (javafx.application.Platform/runLater f))

(defmacro run-later
  [& body]
  `(run-later* (fn [] ~@body)))

(defn run-now*
  [f]
  (let [result (promise)]
    (run-later
     (deliver result (try (f) (catch Throwable e e))))
    @result))

(defmacro run-now
  [& body]
  `(run-now* (fn [] ~@body)))

(defn event-handler*
  [f]
  (reify javafx.event.EventHandler
    (handle [this e] (f e))))

(defmacro event-handler [arg & body]
  `(event-handler* (fn ~arg ~@body)))

(def build-reference
  '{accordion "javafx.scene.control"
    
    affine "javafx.scene.transform"
    
    anchor "javafx.scene.layout"
    border-pane "javafx.scene.layout"
    
    arc "javafx.scene.shape"
    arc-to "javafx.scene.shape"

    blend "javafx.scene.effect"
    bloom "javafx.scene.effect"
    box-blur "javafx.scene.effect"

    area-chart "javafx.scene.chart"
    bar-chart "javafx.scene.chart"
    bubble-chart "javafx.scene.chart"
    axis "javafx.scene.chart"

    audio-clip "javafx.scene.media"

    bounding-box "javafx.geometry"})

(defn- camel [in]
  (let [in (name in)
        in (str/split in #"-")
        in (map #(if (= (str (first %)) (str/upper-case (first %)))
                   % (str/capitalize %)) in)
        in (conj (into [] in) "Builder")]
    (apply str in)))

(defn- get-qualified [builder]
  (let [pkgs {"javafx.scene.control" '[accordion button cell check-box check-box-tree-item check-menu-item choice-box
                                       color-picker combo-box context-menu custom-menu-item hyperlink
                                       indexed-cell index-range label list-cell list-view menu-bar menu menu-button menu-item
                                       pagination password-field popup-control progress-bar progress-indicator
                                       radio-button radio-button-menu-item scroll-bar scroll-pane separator
                                       separator-menu-item slider split-menu-button split-pane tab table-cell table-column
                                       table-position table-row table-view tab-pane text-area text-field tree-cell
                                       titled-pane toggle-button toggle-group tool-bar tooltip tree-item tree-view]
              "javafx.scene.control.cell" '[check-box-list-cell check-box-table-cell check-box-tree-cell
                                            choice-box-list-cell choice-box-table-cell choice-box-tree-cell
                                            combo-box-list-cell combo-box-table-cell combo-box-tree-cell
                                            text-field-list-cell text-field-table-cell text-field-tree-cell
                                            property-value-factory]
              "javafx.scene.layout" '[anchor border-pane column-constraints flow-pane grid-pane h-box pane region
                                      row-constraints stack-pane tile-pane v-box]
              "javafx.scene.text" '[text font]
              "javafx.scene.shape" '[arc arc-to circle close-path cubic-curve cubic-curve-to ellipse
                                     h-line-to line line-to move-to path polygon polyline quad-curve quad-curve-to
                                     rectangle SVG-path b-line-to]
              "javafx.scene.canvas" '[canvas]
              "javafx.scene.image" '[image-view]
              "javafx.scene.input" '[clipboard-content key-character-combination key-code-combination mnemonic]
              "javafx.scene.effect" '[blend bloom box-blur color-adjust color-input displacement-map dropshadow float-map
                                      gaussian-blur glow image-input inner-shadow lighting motion-blur perspective-transform
                                      reflection sepia-tone shadow]
              "javafx.scene.paint" '[color image-pattern linear-gradient radial-gradient stop]
              "javafx.scene.chart" '[chart area-chart bar-chart line-chart bubble-chart pie-chart scatter-chart
                                     stacked-area-chart stacked-bar-chart x-y-chart
                                     axis category-axis number-axis value-axis]
              "javafx.scene.media" '[audio-clip media media-player media-view]
              "javafx.scene.transform" '[affine rotate scale shear translate]
              "javafx.scene.web" '[HTML-editor prompt-data web-engine web-view]
              "javafx.scene" '[scene group image-cursor perspective-camera snapshot-parameters]
              "javafx.animation" '[fade-transition fill-transition parallel-transition path-transition pause-transition
                                   rotate-transition scale-transition sequential-transition stroke-transition timeline
                                   translate-transition]
              "javafx.stage" '[stage directory-chooser file-chooser popup]
              "javafx.geometry" '[bounding-box dimension-2D insets point-2D point-3D rectangle-2D]
              "javafx.embed.swt" '[custom-transfer]
              "javafx.embed.swing" '[JFX-panel-builder]}
        builder (symbol builder)]
    (first (filter (comp not nil?) (for [k (keys pkgs)]
                                     (if (not (empty? (filter #(= % builder) (get pkgs k))))
                                       (symbol (str k "." (camel (name builder))))))))))

(defmacro build [what & args]
  `(.. ~(get-qualified what) create
       ~@args
       build))

(defmacro deffx [name what & args]
  `(def ~name
     (build ~what ~@args)))
