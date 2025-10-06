(ns etaoin.query
  "A module to deal with querying elements.

  This feels like a internal namespace.
  Why do folks need to use this directly?
  Maybe they are extending defmulti with more conversions?"
  (:require
   [clj-commons.slingshot :refer [throw+]]
   [etaoin.impl.xpath :as xpath]))

(set! *warn-on-reflection* true)

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
  "Return query for `q` for `driver`.

  Conversion depends on type of `q`.
  - keyword -> converts to search on element id for q`
  - string -> converts to xpath query (or css if default is changed)
  - map -> converts to :xpath or :css query or map query"
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
  (throw+ {:type :etaoin/argument
           :message "Unsupported query argument type"
           :q q}))

(defn expand
  "Return expanded query `q` for `driver`."
  [driver q]
  (let [query (to-query driver q)]
    [(:locator query) (:term query)]))
