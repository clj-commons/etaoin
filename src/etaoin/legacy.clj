(ns etaoin.legacy
  "A namespace that injects some missing functions
  for legacy Clojure versions.

  Clojure String source code:
  https://github.com/clojure/clojure/blob/master/src/clj/clojure/string.clj

  The `with-ns` macro was taken from SO:
  https://stackoverflow.com/questions/27343707"
  (:require [clojure.string]))

(defmacro with-ns
  [ns & body]
  `(binding [*ns* (the-ns ~ns)]
     ~@(map (fn [form] `(eval '~form)) body)))

(when-not (resolve 'clojure.string/starts-with?)
  (with-ns 'clojure.string

    (defn starts-with?
      "True if s starts with substr."
      {:added "etaoin.legacy"}
      [^CharSequence s ^String substr]
      (.startsWith (.toString s) substr))))

(when-not (resolve 'clojure.string/ends-with?)
  (with-ns 'clojure.string

    (defn ends-with?
      "True if s ends with substr."
      {:added "etaoin.legacy"}
      [^CharSequence s ^String substr]
      (.endsWith (.toString s) substr))))

(when-not (resolve 'clojure.string/includes?)
  (with-ns 'clojure.string

    (defn includes?
      "True if s includes substr."
      {:added "etaoin.legacy"}
      [^CharSequence s ^CharSequence substr]
      (.contains (.toString s) substr))))
