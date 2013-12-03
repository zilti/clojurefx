(ns clojurefx.core-test
  (:require [midje.sweet :refer :all]
            [clojurefx.core :refer :all]))

(fact "ClojureFX node names to JavaFX"
      (get-qualified 'accordion) => 'javafx.scene.control.Accordion
      (get-qualified 'check-box-list-cell) => 'javafx.scene.control.cell.CheckBoxListCell
      (get-qualified 'HTML-editor) => 'javafx.scene.web.HTMLEditor)

(fact "Getter camelcaseizing"
      (prepend-and-camel "get" "one") => "getOne"
      (prepend-and-camel "get" "one-two") => "getOneTwo"
      (prepend-and-camel "get" "one-two-three") => "getOneTwoThree")

(fact "Correct class creation"
      (type (fx button)) => javafx.scene.control.Button
      (type (fx combo-box)) => javafx.scene.control.ComboBox
      (type (fx check-menu-item)) => javafx.scene.control.CheckMenuItem)

(fact "Setter generation"
      (.getText (fx button :text "ClojureFX")) => "ClojureFX")

(fact "Constructor detection"
      (type (fx color-picker :color javafx.scene.paint.Color/BLUE)) => javafx.scene.control.ColorPicker
      (type (fx scene :root (fx v-box) :width 800 :height 600)) => javafx.scene.Scene)

(deffx exlbl label :text "Label")
(def vb (atom nil))
(facts "Simple content swapping"
  (with-state-changes [(before :facts (reset! vb (fx v-box :children [(fx button :text "Hi") exlbl])))]
       (fact "Simple swap-content! usage"
             (-> (count (getfx (swap-content! @vb conj (fx button :text "H")) :children))) => 3)
       (fact "conjoining macro"
             (-> (count (getfx (fx-conj! @vb (fx label :text "ClojureFX")) :children))) => 3)
       (fact "removing an element"
             (-> (count (getfx (fx-remove! @vb exlbl) :children))) => 1)
       (fact "removing everything"
             (-> (count (getfx (fx-remove-all! @vb) :children))) => 0)))

(def propbind (atom nil))
(facts "Property binding"
       (with-state-changes [(before :facts (reset! propbind "New text"))]
         (fact "String binding"
               (-> (fx button :text "Old text") (bind-property! :text propbind) .getText) => "New text"
               (let [btn (fx button :text "Old text")]
                 (bind-property! btn :text propbind)
                 (reset! propbind "ClojureFX")
                 (.getText btn)) => "ClojureFX")
         (fact "String binding at object creation time"
               (-> (fx button :text "Old text" :bind {:text propbind}) .getText) => "New text"))
       (with-state-changes [(before :facts (reset! propbind true))]
         (fact "Boolean binding"
               (-> (fx grid-pane :grid-lines-visible false) (bind-property! :grid-lines-visible propbind) .isGridLinesVisible) => true)
         (fact "Boolean binding at object creation time"
               (-> (fx grid-pane :bind {:grid-lines-visible propbind}) .isGridLinesVisible) => true))
       (future-fact "Write test for binding of multiple properties in bind-properties! macro."))

(def fireatom (atom nil))
(facts "Action binding"
       (with-state-changes [(before :facts (let [actionbtn (fx button)]
                                             (set-listener! actionbtn :on-action [x] (reset! fireatom "Fired"))
                                             (.fire actionbtn)))]
         (fact "Action binding"
               @fireatom => "Fired"))
       (with-state-changes [(before :facts (let [actionbtn-inline (fx button :listen {:on-action (fn [x] (reset! fireatom "InlineFired"))})]
                                             (.fire actionbtn-inline)))]
         (fact "Inline fx* action binding"
               @fireatom => "InlineFired")))

(facts "Child elements"
       (fact "Simple child elements"
             (-> (fx v-box :children [(fx button)]) .getChildren first type) => javafx.scene.control.Button))

(def gpa (atom nil))
(facts "GridPane"
       (fact "Creating an empty GridPane"
             (type (fx grid-pane)) => javafx.scene.layout.GridPane)
       (with-state-changes [(before :facts (reset! gpa (fx grid-pane)))]
         (fact "Adding a raw button"
               (type (first (.getChildren (swap-content! @gpa (fn [_] [(fx button :text "Hi!")]))))) => javafx.scene.control.Button)
         (fact "Adding an enriched button"
               (type (first (.getChildren (swap-content! @gpa (fn [_] [{:node (fx button :text "Hi!")}]))))) => javafx.scene.control.Button)
         (fact "Adding a button with options"
               (getfx (swap-content! @gpa (fn [_] [{:node (fx button :text "Hi!")
                                                            :fill-height? true}]))
                      :fill-height?
                      (first (.getChildren @gpa))) => true))
       (fact "Setting a button at fx-expansion-time"
             (type (first (.getChildren (fx grid-pane :children [(fx button)])))) => javafx.scene.control.Button))
