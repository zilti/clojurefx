# ClojureFX

A Clojure wrapper to make working with [JavaFX](http://download.java.net/jdk8/jfxdocs/index.html) simpler and more idiomatic.

## Features

This is in a very early state, so there isn't much yet. Take a look at the [ClojureFX wiki](https://bitbucket.org/zilti/clojurefx/wiki/Home).

### Declarative UI programming

```clojure
(compile [VBox {:id "TopLevelVBox"
                :children [Label {:text "Hi!"}
                           Label {:text "I'm ClojureFX!"}
                           HBox {:id "HorizontalBox"
                                 :children [Button {:text "Alright."}]}]}])
```

## Donations
[![Gratipay](//img.shields.io/gratipay/zilti.svg)](//gratipay.com/zilti) [![Flattr this](//api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=zilti&url=https%3A%2F%2Fbitbucket.org%2Fzilti%2Fclojurefx)