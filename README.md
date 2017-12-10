[![License](//img.shields.io/badge/license-EPLv1-blue.svg?style=flat)](https://opensource.org/licenses/EPL-1.0)
[![Clojars](//img.shields.io/badge/clojars-0.4.0-blue.svg?style=flat)](https://clojars.org/clojurefx/versions/0.4.0)
[![Gratipay](//img.shields.io/gratipay/zilti.svg?style=flat)](//gratipay.com/zilti)
[![Flattr this](//api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=zilti&url=https%3A%2F%2Fbitbucket.org%2Fzilti%2Fclojurefx)

# ClojureFX

```clojure
[clojurefx "0.4.0"]
```

A Clojure extension to make working with [JavaFX](http://download.java.net/jdk8/jfxdocs/index.html) simpler and more idiomatic. It allows you to naturally work with stock JavaFX components through use of extended protocols. Should a feature be missing you can easily extend ClojureFX in your own codebase or just fall back to standard JavaFX methods.

## Features

Take a look at the [ClojureFX manual](http://lyrion.ch/share/clojurefx.html).

* FXML loading and scripting
* Automatic FXML controller generation
* Declarative EDN GUI structure compilation
* Simplified event binding (bind a Clojure function to an event trigger)

### Declarative UI programming

```clojure
(def superbutton (compile [Button {:text "Close"
                                   :action #'close-handler}]))

(compile [VBox {:id "TopLevelVBox"
                :children [Label {:text "Hi!"}
                           Label {:text "I'm ClojureFX!"}
                           HBox {:id "HorizontalBox"
                           :children [Button {:text "OK"}
						              superbutton]}]}])
```
