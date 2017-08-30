(ns clojurefx.controllergen
  (import (net.openhft.compiler CachedCompiler CompilerUtils))
  (:use swiss.arrows)
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [camel-snake-kebab.core :as csk]))
(timbre/refer-timbre)

;; (def xmlzip (zip/xml-zip (xml/parse "/home/zilti/projects/lizenztool/resources/fxml/mainwindow.fxml")))

;; Compiler

(defonce cached-compiler (CachedCompiler. nil nil))

(defn makeclass [pkg classname code]
  (debug (str "\n" code))
  (try
    (.loadFromJava cached-compiler (str/join "." [pkg classname]) code)
    (catch java.lang.ClassNotFoundException e (error e))))

;; Parser

(def stockimports "import clojure.java.api.Clojure;\nimport clojure.lang.IFn;\nimport java.net.URL;\nimport java.util.ResourceBundle;\nimport javafx.event.ActionEvent;\nimport javafx.fxml.FXML;\n")

(def stockprops "    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;\n\n")

(defn get-imports [filename]
  (->>  (slurp filename)
        (str/split-lines)
        (filter #(str/starts-with? % "<?import"))
        (map #(str/replace % #"<\?" ""))
        (map #(str/replace % #"\?>" ";"))
        (str/join "\n")))

(defn zip-tree-seq [node]
  (tree-seq (complement string?)
            :content
            node))

(defn get-handler-props [{:keys [attrs]}]
  (->> attrs
       (filter #(str/starts-with? (name (key %)) "on"))
       (map val)))

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

(defn gen-props [coll]
  (let [props-str
        (->> (flatten coll) 
             (map #(format "    @FXML\n    public %s %s;\n\n"
                           (name (:tag %)) (get-in % [:attrs :fx:id])))
             (str/join ""))]
    (debug (type props-str))
    props-str))

(defn gen-handlers [coll clj-ns]
  (->> (flatten coll) 
       (map #(format "    @FXML\n    void %s(ActionEvent event) {\n        Clojure.var(\"%s\", \"%s\").invoke(this, event);\n    }\n\n"
                     (subs % 1) clj-ns (csk/->kebab-case (subs % 1))))
       (str/join "")))

(defn gen-initializer [cns cfn]
  (format "    @FXML
    void initialize() {
        Clojure.var(\"%s\", \"%s\").invoke(this);
    }" cns cfn))

(defn gen-fx-controller [fxmlzip fxmlpath [clj-ns clj-fn]]
  (let [clazz (get-controller-class fxmlzip)
        [pkg classname] (reverse (map str/reverse (str/split (str/reverse clazz) #"\." 2)))
        fxid-elems (get-fxid-elems fxmlzip)
        handler-fns (get-handler-fns fxmlzip)]
    (debug "fxid-elems:" (pr-str fxid-elems))
    (debug "handler-fns:" (pr-str handler-fns))
    (str (format "package %s;\n\n" pkg)
         stockimports
         (get-imports fxmlpath)
         (format "\n\npublic class %s {\n\n" classname)
         (gen-props fxid-elems)
         (gen-handlers handler-fns clj-ns)
         (gen-initializer clj-ns clj-fn)
         "\n}")))

;; Plumber

(defn gen-fx-controller-class [fxmlpath clj-fn]
  (let [clj-fn (if (symbol? clj-fn)
                 (str (namespace clj-fn) "/" (name clj-fn))
                 clj-fn)
        fxmlzip (zip-tree-seq (xml/parse (io/input-stream fxmlpath)))
        clazz (get-controller-class fxmlzip)
        [pkg classname] (reverse (map str/reverse (str/split (str/reverse clazz) #"\." 2)))
        cljvec (str/split clj-fn #"/")] 
    (makeclass pkg classname (gen-fx-controller fxmlzip fxmlpath cljvec))))
