;; Thanks to https://gist.github.com/wilkerlucio/db54dc83a9664124f3febf6356f04509
(ns helper.natural-sort
  (:refer-clojure :exclude [sort sort-by])
  (:require [clojure.string]))

(defn vector-compare [[value1 & rest1] [value2 & rest2]]
  (let [result (compare value1 value2)]
    (cond
      (not (zero? result)) result
      (nil? value1) 0
      :else (recur rest1 rest2))))

(defn prepare-string [s]
  (let [s (or s "")
        parts (vec (clojure.string/split s #"\d+"))
        numbers (->> (re-seq #"\d+" s)
                     (map parse-long)
                     (vec))]
    (vec (interleave (conj parts "") (conj numbers -1)))))

(defn natural-compare [a b]
  (vector-compare (prepare-string a)
                  (prepare-string b)))

(defn sort [coll] (clojure.core/sort natural-compare coll))

(defn sort-by [keyfn coll]
  (clojure.core/sort-by keyfn natural-compare coll))
