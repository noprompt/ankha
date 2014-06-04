(ns user
  (:require
   [weasel.repl.websocket]
   [cemerick.piggieback]))

(defn ws-repl []
  (cemerick.piggieback/cljs-repl
   :repl-env (weasel.repl.websocket/repl-env
              :ip "0.0.0.0" :port 9001)))
