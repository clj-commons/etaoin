(ns etaoin.element)

(defrecord Element [_id]
  Object
  (toString [_] _id))

(defn el? [el]
  (instance? Element el))

(defn el [id]
  (->Element id))

(defn els [ids]
  (mapv el ids))
