[![License](//img.shields.io/badge/license-LGPL-blue.svg?style=flat)](https://www.gnu.org/licenses/lgpl-3.0.en.html#content)
[![Clojars](//img.shields.io/badge/clojars-0.0.30--SNAPSHOT-blue.svg?style=flat)](https://clojars.org/clojurefx/versions/0.0.30-SNAPSHOT)
[![Gratipay](//img.shields.io/gratipay/zilti.svg?style=flat)](//gratipay.com/zilti)
[![Flattr this](//api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=zilti&url=https%3A%2F%2Fbitbucket.org%2Fzilti%2Fclojurefx)

# ClojureFX

```clojure
[clojurefx "0.0.30-SNAPSHOT"]
```

**Note: This is a complete rewrite, no code has been taken over from the old version on GitHub, and I'm taking a radically different approach.**

A Clojure extension to make working with [JavaFX](http://download.java.net/jdk8/jfxdocs/index.html) simpler and more idiomatic. It allows you to naturally work with stock JavaFX components through use of extended protocols. Should a feature be missing you can easily extend ClojureFX in your own codebase or just fall back to standard JavaFX methods.

## Features

This is in a very early state, so there isn't much yet. Take a look at the [ClojureFX wiki](https://bitbucket.org/zilti/clojurefx/wiki/Home).

* Declarative EDN GUI structure compilation
* FXML loading and scripting
* Turn a scene graph into a flat id-node-map and/or get nodes by id out of a scene graph

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
