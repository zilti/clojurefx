(ns clojurefx.core
  (:require [clojure.string :as str]
            [clojure.data :as cljdata]
            [clojure.walk :refer :all]
            [clojure.reflect :refer :all]))

(defonce force-toolkit-init (javafx.embed.swing.JFXPanel.))

;; ## Threading helpers

(defn run-later*"
Simple wrapper for Platform/runLater. You should use run-later.
" [f]
(javafx.application.Platform/runLater f))

(defmacro run-later [& body]
  `(run-later* (fn [] ~@body)))

(defn run-now*"
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

(defn camel [in & [method?]]
  (let [in (name in)
        in (str/split in #"-")
        in (map #(if (= (str (first %)) (str/upper-case (first %)))
                   % (str (str/upper-case (subs % 0 1)) (subs % 1))) in)
        in (apply str (into [] in))]
    (if method?
      (str (str/lower-case (subs in 0 1)) (subs in 1))
      in)))

;; TODO this is an idiotic place for this function.
(defn prepend-and-camel [prep s]
  (let [c (camel (str prep "-" s))]
    (str (str/lower-case (subs c 0 1)) (subs c 1))))

;; ## Collection helpers
;; This probably isn't the ideal approach for mutable collections. Check back for better ones.
(defn seq->observable [s]
  (javafx.collections.FXCollections/unmodifiableObservableList s))

(defn map->observable [m]
  (javafx.collections.FXCollections/unmodifiableObservableMap m))

(defn set->observable [s]
  (javafx.collections.FXCollections/unmodifiableObservableSet s))

;; Argument wrapping
(defmulti wrap-arg "Autoboxing-like behaviour for arguments for ClojureFX nodes." (fn [arg class] arg))

(defmethod wrap-arg :default [arg class] arg)

(defmethod wrap-arg :accelerator [arg _]
  (if (string? arg)
    (javafx.scene.input.KeyCombination/keyCombination arg)
    arg))

;; ## <a name="databinding"></a> Data binding
(defmulti bidirectional-bind-property! (fn [type obj prop & args] type))

(defmulti bind-property!* (fn [obj prop target] (type target)))

(defmethod bind-property!* clojure.lang.Atom [obj prop at]
  (let [listeners (atom [])
        inv-listeners (atom [])
        prop (str (camel prop true) "Property")
        property (run-now (clojure.lang.Reflector/invokeInstanceMethod obj prop (to-array [])))
        observable (reify javafx.beans.value.ObservableValue
                     (^void addListener [this ^javafx.beans.value.ChangeListener l] (swap! listeners conj l))
                     (^void addListener [this ^javafx.beans.InvalidationListener l] (swap! inv-listeners conj l))
                     (^void removeListener [this ^javafx.beans.InvalidationListener l] (swap! inv-listeners #(remove #{l} %)))
                     (^void removeListener [this ^javafx.beans.value.ChangeListener l] (swap! listeners #(remove #{l} %)))
                     (getValue [this] @at))]
    (add-watch at (keyword (name prop))
               (fn [_ r oldS newS]
                 (run-now (doseq [listener @inv-listeners] (.invalidated listener observable))
                          (doseq [listener @listeners] (.changed listener observable oldS newS)))))
    (run-now (.bind property observable))
    (run-now (doseq [listener @inv-listeners] (.invalidated listener observable)))
    obj))

(defmacro bind-property! "Binds properties to atoms.
Other STM objects might be supported in the future.
Whenever the content of the atom changes, this change is propagated to the property.

args is a named-argument-list, where the key is the property name (e.g. :text) and the value the atom.
" [obj & args]
  (let [m# (apply hash-map args)]
    `(do ~@(for [entry# m#]
             `(bind-property!* ~obj ~(key entry#) ~(val entry#))))))

;; ## <a name="events"></a> Events
(defn- prep-key-code [k]
  {:keycode k
   :name (-> (.getName k) str/lower-case keyword)
   :value (-> (.valueOf k) str/lower-case keyword)
   :arrow? (.isArrowKey k)
   :digit? (.isDigitKey k)
   :function? (.isFunctionKey k)
   :keypad? (.isKeypadKey k)
   :media? (.isMediaKey k)
   :modifier? (.isModifierKey k)
   :navigation? (.isNavigationKey k)
   :whitespace? (.isWhitespaceKey k)})

(defn- prep-pickresult [p]
  {:pickresult p
   :distance (.getIntersectedDistance p)
   :face (.getIntersectedFace p)
   :node (.getIntersectedNode p)
   :point (.getIntersectedPoint p)
   :tex-coord (.getIntersectedTexCoord p)})


(defn- prep-event-map [e & {:as m}]
  (let [prep {:event e
              :source (.getSource e)
              :type (.getEventType e)
              :target (.getTarget e)
              :consume #(.consume e)
              :string (.toString e)}]
    (if (nil? m) prep (merge prep m))))
(defn- add-modifiers [m e]
  (merge m
         {:alt-down? (.isAltDown e)
          :control-down? (.isControlDown e)
          :meta-down? (.isMetaDown e)
          :shift-down? (.isShiftDown e)
          :shortcut-down? (.isShortcutDown e)}))
(defn- add-coords [m e]
  (merge m
         {:screen-coords {:x (.getScreenX e) :y (.getScreenY e)}
          :scene-coords {:x (.getSceneX e) :y (.getSceneY e)}
          :coords {:x (.getX e) :y (.getY e) :z (.getZ e)}}))

(defmulti preprocess-event (fn [e] (type e)))
(defmethod preprocess-event :default [e]
  (prep-event-map e))

;; ### ContextMenuEvent

(defmethod preprocess-event javafx.scene.input.ContextMenuEvent [e]
  (-> (prep-event-map e
                     :pickresult (prep-pickresult (.getPickResult e))
                     :keyboard-trigger? (.isKeyboardTrigger e))
     (add-coords e)))

;; ### InputMethodEvent

(defmethod preprocess-event javafx.scene.input.InputMethodEvent [e]
  (prep-event-map e
                  :caret-position (.getCaretPosition e)
                  :committed (.getCommitted e)
                  :composed (.getComposed e)))

;; ### KeyEvent

(defmethod preprocess-event javafx.scene.input.KeyEvent [e]
  (-> (prep-event-map e
                     :character (.getCharacter e)
                     :code (prep-key-code (.getCode e))
                     :text (.getText e))
     (add-modifiers e)))

;; ### MouseEvent

(defmethod preprocess-event javafx.scene.input.MouseEvent [e]
  (-> (prep-event-map e
                     :button (-> (.getButton e) .valueOf str/lower-case keyword)
                     :click-count (.getClickCount e)
                     :pickresult (prep-pickresult (.getPickResult e))
                     :drag-detected? (.isDragDetect e)
                     :primary-button? (.isPrimaryButtonDown e)
                     :secondary-button? (.isSecondaryButtonDown e)
                     :middle-button? (.isMiddleButtonDown e)
                     :popup-trigger? (.isPopupTrigger e)
                     :sill-since-press? (.isStillSincePress e)
                     :synthesized? (.isSynthesized e))
     (add-modifiers e)
     (add-coords e)))

;; ### TouchEvent

(defmethod preprocess-event javafx.scene.input.TouchEvent [e]
  (prep-event-map e
                  :set-id (.getEventSetId e)
                  :touch-count (.getTouchCount e)
                  :touch-point (.getTouchPoint e) ;; TODO Wrapper for TouchPoint
                  :touch-points (.getTouchPoints e)
                  :alt-down? (.isAltDown e)
                  :control-down? (.isControlDown e)
                  :meta-down? (.isMetaDown e)
                  :shift-down? (.isShiftDown e)))

;; #### API
(defn set-listener!* [obj event fun]
  (run-now
   (clojure.lang.Reflector/invokeInstanceMethod obj (prepend-and-camel "set" (name event))
                                                (to-array [(reify javafx.event.EventHandler
                                                             (handle [this t]
                                                               (fun (preprocess-event t))))]))))

(defmacro set-listener! "Adds a listener to a node event.
The listener gets a preprocessed event map as shown above.
" [obj event args & body]
`(set-listener!* ~obj ~event (fn ~args ~@body)))

;; ## <a name="contentmodification"></a> Content modification
;; ### Easy modification of child elements.
;; Usage: `(swap-content! <object> <modification-function>)`.
;; The return value of modification-function becomes the new value.

(defmulti swap-content! (fn [obj fun] (class obj)))
(defmacro def-simple-swapper [clazz getter setter]
  `(defmethod swap-content! ~clazz [obj# fun#]
     (let [bunch# (~getter obj#)]
       (run-now (~setter bunch# (fun# (into [] bunch#)))))))

(def-simple-swapper javafx.scene.layout.Pane .getChildren .setAll)
(def-simple-swapper javafx.scene.Group .getChildren .setAll)
(def-simple-swapper javafx.scene.control.Accordion .getPanes .setAll)
(def-simple-swapper javafx.scene.control.ChoiceBox .getItems .setAll)
(def-simple-swapper javafx.scene.control.ColorPicker .getCustomColors .setAll)
(def-simple-swapper javafx.scene.control.ComboBox .getItems .setAll)
(def-simple-swapper javafx.scene.control.ContextMenu .getItems .setAll)
(def-simple-swapper javafx.scene.control.ListView .getItems .setAll)
(def-simple-swapper javafx.scene.control.Menu .getItems .setAll)
(def-simple-swapper javafx.scene.control.MenuBar .getMenus .setAll)
(def-simple-swapper javafx.scene.control.TableColumn .getColumns .setAll)
(def-simple-swapper javafx.scene.control.TabPane .getTabs .setAll)
(def-simple-swapper javafx.scene.control.ToggleGroup .getToggles .setAll)
(def-simple-swapper javafx.scene.control.ToolBar .getItems .setAll)
(def-simple-swapper javafx.scene.control.TreeItem .getChildren .setAll)
(def-simple-swapper javafx.scene.control.TreeTableColumn .getColumns .setAll)

(defmethod swap-content! javafx.scene.control.SplitPane [obj fun]
  (let [data {:items (into [] (.getItems obj))
              :dividers (into [] (.getDividers obj))}
        res (fun data)]
    (.setAll (.getItems obj) (:items res))
    (.setAll (.getDividers obj) (:dividers res))))

;; TODO TreeTableView

(defmethod swap-content! javafx.scene.control.ScrollPane [obj fun]
  (.setContent obj (fun (.getContent obj))))
(defmethod swap-content! javafx.scene.control.TitledPane [obj fun]
  (.setContent obj (fun (.getContent obj))))
(defmethod swap-content! javafx.scene.control.Tab [obj fun]
  (.setContent obj (fun (.getContent obj))))

(defmacro getfx "fetches a property from a node." [obj prop & args]
  (if (= \? (subs (name prop) (dec (count (name prop)))))
    `(~(symbol (str "." (prepend-and-camel "is" (subs (name prop) 0 (dec (count (name prop))))))) ~obj ~@args)
    `(~(symbol (str "." (prepend-and-camel "get" (name prop)))) ~obj ~@args)))

;; ## Builder parsing
(def pkgs (atom {"javafx.scene.control" '[accordion button cell check-box check-box-tree-item check-menu-item choice-box
                                          color-picker combo-box context-menu custom-menu-item date-picker date-cell hyperlink
                                          indexed-cell index-range label list-cell list-view menu-bar menu menu-button menu-item
                                          pagination password-field popup-control progress-bar progress-indicator
                                          radio-button radio-menu-item scroll-bar scroll-pane separator
                                          separator-menu-item slider split-menu-button split-pane tab table-cell table-column
                                          table-position table-row table-view tab-pane text-area text-field tree-cell
                                          titled-pane toggle-button toggle-group tool-bar tooltip tree-item tree-view
                                          tree-table-cell tree-table-column tree-table-row tree-table-view]
                 "javafx.scene.control.cell" '[check-box-list-cell check-box-table-cell check-box-tree-cell
                                               choice-box-list-cell choice-box-table-cell choice-box-tree-cell
                                               combo-box-list-cell combo-box-table-cell combo-box-tree-cell
                                               text-field-list-cell text-field-table-cell text-field-tree-cell
                                               property-value-factory]
                 "javafx.scene.layout" '[anchor-pane border-pane column-constraints flow-pane grid-pane h-box pane region
                                         row-constraints stack-pane tile-pane v-box]
                 "javafx.scene.text" '[text font text-flow]
                 "javafx.scene.shape" '[arc arc-to circle close-path cubic-curve cubic-curve-to ellipse
                                        h-line-to line line-to move-to path polygon polyline quad-curve quad-curve-to
                                        rectangle SVG-path box cylinder mesh-view sphere]
                 "javafx.scene.canvas" '[canvas]
                 "javafx.scene.image" '[image-view]
                 "javafx.scene.input" '[clipboard-content key-character-combination key-code-combination mnemonic]
                 "javafx.scene.effect" '[blend bloom box-blur color-adjust color-input displacement-map drop-shadow float-map
                                         gaussian-blur glow image-input inner-shadow lighting motion-blur perspective-transform
                                         reflection sepia-tone shadow]
                 "javafx.scene.paint" '[color image-pattern linear-gradient radial-gradient stop]
                 "javafx.scene.chart" '[chart area-chart bar-chart line-chart bubble-chart pie-chart scatter-chart
                                        stacked-area-chart stacked-bar-chart x-y-chart
                                        axis category-axis number-axis value-axis]
                 "javafx.scene.media" '[audio-clip media media-player media-view]
                 "javafx.scene.transform" '[affine rotate scale shear translate]
                 "javafx.scene.web" '[HTML-editor prompt-data web-engine web-view]
                 "javafx.scene" '[scene group image-cursor perspective-camera snapshot-parameters
                                  ambient-light parallel-camera perspective-camera point-light]
                 "javafx.animation" '[fade-transition fill-transition parallel-transition path-transition pause-transition
                                      rotate-transition scale-transition sequential-transition stroke-transition timeline
                                      translate-transition]
                 "javafx.stage" '[stage directory-chooser file-chooser popup]
                 "javafx.geometry" '[bounding-box dimension-2D insets point-2D point-3D rectangle-2D]
                 "javafx.embed.swing" '[JFX-panel]}))

(def get-qualified "
An exhaustive list of every visual JavaFX component. To add entries, modify the pkgs atom.<br/>
Don't use this yourself; See the macros \"fx\" and \"deffx\" below.
" (memoize (fn [builder]
             (let [builder (symbol builder)]
               (first (filter (comp not nil?) (for [k (keys @pkgs)]
                                              (if (not (empty? (filter #(= % builder) (get @pkgs k))))
                                                (symbol (str k "." (camel (name builder))))))))))))

(defn method-fetcher "Fetches all public methods of a class." [class]
  (let [cl (reflect class)
        current-methods (filter #(contains? (:flags %) :public) (filter :return-type (:members cl)))]
    (if (nil? cl)
      current-methods
      (reduce #(flatten (conj %1 (method-fetcher (resolve %2)))) current-methods (:bases cl)))))

(defn- uncamelcaseize [sym]
  (let [s (-> sym str seq)]
    (loop [s s
           out (list)]
      (if (empty? s)
        (subs (->> out reverse (apply str)) 1)
        (recur (rest s)
               (if (Character/isUpperCase (first s))
                 (->> out (cons \-) (cons (Character/toLowerCase (first s))))
                 (cons (first s) out)))))))

(def get-method-calls (memoize (fn [ctrl]
                                 (let [full (resolve (get-qualified ctrl))
                                       fns (eval `(method-fetcher ~full))
                                       calls (atom {})]
                                   (doseq [fun fns
                                           :when (= "set" (subs (str (:name fun)) 0 3))]
                                     (swap! calls assoc
                                            (keyword (uncamelcaseize (subs (name (:name fun)) 3)))
                                            (eval `(fn [obj# arg#] (. obj# ~(:name fun) arg#)))))
                                   @calls))))

;; Constructor tools
(defn constructor-helper [clazz & args]
  (run-now (clojure.lang.Reflector/invokeConstructor (resolve clazz) (to-array (remove nil? args)))))

(defmacro construct [clazz keys]
  `(defmethod construct-node '~clazz [cl# ar#]
     (apply constructor-helper cl# (for [k# ~keys] (get ar# k#)))))

(defmulti construct-node (fn [class args] class))
(defmethod construct-node :default [class _]
  (run-now (eval `(new ~class))))

(construct javafx.scene.control.ColorPicker [:color])
(defmethod construct-node javafx.scene.paint.LinearGradient [c {:keys [start-x start-y end-x end-y proportional cycle-method stops]}]
  (constructor-helper c [start-x start-y end-x end-y proportional cycle-method (into-array javafx.scene.paint.Stop stops)]))

;; Builder API
(defn- symbolwalker [q]
  (if (symbol? q)
    (if-let [x (resolve q)]
      x
      q)
    q))

(defn- varwalker [q]
  (if (var? q)
    (deref q)
    q))

;; ## <a name="contentcreation"></a> Content creation
(defn fx* [ctrl & args]
  (let [args# (if-not (and (nil? (first args)) (map? (first args))) (apply hash-map args) (first args))
        {:keys [bind listen content children]} args#
        props# bind
        listeners# listen
        content# (-> [] (into content) (into children))
        qualified-name# (get-qualified ctrl)
        methods# (get-method-calls ctrl)
        args# (dissoc args# :bind :listen)
        obj# (construct-node qualified-name# args#)]
    (run-now (doseq [arg# args#] ;; Apply arguments
               (if (contains? methods# (key arg#))
                 (((key arg#) methods#) obj# (wrap-arg (val arg#) (type obj#)))))
             (doseq [prop# props#] ;; Bind properties
               (bind-property!* obj# (key prop#) (val prop#)))
             (doseq [listener# listeners#] ;; Add listeners
               (set-listener!* obj# (key listener#) (val listener#)))
             (if-not (empty? content#)
               (swap-content! obj# (fn [_] content#)))
             obj#)))

(defmacro fx "
The central macro of ClojureFX. This takes the name of a node as declared in the pkgs atom and
named arguments for the constructor arguments and object setters.

Special keys:

 * `bind` takes a map where the key is a property name (e.g. :text or :grid-lines-visible) and the value an atom. This internally calls `bind-property!`.
 * `listen` takes a map where the key is an event name (e.g. :on-action) and the value a function handling this event.
 * `content` or `children` (equivalent) must be a datastructure a function given to `swap-content!` would return.

" [ctrl & args]
`(fx* '~ctrl ~@args))

(defmacro deffx [name ctrl & props]
  `(def ~name (fx ~ctrl ~@props)))

;; Stage

(construct javafx.stage.Stage [:stage-style])

(defmethod wrap-arg :stage-style [arg class]
  (clojure.lang.Reflector/getStaticField javafx.stage.StageStyle (-> arg name str/upper-case)))

(defmethod swap-content! javafx.stage.Stage [obj fun]
  (.setScene obj (fun (.getScene obj))))

;; Scene

(construct javafx.scene.Scene [:root :width :height :depth-buffer :scene-antialiasing])

(defmethod wrap-arg :scene-antialiasing [arg class]
  (clojure.lang.Reflector/getStaticField javafx.scene.SceneAntialiasing (-> arg name str/upper-case)))

(defmethod swap-content! javafx.scene.Scene [obj fun]
  (.setRoot obj (fun (.getRoot obj))))

;; GridPane

(defmethod swap-content! javafx.scene.layout.GridPane [obj fun]
  (let [data (map #({:node %
                     :column-index (.getColumnIndex obj %)
                     :row-index (.getRowIndex obj %)
                     :column-span (.getColumnSpan obj %)
                     :row-span (.getRowSpan obj %)
                     :h-alignment (-> (.getHalignment obj %) .toString str/lower-case keyword)
                     :v-alignment (-> (.getValignment obj %) .toString str/lower-case keyword)
                     :h-grow (.getHgrow obj %)
                     :v-grow (.getVgrow obj %)
                     :margin (.getMargin obj %)
                     :fill-height? (.isFillHeight obj %)
                     :fill-width? (.isFillWidth obj %)}) (.getChildren obj))
        res (map #(if (map? %) % {:node %}) (fun data))
        children (.getChildren obj)]
    (.setAll children (map :node res))
    (doseq [child res
            :let [n (:node child)]]
      (doseq [[k v] child]
        (case k
          :column-index (.setColumnIndex obj n v)
          :row-index (.setRowIndex obj n v)
          :column-span (.setColumnSpan obj n v)
          :row-span (.setRowSpan obj n v)
          :h-alignment (.setHalignment obj n (-> v name str/upper-case javafx.geometry.HPos/valueOf))
          :v-alignment (.setValignment obj n (-> v name str/upper-case javafx.geometry.VPos/valueOf))
          :h-grow (.setHgrow obj n v)
          :v-grow (.setVgrow obj n v)
          :margin (.setMargin obj n v)
          :fill-height? (.setFillHeight obj n v)
          :fill-width? (.setFillWidth obj n v)
          nil))))
  obj)

(defn swap-column-constraints! [obj fun]
  (let [data (map #({:column %
                     :h-alignment (.getHalignment %)
                     :h-grow (.getHgrow %)
                     :max-width (.getMaxWidth %)
                     :min-width (.getMinWidth %)
                     :%-width (.getPercentWidth %)
                     :pref-width (.getPrefWidth %)
                     :fill-width? (.isFillWidth %)}) (.getColumnConstraints obj))
        res (fun data)]
    (.setAll (.getColumnConstraints obj) (to-array []))
    (doseq [col res
            :let [const (new javafx.scene.layout.ColumnConstraints)]]
      (doseq [[k v] col]
        (case k
          :h-alignment (.setHalignment const v)
          :h-grow (.setHgrow const v)
          :max-width (.setMaxWidth const v)
          :min-width (.setMinWidth const v)
          :%-width (.setPercentWidth const v)
          :pref-width (.setPrefWidth const v)
          :fill-width? (.setFillWidth const v)
          nil))
      (.add (.getColumnConstraints obj) const)))
  obj)

(defn swap-row-constraints! [obj fun]
  (let [data (map #({:row %
                     :v-alignment (.getValignment %)
                     :v-grow (.getVgrow %)
                     :max-height (.getMaxHeight %)
                     :min-height (.getMinHeight %)
                     :%-height (.getPercentHeight %)
                     :pref-height (.getPrefHeight %)
                     :fill-height? (.isFillHeight %)}) (.getRowConstraints obj))
        res (fun data)]
    (.setAll (.getRowConstraints obj) (to-array []))
    (doseq [row res
            :let [const (new javafx.scene.layout.RowConstraints)]]
      (doseq [[k v] row]
        (case k
          :v-alignment (.setValignment const v)
          :v-grow (.setVgrow const v)
          :max-height (.setMaxHeight const v)
          :min-height (.setMinHeight const v)
          :%-height (.setPercentHeight const v)
          :pref-height (.setPrefHeight const v)
          :fill-height? (.setFillHeight const v)
          nil))
      (.add (.getRowConstraints obj) const)))
  obj)

;; Table view

(defmethod wrap-arg :items javafx.scene.control.TableView [arg clazz]
  (seq->observable arg))

(defmethod wrap-arg :columns javafx.scene.control.TableView [arg clazz]
  (seq->observable arg))

(defmethod swap-content! javafx.scene.control.TableView [obj fun]
  (let [data {:items (into [] (.getItems obj))
              :columns (into [] (.getColumns obj))
              :sort-order (into [] (.getSortOrder obj))
              :visible-leaf-columns (into [] (.getVisibleLeafColumns obj))}
        res (fun data)]
    (.setAll (.getItems obj) (:items res))
    (.setAll (.getColumns obj) (:columns res))
    (.setAll (.getSortOrder obj) (:sort-order res))
    (.setAll (.getVisibleLeafColumns obj) (:visible-leaf-columns res))))
