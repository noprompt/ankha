(require '[clojure.java.shell])

(defn project-version
  "Return the current version string."
  [base-version {release? :release?}]
  (if-not (true? release?)
    (let [last-commit (-> (clojure.java.shell/sh "git" "rev-parse" "HEAD")
                          (:out)
                          (.trim))
          revision (-> (clojure.java.shell/sh "git" (str "rev-list.." last-commit))
                       (:out)
                       (.. trim (split "\\n"))
                       (count))
          sha (subs last-commit 0 6)]
      (str base-version "." revision "-" sha))
    base-version))

(defproject ankha (project-version "0.1.5" {:release? false})
  :description "A generic data inspection component for use with Om."
  :url "https://github.com/noprompt/ankha"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies
  [[org.clojure/clojure "1.5.1"]
   [org.clojure/clojurescript "0.0-2227" :scope "provided"]
   [org.clojure/tools.reader "0.8.1"]
   [om "0.6.2" :scope "provided"]]

  :source-paths ["src"]

  :profiles
  {:dev {:source-paths ["src" "dev"]
         :dependencies [[weasel "0.2.0"]
                        [com.cemerick/piggieback "0.1.3"]
                        [figwheel "0.1.3-SNAPSHOT"]]
         :plugins [[com.cemerick/austin "0.1.3"]
                   [lein-figwheel "0.1.3-SNAPSHOT"]
                   [lein-cljsbuild "1.0.3"]]
         :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
   :build {:source-paths ["src"]}}

  :aliases
  {"build-example" ["with-profile" "build" "do"
                    "cljsbuild" "clean,"
                    "cljsbuild" "once" "a"]}

  :cljsbuild
  {:builds [{:id "dev"
             :source-paths ["src" "dev/amulet"]
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
