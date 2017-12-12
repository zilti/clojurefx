package clojurefx;

import javafx.application.Application;
import javafx.stage.Stage;
import clojure.lang.IFn;

public class ApplicationInitializer extends Application {

    private static IFn initfn;
    private static IFn startfn;
    private static IFn stopfn;

    @Override
    public void init() {
	initfn.invoke();
    }

    public void start(Stage stage) {
	startfn.invoke(stage);
    }

    public void stop() {
	stopfn.invoke();
    }
    
    public static void initApp(IFn initApp, IFn startApp, IFn stopApp) {
	initfn = initApp;
	startfn = startApp;
	stopfn = stopApp;
	launch();
    }
    
}
