(ns taoensso.encore
  "I don't personally use clj-kondo, so these hooks are
  kindly authored and maintained by contributors.
  PRs very welcome! - Peter Taoussanis"
  (:refer-clojure :exclude [defonce])
  (:require
   [clj-kondo.hooks-api :as hooks]))

(defn defalias
  [{:keys [node]}]
  (let [[sym-raw src-raw] (rest (:children node))
        src (or src-raw sym-raw)
        sym (if src-raw sym-raw (symbol (name (hooks/sexpr src))))]
    {:node
     (with-meta
       (hooks/list-node
         [(hooks/token-node 'def)
          (hooks/token-node (hooks/sexpr sym))
          (hooks/token-node (hooks/sexpr src))])
       (meta src))}))

(defn defn-cached
  [{:keys [node]}]
  (let [[sym _opts binding-vec & body] (rest (:children node))]
    {:node
     (hooks/list-node
       (list
         (hooks/token-node 'def)
         sym
         (hooks/list-node
           (list*
             (hooks/token-node 'fn)
             binding-vec
             body))))}))

(defn defonce
  [{:keys [node]}]
  ;; args = [sym doc-string? attr-map? init-expr]
  (let [[sym & args] (rest (:children node))
        [doc-string args]    (if (and (hooks/string-node? (first args)) (next args)) [(hooks/sexpr (first args)) (next  args)] [nil        args])
        [attr-map init-expr] (if (and (hooks/map-node?    (first args)) (next args)) [(hooks/sexpr (first args)) (fnext args)] [nil (first args)])

        attr-map (if doc-string (assoc attr-map :doc doc-string) attr-map)
        sym+meta (if attr-map (with-meta sym attr-map) sym)
        rewritten
        (hooks/list-node
          [(hooks/token-node 'clojure.core/defonce)
           sym+meta
           init-expr])]

    {:node rewritten}))
