classes:
	mkdir classes

classes/clojurefx/ApplicationInitializer.class: src/clojurefx/ApplicationInitializer.java
	javac -classpath $$(clojure -Spath) -d classes src/clojurefx/ApplicationInitializer.java

classes/%__init.class: $(wildcard src/%.*)
	mkdir -p $(@D)
	clojure -e "(compile '`echo "$*" | sed 's/\//\./g' | sed 's/_/-/g'`)"

pom.xml: deps.edn
	clojure -Spom

.PHONY: clean
clean:
	rm -rf classes target pom.xml *.jar

.PHONY: test
test: classes/clojurefx/ApplicationInitializer.class
	clojure -A:test
