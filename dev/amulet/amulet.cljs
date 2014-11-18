(ns amulet
  (:require
   [figwheel.client :as fw :include-macros true]
   [weasel.repl :as ws-repl]
   [ankha.core :as ankha]
   [om.core :as om :include-macros true]))

(enable-console-print!)

(ws-repl/connect "ws://localhost:9001" :verbose true)

(fw/watch-and-reload
  ;; :websocket-url "ws:localhost:3449/figwheel-ws" default
  :jsload-callback
  (fn []
    (js/console.log (str (gensym "R")))))

(om/root
 ankha/inspector
 {:origin {:x 0 :y 0}
  :a (atom {:a 1})
  :b (atom 1)
  :c #js {:a 1}}
 {:target (js/document.getElementById "ankha")})
