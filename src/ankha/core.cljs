(ns ankha.core
  (:refer-clojure :exclude [empty? inspect])
  (:require
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [clojure.string :as string]
   [cljs.reader :as reader]
   [goog.object :as object]
   [goog.crypt :as crypt]
   goog.crypt.Md5))

(enable-console-print!)

;; ---------------------------------------------------------------------
;; Protocols

(defprotocol IInspect
  (-inspect [this]
    "Return a React or Om compatible representation of this."))

;; ---------------------------------------------------------------------
;; Utilities

(defn- empty?
  "Return true if x is an empty js/Object or empty Clojure collection."
  [x]
  (if (object? x)
    (object/isEmpty x)
    (clojure.core/empty? x)))

(defn- record?
  "Return true if x satisfies IRecord, false otherwise."
  [x]
  (satisfies? IRecord x))

(defn record-name
  "Return the name of a Record type."
  [r]
  (let [s (pr-str r)]
    (subs s 0 (.indexOf s "{"))))

(defn record-opener
  "Return an opener for a Record type."
  [r]
  (str (record-name r) "{"))

(defn hash-key [data]
  (let [d (goog.crypt.Md5.)]
    (.update d (crypt/stringToByteArray (str data)))
    (crypt/byteArrayToHex (.digest d))))

;; ---------------------------------------------------------------------
;; View helpers

(declare collection-view)

(defn literal [class x]
  (dom/span #js {:className class :key x}
    (pr-str x)))

(defn coll-view [data opener closer class]
  (om/build collection-view data
    {:opts {:opener opener :closer closer :class class}}))

(defn inspect [x]
  (cond
    (satisfies? IInspect x)
    (-inspect x)
    (fn? x)
    (literal "function" x)
    :else
    (literal "literal" x)))

(defn associative->dom
  [data {:keys [entry-class key-class val-class]}]
  (into-array
    (for [[k v] data]
      (dom/li #js {:key (hash-key [k v])}
        (dom/div #js {:className (str "entry " entry-class)
                      :style #js {:position "relative"}}
          (dom/span #js {:className (str "key " key-class)
                         :style #js {:display "inline-block"
                                     :verticalAlign "top"}}
            (inspect k))
          (dom/span #js {:style #js {:display "inline-block"
                                     :width "1em"}})
          (dom/span #js {:className (str "val " val-class)
                         :style #js {:display "inline-block"
                                     :verticalAlign "top"}}
            (inspect v)))))))

(defn sequential->dom [data owner {:keys [page-item-count]
                                   :or {page-item-count 10}}]
  (reify
    om/IInitState
    (init-state [_]
      {:page 0})
    om/IRenderState
    (render-state [_ {:keys [page]}]
      (let [button-style {:display "inline-block"
                          :verticalAlign "top"
                          :border "none"
                          :background "none"
                          :cursor "pointer"
                          :outline "none"
                          :fontWeight "bold"
                          :padding "0"}]
        (dom/div
         nil
         (dom/button
          #js {:onClick
               (fn [_]
                 (om/update-state!
                  owner :page
                  (fn [page-number]
                    (if-not (zero? page-number)
                      (dec page-number)
                      page-number))))
               :style (clj->js
                       (assoc button-style
                         :opacity (if (zero? page) "0.3" "1.0")))}
          "<<")
         (dom/span nil page)
         (let [last-page? (fn [p cnt] (> (inc (* (inc p) page-item-count))
                                         cnt))]
           (dom/button
            #js {:onClick
                 (fn [_]
                   (om/update-state!
                    owner :page
                    (fn [page-number]
                      (if-not (last-page? page-number (count @data))
                        (inc page-number)
                        page-number))))
                 :style (clj->js
                         (assoc button-style
                           :opacity (if (last-page? page (count data))
                                      "0.3"
                                      "1.0")))}
            ">>"))
         (dom/span nil (str "(" (count data) ")"))

         (let [page-data (->> data
                              (drop (* page page-item-count))
                              (take page-item-count))]
           (into-array
            (for [[i x :as pair] (map-indexed vector page-data)]
              (dom/li #js {:className "entry"
                           :key (hash-key pair)}
                      (inspect x))))))))))

(defn coll->dom [data]
  (cond
   (map? data)
   (associative->dom data {:entry-class "map-entry"
                           :key-class "map-key"
                           :val-class "map-val"})
   (object? data)
   (let [;; Avoid zipmap to preserve key order.
         ks (object/getKeys data)
         vs (object/getValues data)
         m (map vector ks vs)]
     (associative->dom m {:entry-class "object-entry"
                          :key-class "object-key"
                          :val-class "object-val"}))
   :else
   (om/build sequential->dom data)))

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

(defn- edit-button [owner {:keys [disable? save-editor open-editor]}]
  (dom/button #js {:className "edit-button"
                   :disabled disable?
                   :onClick
                   (fn [_]
                     (if (om/get-state owner :editing?)
                       (save-editor)
                       (open-editor)))
                   :style #js {:display "inline-block"
                               :verticalAlign "top"
                               :border "none"
                               :background "none"
                               :cursor "pointer"
                               :outline "none"
                               :fontWeight "bold"
                               :padding "0"
                               :opacity (if disable? "0.5" "1.0")}}
              (if (om/get-state owner :editing?) "Save" "Edit")))

(defn enter-key? [e]
  (= 13 (.-keyCode e)))

(defn escape-key? [e]
  (= 27 (.-keyCode e)))

