(ns ankha.core
  (:refer-clojure :exclude [empty? inspect])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]
            [goog.object :as object]))

(enable-console-print!)

;; ---------------------------------------------------------------------
;; View helpers

(defprotocol IInspect
  (-inspect [this]))

;; ---------------------------------------------------------------------
;; Utilities

(defn- empty? [x]
  (if (object? x)
    (object/isEmpty x)
    (clojure.core/empty? x)))

(defn- record? [x]
  (satisfies? IRecord x))

(defn record-name [r]
  (let [s (pr-str r)]
    (subs s 0 (.indexOf s "{"))))

(defn record-opener [r]
  (str (record-name r) "{"))

;; ---------------------------------------------------------------------
;; View helpers

(declare view)

;; TODO: Build a view for functions. 

(comment
  (def fn-re
    #"(\#<function.*?\([\s\S]*?\) *\{)([\s\S]*?)(\}>)")

  (defn function [cursor f]
    (let [s (pr-str f)
          [_ opener fn-body closer] (re-matches fn-re s)]))

  (defn function-name [f]
    (first (string/split (pr-str f) "{"))))

(defn- literal [class x]
  (dom/span #js {:className class :key x}
    (pr-str x)))

(defn coll-view [data opener closer class]
  (om/build view data
    {:opts {:opener opener :closer closer :class class}}))

(defn inspect* [x]
  (cond
    (satisfies? IInspect x)
    (-inspect x)
    (fn? x)
    (literal "function" x)
    :else
    (literal "literal" x)))

(defn- associative->dom
  [data {:keys [entry-class key-class val-class]}]
  (into-array
    (for [[k v] data]
      (dom/li #js {:key (str [k v])}
        (dom/div #js {:className (str "entry " entry-class)
                      :style #js {:position "relative"}}
          (dom/span #js {:className (str "key " key-class) 
                         :style #js {:display "inline-block"
                                     :verticalAlign "top"}}
            (inspect* k))
          (dom/span #js {:style #js {:display "inline-block"
                                     :width "1em"}})
          (dom/span #js {:className (str "val " val-class)
                         :style #js {:display "inline-block"
                                     :verticalAlign "top"}}
            (inspect* v)))))))

(defn- sequential->dom [data]
  (into-array
    (for [[i x :as pair] (map-indexed vector data)]
      (dom/li #js {:className "entry"
                   :key (str pair)}
        (inspect* x)))))

(defn- toggle-button [owner {:keys [disable?]}]
  (dom/button #js {:className "toggle-button"
                   :disabled disable?
                   :onClick
                   (fn [_]
                     (om/update-state! owner :open? not))
                   :style #js {:display "inline-block"
                               :verticalAlign "top"
                               :border "none"
                               :background "none"
                               :cursor "pointer"
                               :outline "none"
                               :fontWeight "bold"
                               :padding "0"
                               :opacity (if disable? "0.5" "1.0")}}
    (if (om/get-state owner :open?) "-" "+")))

(defn- view [data owner {:keys [class opener closer] :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:vacant? (empty? data)
       :open? (and (not (false? (:open? opts)))
                   (not (empty? data)))})

    om/IRenderState
    (render-state [_ {:keys [open? vacant?]}]
      (dom/div #js {:className class}
        (toggle-button owner {:disable? vacant?})
        
        (dom/span #js {:className "opener"
                       :style #js {:display "inline-block"}}
          opener)

        (dom/ul #js {:className "values"
                     :style #js {:display (if open? "block" "none")
                                 :listStyleType "none"
                                 :margin "0"}}
          (cond
            (map? data)
            (associative->dom data {:entry-class "map-entry"
                                    :key-class "map-key"
                                    :val-class "map-val"})
            (object? data)
            (let [m (zipmap (goog.object/getKeys data)
                            (goog.object/getValues data))]
              (associative->dom m {:entry-class "object-entry"
                                   :key-class "object-key"
                                   :val-class "object-val"}))
            :else
            (sequential->dom data)))

        (dom/span #js {:className "ellipsis"
                       :style #js {:display (if (or open? vacant?)
                                              "none"
                                              "inline")}}
          "…")

        (dom/span #js {:className "closer"
                       :style #js {:display (if open?
                                              "block"
                                              "inline-block")}}
          closer)))))

(defn inspector
  ([data owner]
     (inspector data owner {:opts {:class "inspector"}}))
  ([data owner {:keys [class] :or {class "inspector"} :as opts}]
     (reify
       om/IRender
       (render [_]
         (dom/div #js {:className "inspector"
                       :style #js {:fontFamily "monospace"
                                   :whiteSpace "pre-wrap"
                                   :width "100%"
                                   :overflowX "scroll"}}
           (inspect* data))))))

;; ---------------------------------------------------------------------
;; IInspect Implementation

(extend-protocol IInspect
  Keyword
  (-inspect [this] (literal "keyword" this))

  Symbol
  (-inspect [this] (literal "symbol" this))

  PersistentVector
  (-inspect [this] (coll-view this "[" "]" "vector"))

  PersistentHashSet
  (-inspect [this] (coll-view this "#{" "}" "set"))

  PersistentTreeSet
  (-inspect [this] (coll-view this "#{" "}" "set"))

  List
  (-inspect [this] (coll-view this "(" ")" "list"))

  LazySeq
  (-inspect [this] (coll-view this "(" ")" "lazy-seq"))

  KeySeq
  (-inspect [this] (coll-view this "(" ")" "val-seq"))

  ValSeq
  (-inspect [this] (coll-view this "(" ")" "key-seq"))

  PersistentArrayMapSeq
  (-inspect [this] (coll-view this "(" ")" "map-seq"))

  Range
  (-inspect [this] (coll-view this "(" ")" "range"))

  om/IndexedCursor
  (-inspect [this]
    (coll-view this "[" "]" "vector"))

  om/MapCursor
  (-inspect [this]
    (if (record? (om/value this))
      (coll-view this (record-opener this) "}" "record")
      (coll-view this "{" "}" "map")))

  js/RegExp
  (-inspect [this] (literal "regexp" this))

  function
  (-inspect [this] (literal "function" this)) 

  number
  (-inspect [this] (literal "number" this))

  string
  (-inspect [this] (literal "string" this))

  boolean
  (-inspect [this] (literal "boolean" this))

  array
  (-inspect [this] (coll-view this "#js [" "]" "array"))

  object
  (-inspect [this] (coll-view this "#js {" "}" "object"))

  nil
  (-inspect [this] (literal "nil" this)))
