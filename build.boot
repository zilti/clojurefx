                                        ;-*- mode: Clojure;-*-
(set-env! :resource-paths #{"src" "java"}
          :dependencies '[[org.clojure/clojure "1.7.0-alpha4"]
                          [com.taoensso/timbre "3.3.1" :exclusions [com.taoensso/carmine]]
                          [org.clojure/core.typed "0.2.77"]
                          [clojure-jsr-223 "0.1.0"]

                          [boot-deps "0.1.2" :scope "test"]
                          [midje "1.6.3" :scope "test"]
                          [zilti/boot-midje "0.1.1" :scope "test"]
                          [zilti/boot-typed "0.1.0" :scope "test"]])

(require '[zilti.boot-midje :refer [midje]]
         '[zilti.boot-typed :refer [typed]])

(def +version+ "0.0.5-SNAPSHOT")

(task-options!
 pom {:project 'clojurefx
      :version +version+
      :description "A Clojure JavaFX wrapper."
      :url "https://bitbucket.com/zilti/clojurefx"
      :scm {:url "https://bitbucket.com/zilti/clojurefx"}
      :license {:name "GNU Lesser General Public License 3.0"
                :url "http://www.gnu.org/licenses/lgpl-3.0.txt"}}
 midje {:test-paths #{"test"}
        :autotest true}
 typed {:namespaces #{'clojurefx.blargh}}
 repl {:server true})

(deftask develop
  []
  (comp (repl)
        (midje)
        (watch)
        (typed)))
