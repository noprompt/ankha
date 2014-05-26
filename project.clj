(defproject ankha "0.1.3-SNAPSHOT"
  :description "A generic data inspection component for use with Om."
  :url "https://github.com/noprompt/ankha"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies
  [[org.clojure/clojure "1.5.1"]
   [org.clojure/clojurescript "0.0-2227" :scope "provided"]
   [om "0.6.2" :scope "provided"]]

  :plugins
  [[lein-cljsbuild "1.0.3"]]

  :source-paths ["src"]

  :profiles
  {:dev {:source-paths ["src" "dev"]
         :dependencies [[compojure "1.1.6"]
                        [ring "1.2.1"]
                        [enlive "1.1.5"]
                        [figwheel "0.1.3-SNAPSHOT"]]
         :plugins [[com.cemerick/austin "0.1.3"]
                   [lein-figwheel "0.1.3-SNAPSHOT"]]}
   :build {:source-paths ["src"]}}

  :aliases
  {"auto-build" ["with-profile" "build" "do"
                 "cljsbuild" "clean,"
                 "cljsbuild" "auto" "dev"]
   "build-example" ["with-profile" "build" "do"
                    "cljsbuild" "clean,"
                    "cljsbuild" "once" "a"]}

  :cljsbuild
  {:builds [{:id "dev"
             :source-paths ["src" "dev/brepl"]
             :compiler {:output-to "resources/public/js/ankha.js"
                        :output-dir "resources/public/js/out"
                        :source-map "resources/public/js/ankha.js.map"
                        :optimizations :none}}
            {:id "a"
             :source-paths ["src" "examples/a/src"]
             :compiler {:preamble ["react/react.min.js"]
                        :output-to "examples/a/ankha.js"
                        :output-dir "examples/a/out"
                        :source-map "examples/a/ankha.js.map"
                        :optimizations :whitespace}}]})
