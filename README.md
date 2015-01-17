# ClojureFX

[![Clojars Project](http://clojars.org/clojurefx/latest-version.svg)](http://clojars.org/clojurefx)

**Note: This is a complete rewrite, no code has been taken over from the old version on GitHub, and I'm taking a radically different approach.**

A Clojure extension to make working with [JavaFX](http://download.java.net/jdk8/jfxdocs/index.html) simpler and more idiomatic. It allows you to naturally work with stock JavaFX components through use of extended protocols. Should a feature be missing you can easily extend ClojureFX in your own codebase or just fall back to standard JavaFX methods.

## Features

This is in a very early state, so there isn't much yet. Take a look at the [ClojureFX wiki](https://bitbucket.org/zilti/clojurefx/wiki/Home) to find out what's done already.

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