(ns example
  (:require [ankha.core :as ankha]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.reader :as reader]))

(defrecord Person [first-name last-name])

(reader/register-tag-parser! "example.Person" map->Person)

(js/document.body.appendChild
 (doto (js/document.createElement "div")
   (.setAttribute "id" "example")))

(om/root
 ankha/inspector
 (atom {:users [{:user (map->Person {:first-name "Bill"
                                     :last-name "Braskey"})
                 :tagline "Best damn salesman in the office!"}]
        :set (set "abc")
        :arrs [#js [1 2 3]
               #js [4 5 6]]
        :objs [#js {:foo "bar"
                    :baz "quux"}
               #js {:weeble "bar"
                    :baz "quux"}]})
 {:target (js/document.getElementById "example")})