(defn- editor [owner {:keys [value save-editor cancel-editor error-message]}]
  (dom/div #js {:style #js {:display "inline"}}
           (dom/textarea #js {:className "editor"
                              :ref "editor"
                              :style #js {:display "inline-block"}
                              :value value
                              :onKeyPress (fn [e]
                                            (when (enter-key? e)
                                              (.preventDefault e)))
                              :onKeyUp (fn [e]
                                         (cond
                                           (enter-key? e) (save-editor)
                                           (escape-key? e) (cancel-editor)))
                              :onChange (fn [e] (om/set-state! owner :edited-data (.. e -target -value)))
                              :onBlur save-editor})
           (when error-message
             (dom/span #js {:className "error" :style #js {:vertical-align "top"}}
                       error-message))))

;; ---------------------------------------------------------------------
;; Main component

(defn collection-view
  [data owner {:keys [class opener closer] :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:edited-data (pr-str data)
       :editing? (boolean (:editing? opts))
       :open-editor (fn [] (om/update-state! owner (fn [s] (merge s {:editing-error-message nil :editing? true :edited-data (pr-str @data)}))))
       :save-editor (fn []
                      (try
                        (let [new-data (reader/read-string (om/get-state owner :edited-data))]
                          ;; if the new data is the same as the old just stop editing
                          ;; if not, set data to new data, which will cause this component to re-mount
                          ;; this is a little funky but seems necessary to avoid trying to update an unmounted component
                          (if (= new-data @data)
                            (om/set-state! owner :editing? false)
                            (om/update! data new-data)))
                        (catch js/Error e
                          (om/set-state! owner :editing-error-message (.-message e)))))
       :cancel-editor (fn [] (om/set-state! owner :editing? false))
       :vacant? (empty? data)
       :open? (and (not (false? (:open? opts)))
                   (not (empty? data)))})

    om/IRenderState
    (render-state [_ {:keys [open? vacant? editing? edited-data editing-error-message open-editor save-editor cancel-editor]}]
      (dom/div #js {:className class}
        (toggle-button owner {:disable? vacant?})

        (when open?
         (edit-button owner {:open-editor open-editor :save-editor save-editor}))

        (when (and open? editing?)
          (editor owner {:value edited-data :error-message editing-error-message
                         :save-editor save-editor :cancel-editor cancel-editor}))

        (dom/span #js {:className "opener"
                       :style #js {:display (if (and open? editing?) "none" "inline-block")}}
          opener)

        (dom/ul #js {:className "values"
                     :style #js {:display (if (and open? (not editing?)) "block" "none")
                                 :listStyleType "none"
                                 :margin "0"}}
          (coll->dom data))

        (dom/span #js {:className "ellipsis"
                       :style #js {:display (if (or open? vacant?)
                                              "none"
                                              "inline")}}
          "…")

        (dom/span #js {:className "closer"
                       :style #js {:display (if open?
                                              (if editing?
                                                "none"
                                                "block")
                                              "inline-block")}}
          closer)))

    om/IDidUpdate
    (did-update [this prev-props prev-state]
      (when (om/get-state owner :editing?)
        (.focus (om/get-node owner "editor"))))))

(defn inspector
  ([data owner]
     (inspector data owner {:opts {:class "inspector"}}))
  ([data owner {:keys [class] :or {class "inspector"} :as opts}]
     (reify
       om/IRender
       (render [_]
         (dom/div #js {:className class
                       :style #js {:fontFamily "monospace"
                                   :whiteSpace "pre-wrap"
                                   :width "100%"
                                   :overflowX "scroll"}}
           (inspect data))))))

;; ---------------------------------------------------------------------
;; IInspect Implementation

(extend-protocol IInspect
  Keyword
  (-inspect [this] (literal "keyword" this))

  Symbol
  (-inspect [this] (literal "symbol" this))

  PersistentArrayMap
  (-inspect [this]
    (coll-view this "{" "}" "map persistent-array-map"))

  PersistentHashMap
  (-inspect [this]
    (coll-view this "{" "}" "map persistent-hash-map"))

  PersistentVector
  (-inspect [this] (coll-view this "[" "]" "vector"))

  PersistentHashSet
  (-inspect [this] (coll-view this "#{" "}" "set persistent-hash-set"))

  PersistentTreeSet
  (-inspect [this] (coll-view this "#{" "}" "set persistent-tree-set"))

  List
  (-inspect [this] (coll-view this "(" ")" "list"))

  LazySeq
  (-inspect [this] (coll-view this "(" ")" "seq lazy-seq"))

  KeySeq
  (-inspect [this] (coll-view this "(" ")" "seq key-seq"))

  ValSeq
  (-inspect [this] (coll-view this "(" ")" "seq val-seq"))

  PersistentArrayMapSeq
  (-inspect [this] (coll-view this "(" ")" "seq persistent-array-map-seq"))

  Range
  (-inspect [this] (coll-view this "(" ")" "seq range"))

  UUID
  (-inspect [this] (literal "uuid" this))

  om/IndexedCursor
  (-inspect [this]
    (coll-view this "[" "]" "vector indexed-cursor"))

  om/MapCursor
  (-inspect [this]
    (if (record? (om/value this))
      (coll-view this (record-opener this) "}" "record map-cursor")
      (coll-view this "{" "}" "map map-cursor")))

  js/RegExp
  (-inspect [this] (literal "regexp" this))

  js/Date
  (-inspect [this] (literal "date" this))

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
