(ns clojurefx.controllergen
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [taoensso.timbre :as timbre]))
(timbre/refer-timbre)

(def xmlzip (zip/xml-zip (xml/parse "/Users/danielziltener/projects/lizenztool/resources/fxml/mainwindow.fxml")))

(defn get-fxid-elems
  ([ziptree] (get-fxid-elems ziptree []))
  ([ziptree elems]
   (cond
     (zip/end? ziptree) (do (debug "End reached, returning.") elems)
     (contains? (:attrs (zip/node ziptree)) :fx:id) (do (debug "Found a match!\n" (zip/node ziptree))
                                               (recur (zip/next ziptree) (conj elems (zip/node ziptree))))
     :else (do (debug "No match, continuing:" (zip/node ziptree)) (recur (zip/next ziptree) elems)))))
