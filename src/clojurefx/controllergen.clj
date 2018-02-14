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

;; ;; Parser

(def imports (list clojure.java.api.Clojure clojure.lang.IFn java.net.URL java.util.ResourceBundle javafx.event.ActionEvent javafx.fxml.FXML))

(defn build-imports [filename] 
  (->> (slurp filename)
       str/split-lines
       (filter #(str/starts-with? % "<?import"))
       (map #(str/replace % #"<\?import " ""))
       (map #(str/replace % #"\?>" ""))
       (map #(Class/forName %))
       (reduce conj imports)))

(defn qualify-class [imports class-str]
  (let [classname (first (filter #(= class-str (last (str/split (pr-str %) #"\."))) imports))
        classfull (str/replace classname #"\." "/")
        classreal (str/split classfull #"\s")]
    (-> (filter #(= class-str (last (str/split (pr-str %) #"\."))) imports)
        first
        (str/replace #"\." "/")
        (str/split #"\s")
        last)))

(defn init-class [pkg classname import-classes]
  {:post [(s/valid? (partial instance? org.objectweb.asm.ClassWriter) %)]}
  (debug (str (str/replace pkg #"\." "/") "/" classname))

  (let [cw (new org.objectweb.asm.ClassWriter 0)
        clazz (.visit cw Opcodes/V1_8
                      (+ Opcodes/ACC_PUBLIC Opcodes/ACC_SUPER)
                      (str (str/replace pkg #"\." "/") "/" classname)
                      nil
                      "java/lang/Object"
                      nil)
        resources_fv (.visitField cw Opcodes/ACC_PRIVATE
                                  "resources" 
                                  (str "L" (qualify-class import-classes "ResourceBundle") ";")
                                  nil
                                  nil)
        url_fv (.visitField cw Opcodes/ACC_PRIVATE
                            "location"
                            (str "L" (qualify-class import-classes "URL") ";")
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

(defn gen-props [cw [entry & coll] import-classes]
  {:post [(s/valid? (partial instance? org.objectweb.asm.ClassWriter) %)]}
  (if-not (nil? entry)
    (let [fv (.visitField cw Opcodes/ACC_PUBLIC
                          (get-in entry [:attrs :fx:id])
                          (str "L" (qualify-class import-classes (name (:tag entry))) ";")
                          nil nil)]
      (debug "Generating" (get-in entry [:attrs :fx:id]) "with type" (qualify-class import-classes (name (:tag entry))))
      (-> (.visitAnnotation fv "Ljavafx/fxml/FXML;" true)
          .visitEnd)
      (.visitEnd fv)
      (gen-props cw coll import-classes))
    cw))

(defn gen-handlers [cw [entry & coll] clj-ns]
  {:post [(s/valid? (partial instance? org.objectweb.asm.ClassWriter) %)]}
  (if-not (nil? entry)
    (let [mv (.visitMethod cw 0
                           (subs entry 1)
                           "(Ljavafx/event/Event;)V"
                           nil nil)]
      (debug "Generating handler" (subs entry 1) "for" entry)
      (-> (.visitAnnotation mv "Ljavafx/fxml/FXML;" true)
          .visitEnd)
      (.visitCode mv)
      (.visitLdcInsn mv clj-ns)
      (.visitLdcInsn mv (csk/->kebab-case (subs entry 1)))
      (.visitMethodInsn mv Opcodes/INVOKESTATIC "clojure/java/api/Clojure" "var" "(Ljava/lang/Object;Ljava/lang/Object;)Lclojure/lang/IFn;" false)
      (.visitVarInsn mv Opcodes/ALOAD 0)
      (.visitVarInsn mv Opcodes/ALOAD 1)
      (.visitMethodInsn mv Opcodes/INVOKEINTERFACE "clojure/lang/IFn" "invoke" "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;" true)
      (.visitInsn mv Opcodes/POP)
      (.visitInsn mv Opcodes/RETURN)
      (.visitMaxs mv 3 2)
      (.visitEnd mv)
      
      (gen-handlers cw coll clj-ns))
    cw))

(defn gen-initializer [cw [clj-ns clj-fn]]
  {:post [(s/valid? (partial instance? org.objectweb.asm.ClassWriter) %)]}
  (debug clj-ns clj-fn)
  (let [mv (.visitMethod cw Opcodes/ACC_PUBLIC "initialize" "()V" nil nil)
        init-mv (.visitMethod cw Opcodes/ACC_PUBLIC "<init>" "()V" nil nil)]
    (.visitCode mv)
    (-> (.visitAnnotation mv "Ljavafx/fxml/FXML;" true)
        .visitEnd)
    (.visitLdcInsn mv clj-ns)
    (.visitLdcInsn mv clj-fn)
    (.visitMethodInsn mv Opcodes/INVOKESTATIC "clojure/java/api/Clojure" "var" "(Ljava/lang/Object;Ljava/lang/Object;)Lclojure/lang/IFn;" false)
    (.visitVarInsn mv Opcodes/ALOAD 0) 
    (.visitMethodInsn mv Opcodes/INVOKEINTERFACE "clojure/lang/IFn" "invoke" "(Ljava/lang/Object;)Ljava/lang/Object;" true)
    (.visitInsn mv Opcodes/POP)
    (.visitInsn mv Opcodes/RETURN)
    (.visitMaxs mv 2 1)
    (.visitEnd mv)

    (.visitCode init-mv)
    (.visitVarInsn init-mv Opcodes/ALOAD 0)
    (.visitMethodInsn init-mv Opcodes/INVOKESPECIAL "java/lang/Object" "<init>" "()V" false)
    (.visitInsn init-mv Opcodes/RETURN)
    (.visitMaxs init-mv 1 1)
    (.visitEnd init-mv)
    
    cw))

(defn gen-fx-controller [fxmlzip fxmlpath [clj-ns clj-fn] [pkg classname]]
  (let [fxid-elems (get-fxid-elems fxmlzip)
        handler-fns (get-handler-fns fxmlzip)
        import-classes (build-imports fxmlpath)
        inited-class (init-class pkg classname import-classes)
        propped-class (gen-props inited-class fxid-elems import-classes)
        initializer-class (gen-initializer propped-class [clj-ns clj-fn])
        handled-class (gen-handlers initializer-class handler-fns clj-ns)]
    (debug (pr-str handler-fns))
    (.visitEnd handled-class)
    (.toByteArray handled-class)))

;; ;; Plumber

(defn define-class [name bytes]
  (let [cl (.getClassLoader clojure.lang.RT)
        method (.getDeclaredMethod java.lang.ClassLoader "defineClass"
                                   (into-array [String (Class/forName "[B") (Integer/TYPE) (Integer/TYPE)]))]
    (try
      (.setAccessible method true)
      (.invoke method cl (into-array Object [name bytes (int 0) (int (count bytes))]))
      (finally (.setAccessible method false)))))

(defn gen-fx-controller-class [fxmlpath clj-fn] 
  (let [clj-fn ^String (if (symbol? clj-fn)
                         (str (namespace clj-fn) "/" (name clj-fn))
                         clj-fn)
        fxmlzip (zip-tree-seq (xml/parse (io/input-stream fxmlpath)))
        clazz (get-controller-class fxmlzip)
        [pkg classname] (reverse (map str/reverse (str/split (str/reverse clazz) #"\." 2))) 
        cljvec (str/split clj-fn #"/")]
    (try
      (Class/forName (str pkg "." classname))
      (catch Exception e
        (define-class (str pkg "." classname)
          (gen-fx-controller fxmlzip fxmlpath cljvec [pkg classname]))))))
