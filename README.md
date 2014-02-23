# Ankha

A generic data inspection component.

## Usage

Require `ankha.core` somewhere in your project. 

```clojure
(ns example 
  (:require [om.core :as om :include-macros true]
            [ankha.core :as ankha]))
```

Next mount the `ankha/inspector` component somewhere.

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
					:x1
					:y0
				}
				-{
					:x0
					:y1
				}
				-{
					:x-1
					:y0
				}
				-{
					:x0
					:y-1
				}
			]
}
```
