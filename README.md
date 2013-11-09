clojurefx
=========

A Clojure JavaFX wrapper. [Documentation and code can be found here](http://zilti.github.io/clojurefx).

Note that this library is brand-new and lacks a lot of features; I'm heavily working at it. Stay tuned!

Lots of neat features will follow soon!

Installation: `[clojurefx "0.0.7"]`

API
---

### Creating
The no.1 macro for creation is the fx-macro. Additionally, there's the deffx-macro which does the same
as defn, except that it binds the new JavaFX object to a symbol instead of a function.

```clojure
(deffx scn scene :width 800 :height 600 :root rt)
```

* Instead of camelCase, the normal clojure dashes are used for the class names: button, check-box, v-box, ...
* The keys for the options are the normal setters, without the "set" word at the beginning: :title :scene :root and so on.
* Constructor arguments also are given that way, e.g. the :width, :height and :root in the example above.
* You can directly add property bindings. Give them in a map with the :bind key, example:
  ```clojure
  (fx stage :title "Hello ClojureFX!" :bind {:title title-atom})
  ```

* You can do the same for action listeners, just use the :listen key instead:
  ```clojure
  (fx button :text "Hide the window" :listen {:onAction (fn [_] (run-now (.hide stg)))})
  ```

* And you can do it for child elements. Use the key `content` or `children` (equivalent). The value of this key must be a datastructure a function given to `swap-content!` would return.

### Modifying
#### Child elements
Besides the possibility to use the normal Java methods, you can use the `swap-content!` multimethod to modify child-elements.
The return value of the function you provide becomes the new content of the node.

This works for all layout classes as well as everything with child elements, like combo-box, menu, split-pane and so on.

Note that for split-pane and table-view you get maps; See the source code for details.
#### Properties
Currently the only way to modify properties this library provides is using the `bind-property!` function.
It expects an atom it will listen to, and whenever you change the atom value, this value will be propagated to the property.

Example:

```clojure
(bind-property! scn :title title-atom)
```

It is also possible to bind multiple properties at once in bind-property!. Just use additional named arguments:

```clojure
(bind-property! scn :width width-atom :height height-atom)
```

Other STM objects will follow.

### Acting
Event handling is really simple. All you need is the action name and a function. Example:

```clojure
(set-listener! btn :onAction [x] (run-now (.hide stg)))
```

Note that your function will get a map. See the source code for further details.