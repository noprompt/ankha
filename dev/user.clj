(ns user
  (:require [cemerick.austin.repls :refer (browser-connected-repl-js)]
            [net.cgrand.enlive-html :as enlive]
            [compojure.route :refer  (resources)]
            [compojure.core :refer (GET defroutes)]
            [ring.adapter.jetty :as jetty]
            [clojure.java.io :as io]
            [cemerick.austin.repls]))

(enlive/deftemplate page
  (io/resource "public/index.html")
  []
  [:body]
  (enlive/append
   (enlive/html [:script (browser-connected-repl-js)])))

(defroutes site
  (GET "/" req (page))
  (resources "/"))

(def server nil)

(defn init []
  (when-not server
    (alter-var-root #'server (constantly (jetty/run-jetty #'site {:port 3000 :join? false})))))

(defn start []
  (when server
    (.start server)))

(defn stop []
  (when server
    (.stop server)))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (alter-var-root #'server (constantly nil))
  (init))

(defn browser-repl []
  (cemerick.austin.repls/cljs-repl
   (reset! cemerick.austin.repls/browser-repl-env
           (cemerick.austin/repl-env))))
