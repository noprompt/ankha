(ns ankha.core
  (:refer-clojure :exclude [empty? inspect])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [clojure.string :as string]
            [goog.object :as object]))

(enable-console-print!)

;; ---------------------------------------------------------------------
;; Utilities

(defn- empty? [x]
  (cond
   (object? x) (object/isEmpty x)
   :else (clojure.core/empty? x)))

(defn- record? [x]
  (satisfies? IRecord x))

(defn- regex? [x]
  (instance? js/RegExp x))

(defn- record-name [r]
  (first (string/split (pr-str r) "{")))

;; ---------------------------------------------------------------------
;; View helpers

(declare view)

(defn- literal [class x]
  (dom/span #js {:className class
                 :key x}
            (pr-str x)))

(defn- collection [cursor coll class opener closer]
  (om/build view
            {:value coll}
            {:opts {:opener opener
                    :class class
                    :closer closer}}))

;; TODO: Build a view for functions. 

(comment
  (def fn-re
    #"(\#<function.*?\([\s\S]*?\) *\{)([\s\S]*?)(\}>)")

  (defn function [cursor f]
    (let [s (pr-str f)
          [_ opener fn-body closer] (re-matches fn-re s)]))

  (defn function-name [f]
    (first (string/split (pr-str f) "{"))))

(defn- inspect [data x]
  (cond
   (number? x)  (literal "number" x)
   (keyword? x) (literal "keyword" x)
   (symbol? x)  (literal "symbol" x)
   (string? x)  (literal "string" x)
   (true? x)    (literal "boolean" x)
   (false? x)   (literal "boolean" x)
   (nil? x)     (literal "nil" x)
   (fn? x)      (literal "function" x)
   (regex? x)   (literal "regex" x)
   (record? x)  (collection data x "record" (str (record-name x) "{") "}")
   (map? x)     (collection data x "map" "{" "}")
   (vector? x)  (collection data x "vector" "[" "]")
   (set? x)     (collection data x "set" "#{" "}")
   (seq? x)     (collection data x "seq " "(" ")")
   (object? x)  (collection data x "object" "#js {" "}")
   (array? x)   (collection data x "array" "#js [" "]")
   :else        (literal "literal" x)))

(defn- associative->dom
  [data kvs {:keys [entry-class key-class val-class]}]
  (into-array
   (for [[k v] kvs]
     (dom/li nil
             (dom/div #js {:className (str "entry "  entry-class)
                           :style #js {:position "relative"}}
                      (dom/span #js {:className (str "key " key-class) 
                                     :style #js {:display "inline-block"
                                                 :verticalAlign "top"}}
                                (inspect data (om/value k)))
                      (dom/span #js {:style #js {:display "inline-block"
                                                 :width "1em"}})
                      (dom/span #js {:className (str "val " val-class)
                                     :style #js {:display "inline-block"
                                                 :verticalAlign "top"}}
                                (inspect data (om/value v))))))))

(defn- map->dom [data m]
  (associative->dom data m {:entry-class "map-entry"
                            :key-class "map-key"
                            :val-class "map-val"}))

(defn- object->dom [data o]
  (as-> (zipmap (object/getKeys o) (object/getValues o)) _
        (associative->dom data _ {:entry-class "object-entry"
                                  :key-class "object-key"
                                  :val-class "object-val"})))

(defn- coll->dom [data v]
  (into-array
   (for [x v]
     (dom/li #js {:className "entry"}
             (inspect data (om/value x))))))

(defn- toggle-button [owner {:keys [disable?]}]
  (dom/button #js {:className "toggle-button"
                   :disabled disable?
                   :onClick (fn [_]
                              (->> (om/get-state owner :open?)
                                   (not)
                                   (om/set-state! owner :open?)))
                   :style #js {:display "inline-block"
                               :verticalAlign "top"
                               :border "none"
                               :background "none"
                               :cursor "pointer"
                               :outline "none"
                               :fontWeight "bold"
                               :padding "0"
                               :opacity (if disable?
                                          "0.5"
                                          "1.0")}}
              (if (om/get-state owner :open?)
                "-"
                "+")))

(defn- view [data owner {:keys [class opener closer] :as opts}]
  (let [value (:value data)
        value? (empty? value)
        open? #(om/get-state owner :open?)]
    (reify
      om/IInitState
      (init-state [_]
        {:open? (and (not (false? (:open? opts)))
                     (not value?))})

      om/IRender
      (render [_]
        (dom/div #js {:className class}
                 (toggle-button owner {:disable? value?})
                 
                 (dom/span #js {:className "opener"
                                :style #js {:display "inline-block"}}
                           opener)

                 (dom/ul #js {:className "values"
                              :style #js {:display (if (open?)
                                                     "block"
                                                     "none")
                                          :listStyleType "none"
                                          :margin "0"}}
                         (let [f (cond
                                  (map? value) map->dom
                                  (object? value) object->dom
                                  :else coll->dom)]
                           (f data value)))

                 (dom/span #js {:className "ellipsis"
                                :style #js {:display (cond
                                                      (open?) "none"
                                                      value? "none"
                                                      :else "inline")}}
                           "…")

                 (dom/span #js {:className "closer"
                                :style #js {:display (cond
                                                      (not value?) "inline-block"
                                                      (open?) "block"
                                                      :else "inline-block")}}
                           closer))))))

(defn inspector
  ([data owner]
     (inspector data owner {:opts {:class "inspector"}}))
  ([data owner opts]
     (let [{:keys [class] :or {class "inspector"}} opts]
       (reify
         om/IRender
         (render [_]
           (dom/div #js {:className "inspector"
                         :style #js {:fontFamily "monospace"
                                     :whiteSpace "pre-wrap"
                                     :width "100%"
                                     :overflowX "scroll"}}
                    (inspect data (om/value data))))))))
