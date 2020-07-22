(ns etaoin.legacy
  "A namespace that injects some missing functions
  for legacy Clojure versions.

  Clojure String source code:
  https://github.com/clojure/clojure/blob/master/src/clj/clojure/string.clj

  The `with-ns` macro was taken from SO:
  https://stackoverflow.com/questions/27343707"
  (:require [clojure.string]))

(defn intern-fq [fqsym val]
  (when-not (resolve fqsym)
    (intern (-> fqsym namespace symbol)
            (-> fqsym name symbol)
            val)))

(intern-fq 'clojure.string/starts-with?
           (fn [^CharSequence s ^String substr]
             (.startsWith (.toString s) substr)))

(intern-fq 'clojure.string/ends-with?
           (fn [^CharSequence s ^String substr]
             (.endsWith (.toString s) substr)))

(intern-fq 'clojure.string/includes?
           (fn [^CharSequence s ^CharSequence substr]
             (.contains (.toString s) substr)))
