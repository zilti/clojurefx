package clojurefx;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Created by zilti on 07.01.2017.
 */
public class AppWrap extends Application {

    String ns, fn;

    public AppWrap(String ns, String fn) {
        super();
        this.ns = ns;
        this.fn = fn;
    }

    @Override
    public void start(Stage stage) throws Exception {
        IFn handler = Clojure.var(ns, fn);
        handler.invoke(stage);
    }
}
