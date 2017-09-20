(ns clojurefx.controllergen
  ;; (:gen-class :name Controllergen
  ;;             :implements [org.objectweb.asm.Opcodes])
  ;; (:import (net.openhft.compiler CachedCompiler CompilerUtils))
  (:import (org.objectweb.asm ClassWriter Opcodes))
  (:use swiss.arrows)
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [camel-snake-kebab.core :as csk]
            [clojure.spec.alpha :as s]))
(timbre/refer-timbre)

;; (def xmlzip (zip/xml-zip (xml/parse "/home/zilti/projects/lizenztool/resources/fxml/mainwindow.fxml")))

;; Compiler

;; (defonce cached-compiler (CachedCompiler. nil nil))

;; (defn makeclass [pkg classname code]
;;   (debug (str "\n" code))
;;   (try
;;     (.loadFromJava cached-compiler (str/join "." [pkg classname]) code)
;;     (catch java.lang.ClassNotFoundException e (error e))))

;; ;; Parser

(def imports (list clojure.java.api.Clojure clojure.lang.IFn java.net.URL java.util.ResourceBundle javafx.event.ActionEvent javafx.fxml.FXML))

(defn build-imports [filename] 
  (->> slurp filename
       str/split-lines
       (filter #(str/starts-with? % "<?import"))
       (map #(str/replace % #"<\?" ""))
       (map #(str/replace % #"\?>" ""))
       (map #(Class/forName %))
       (reduce conj imports)))

(defn qualify-class [imports class-str]
  (first (filter #(= class-str (last (str/split (pr-str %) #"\."))))))

(defn init-class [pkg classname]
  (let [cw (new org.objectweb.asm.ClassWriter 0)
        clazz (.visit cw Opcodes/V1_8
                      (+ Opcodes/ACC_PUBLIC Opcodes/ACC_SUPER)
                      (str pkg "/" classname)
                      nil
                      "java/lang/Object"
                      nil)
        resources_fv (.visitField cw Opcodes/ACC_PRIVATE
                                  "resources"
                                  (pr-str (qualify-class "ResourceBundle"))
                                  nil
                                  nil)
        url_fv (.visitField cw Opcodes/ACC_PRIVATE
                            "location"
                            (pr-str (qualify-class "URL"))
                            nil
                            nil)]
    (-> (.visitAnnotation resources_fv "Ljavafx/fxml/FXML;" true)
        .visitEnd)
    (.visitEnd resources_fv)

    (-> (.visitAnnotation url_fv "Ljavafx/fxml/FXML;" true)
        .visitEnd)
    (.visitEnd url_fv)
    cw))

(defn zip-tree-seq [node]
  (tree-seq (complement string?)
            :content
            node))

(defn get-handler-props [{:keys [attrs]}]
  (->> attrs
       (filter #(str/starts-with? (name (key %)) "on"))
       (map val)
       flatten))

(defn get-handler-fns [ziptree]
  (->> ziptree
       (map get-handler-props)
       (remove empty?)
       flatten))

(defn get-fxid-elems [ziptree]
  (->> ziptree
       (filter #(contains? (:attrs %) :fx:id))))

(defn get-controller-class [fxmlzip]
  (->> fxmlzip
       (filter #(contains? (:attrs %) :fx:controller))
       first
       :attrs
       :fx:controller))

(defn gen-props [cw [entry & coll]]
  (if-not (empty? coll)
    (let [fv (.visitField cw Opcodes/ACC_PUBLIC
                          (get-in entry [:attrs :fx:id])
                          (pr-str (qualify-class (name (:tag entry))))
                          nil nil)]
      (-> (.visitAnnotation fv "Ljava/fxml/FXML;" true)
          .visitEnd)
      (.visitEnd fv)
      (recur cw coll))
    cw))

(defn gen-handlers [cw [entry & coll] clj-ns]
  (if-not (empty? coll)
    (let [mv (.visitMethod cw 0
                           (subs entry 1)
                           "(Ljavafx/event/ActionEvent;)V"
                           nil nil)]
      (-> (.visitAnnotation mv "Ljava/fxml/FXML;" true)
          .visitEnd)
      (.. mv
          visitCode
          (visitLdcInsn clj-ns)
          (visitLdcInsn (csk/->kebab-case (subs % 1)))
          (visitMethodInsn Opcodes/INVOKESTATIC "clojure/java/api/Clojure" "var" "(Ljava/lang/Object;Ljava/lang/Object;)Lclojure/lang/IFn;" false)
          (visitVarInsn Opcodes/ALOAD 0)
          (visitVarInsn Opcodes/ALOAD 1)
          (visitMethodInsn Opcodes/INVOKEINTERFACE "clojure/lang/IFn" "invoke" "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;" true)
          (visitInsn Opcodes/POP)
          (visitInsn Opcodes/RETURN)
          (visitMaxs 3 2)
          visitEnd) 
      (recur cw coll clj-ns))
    cw))

;; (defn gen-handlers [coll clj-ns]
;;   (->> (flatten coll) 
;;        (map #(format "    @FXML\n    void %s(ActionEvent event) {\n        Clojure.var(\"%s\", \"%s\").invoke(this, event);\n    }\n\n"
;;                      (subs % 1) clj-ns (csk/->kebab-case (subs % 1))))
;;        (str/join "")))

;; (defn gen-initializer [cns cfn]
;;   (format "    @FXML
;;     void initialize() {
;;         Clojure.var(\"%s\", \"%s\").invoke(this);
;;     }" cns cfn))

;; (defn gen-fx-controller [fxmlzip fxmlpath [clj-ns clj-fn]]
;;   (let [clazz (get-controller-class fxmlzip)
;;         [pkg classname] (reverse (map str/reverse (str/split (str/reverse clazz) #"\." 2)))
;;         fxid-elems (get-fxid-elems fxmlzip)
;;         handler-fns (get-handler-fns fxmlzip)]
;;     (debug "fxid-elems:" (pr-str fxid-elems))
;;     (debug "handler-fns:" (pr-str handler-fns))
;;     (str (format "package %s;\n\n" pkg)
;;          stockimports
;;          (get-imports fxmlpath)
;;          (format "\n\npublic class %s {\n\n" classname)
;;          (gen-props fxid-elems)
;;          (gen-handlers handler-fns clj-ns)
;;          (gen-initializer clj-ns clj-fn)
;;          "\n}")))

;; ;; Plumber

;; (defn gen-fx-controller-class [fxmlpath clj-fn]
;;   (let [clj-fn (if (symbol? clj-fn)
;;                  (str (namespace clj-fn) "/" (name clj-fn))
;;                  clj-fn)
;;         fxmlzip (zip-tree-seq (xml/parse (io/input-stream fxmlpath)))
;;         clazz (get-controller-class fxmlzip)
;;         [pkg classname] (reverse (map str/reverse (str/split (str/reverse clazz) #"\." 2)))
;;         cljvec (str/split clj-fn #"/")] 
;;     (makeclass pkg classname (gen-fx-controller fxmlzip fxmlpath cljvec))))

(defn gen-fx-controller-class [fxmlpath clj-fn])
