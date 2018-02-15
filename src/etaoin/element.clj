(ns etaoin.element)

(defrecord Element [id])

(defn el? [el]
  (instance? Element el))

(defn el [id]
  (->Element id))

(defn id [el]
  (:id el))
