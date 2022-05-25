(ns etaoin.impl.util)

(defmacro defmethods
  "Declares multimethods in batch. For each dispatch value from
  dispatch-vals, creates a new method."
  [multifn dispatch-vals & fn-tail]
  `(doseq [dispatch-val# ~dispatch-vals]
     (defmethod ~multifn dispatch-val# ~@fn-tail)))

;; essence only for linting
(defmacro with-tmp-file [prefix suffix bind & body]
  `(let [~bind "somepath"]
     ~@body))
