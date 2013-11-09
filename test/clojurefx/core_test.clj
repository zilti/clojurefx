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
