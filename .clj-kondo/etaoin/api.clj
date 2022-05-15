(ns etaoin.api)

(defmacro with-key-down
  [input key & body]
  `(-> ~input
       (add-key-down ~key)
       ~@body
       (add-key-up ~key)))

(defmacro with-pointer-btn-down
  [input button & body]
  `(-> ~input
       (add-pointer-down ~button)
       ~@body
       (add-pointer-up ~button)))

(defmacro with-pointer-left-btn-down
  [input & body]
  `(-> ~input
       add-pointer-down
       ~@body
       add-pointer-up))

;; simplified to remove with-pool which is of no consequence to linting
(defmacro with-driver
  [type opt bind & body]
  `(let [~bind (boot-driver ~type ~opt)]
     (try
       ~@body
       (finally
         (quit ~bind)))))

(defmacro with-firefox
  [opt bind & body]
  `(with-driver :firefox ~opt ~bind
     ~@body))

(defmacro with-chrome
  [opt bind & body]
  `(with-driver :chrome ~opt ~bind
     ~@body))
