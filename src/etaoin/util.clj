(ns etaoin.util)

(defn map-or-nil?
  [x]
  (or (map? x) (nil? x)))

(defn deep-merge
  [& vals]
  (if (every? map-or-nil? vals)
    (apply merge-with deep-merge vals)
    (if (every? sequential? vals)
      (apply concat vals)
      (last vals))))

(defmacro defmethods
  "Declares multimethods in batch. For each dispatch value from
  dispatch-vals, creates a new method."
  [multifn dispatch-vals & fn-tail]
  `(doseq [dispatch-val# ~dispatch-vals]
     (defmethod ~multifn dispatch-val# ~@fn-tail)))
