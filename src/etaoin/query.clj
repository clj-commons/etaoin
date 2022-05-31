(ns etaoin.query
  "A module to deal with querying elements."
  (:require
   [etaoin.impl.util :as util]
   [etaoin.impl.xpath :as xpath]))

;; todo duplicates with api.clj
(def locator-xpath "xpath")
(def locator-css "css selector")

(defrecord Query [locator term])

(defn query [locator term]
  (->Query locator term))

(defn query-xpath [term]
  (query locator-xpath term))

(defn query-css [term]
  (query locator-css term))

(defmulti to-query
  (fn [_driver q]
    (type q)))

(defmethod to-query clojure.lang.Keyword
  [driver q]
  (to-query driver {:id q}))

(defmethod to-query java.lang.String
  [driver q]
  (query (:locator driver) q))

(defmethod to-query clojure.lang.IPersistentMap
  [_driver {:keys [xpath css] :as q}]
  (cond
    xpath (query-xpath xpath)
    css   (query-css css)
    :else (query-xpath (xpath/expand q))))

(defmethod to-query :default
  [_driver q]
  (util/error "Wrong query: %s" q))

(defn expand [driver q]
  (let [query (to-query driver q)]
    [(:locator query) (:term query)]))
