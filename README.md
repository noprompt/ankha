# Ankha

A generic data inspection component.

## Contents

* [Usage](#usage)
* [Example](#example)
* [Styling with CSS](#styling-with-css)
* [Extending the Inspector](#extending-the-inspector)

## Usage

Add `ankha` as a dependency to your `project.clj` file:

```clojure
[ankha "0.1.4"]
```

For the current edge version use:

```clojure
[ankha "0.1.5.1-8f1268"]
```

Require `ankha.core` somewhere in your project.

```clojure
(ns example
  (:require [om.core :as om :include-macros true]
            [ankha.core :as ankha]))
```

Next mount the `ankha/inspector` component with some data.

```clojure
(om/root
 ankha/inspector
 {:points [{:x 1 :y 0}
		   {:x 0 :y 1}
		   {:x -1 :y 0}
	       {:x 0 :y -1}]}
 {:target (js/document.getElementById "example")})
```

Refresh your browser to so see something similar to the following.

```clojure
-{
	:points -[
				-{
					:x 1
					:y 0
				}
				-{
					:x 0
					:y 1
				}
				-{
					:x -1
					:y 0
				}
				-{
					:x 0
					:y -1
				}
			]
}
```

The `ankha/inspector` is compatible with all Clojure data structures
including record types, JavaScript objects and arrays, and all
primitive types such as strings, numbers, and so forth.

## Example

To see an example with more data, clone the repository for this
project and from it's root run

```
$ lein build-example
```

then open `examples/a/index.html`.

## Styling with CSS

By default `ankha` only adds a minimum amount of styling to the
output. You can use the stylesheet in `examples/a/ankha.css` for a
better experience.

## Extending the Inspector

Ankha provides inspection for all Clojure data types (including
records), and JavaScript Arrays and Objects. You may want to provide
special handling for your own custom data type, override Ankha's
existing implementations, or even types you don't control. To do this
simply implement Ankha's `IInspect` protocol.

```clj
(extend-protocol ankha/IInspect
  User
  (-inspect [this]
    (dom/span #js {:className "record user"}
	  (dom/span nil "First name: " (:first-name this))
	  " "
	  (dom/span nil "Last name: " (:last-name this)))))
```

Your implementation must return a value capable of being rendered by
React or Om.

## Support

Right now only known to work with recent versions of Om `(>= 0.5)`
and ClojureScript `(>= 0.0-2156)`.
