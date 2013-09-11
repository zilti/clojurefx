(ns clojurefx.core
  (:require [clojure.string :as str]
            [clojure.data :as cljdata]))

(defonce force-toolkit-init (javafx.embed.swing.JFXPanel.))

(defn run-later*"
Simple wrapper for Platform/runLater. You should use run-later.
" [f]
  (javafx.application.Platform/runLater f))

(defmacro run-later [& body]
  `(run-later* (fn [] ~@body)))

(defn run-now*"
A modification of run-later waiting for the running method to return. You should use run-now.
" [f]
(if (= "JavaFX Application Thread" (.. Thread currentThread getName))
  (apply f [])
  (let [result (promise)]
    (run-later
     (deliver result (try (f) (catch Throwable e e))))
    @result)))

(defmacro run-now [& body]
  `(run-now* (fn [] ~@body)))

(defn- camel [in]
  (let [in (name in)
        in (str/split in #"-")
        in (map #(if (= (str (first %)) (str/upper-case (first %)))
                   % (str/capitalize %)) in)
        in (conj (into [] in) "Builder")]
    (apply str in)))

(defn- get-qualified "
An exhaustive list of [everything implementing the builder pattern](http://docs.oracle.com/javafx/2/api/javafx/util/Builder.html). In the future it will be possible to add entries yourself.<br/>
Don't use this yourself; See the macros \"build\" and \"deffx\" below.
" [builder]
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

(defmulti argparser (fn [n] (first n)))
(defmethod argparser :default [[n & rst]]
  (case n    
    `(~n ~@rst)))

(defmacro build "This helper macro makes it easier to use the [JavaFX builder classes](http://docs.oracle.com/javafx/2/api/javafx/util/Builder.html). You can also use a map, so it is possible to compose the arguments for the builder over time.

**Examples:**

 * `(build button (text \"Close me.\") (cancelButton true))`
 * `(build button {:text \"Close me.\" :cancelButton true})`
"[what & args]
  (if (and (= 1 (count args))
           (map? (first args)))
    `(build ~what ~@(for [entry# (keys (first args))]
                      `(~(symbol (name entry#)) ~((first args) entry#))))
    `(run-now (.. ~(get-qualified what) ~(symbol "create")
                  ~@(for [arg# args]
                      (argparser arg#))
                  ~(symbol "build")))))

(defmacro deffx "
Uses build and assigns the result to a symbol.
"[name what & args]
`(def ~name
     (build ~what ~@args)))

;; ### Event handling
(defmacro add-property-listener "
Adds a listener to  prop (\"Property\" gets added automatically) of obj, gets the value and passes it to fun.<br/>
Example: `(add-listener inputfield focused #(println \"Focus change!\"))`
"[obj prop fun]
  `(.. ~obj
       ~(symbol (str (name prop) "Property"))
       (addListener (reify javafx.beans.value.ChangeListener
                      (changed [c#]
                        (~fun (.getValue c#)))))))

(defn event-handler* [f]
  (reify javafx.event.EventHandler
    (handle [this e] (f e))))

(defmacro event-handler [arg & body]
  `(event-handler* (fn ~arg ~@body)))

;; ### Tables
;; Table data in an atom
(defn add-listener [coll l]
  (with-meta coll (update-in (meta coll) [:listener] conj l)))
(defn remove-listener [coll l]
  (with-meta coll (update-in (meta coll) [:listener] (fn [x y] (remove #(= y %) x)) l)))

(defmethod argparser 'table-view [[n & rst]]
  (case n
    items `(items (convert-table-data ~@rst))
    `(~n ~@rst)))
