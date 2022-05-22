(ns download-deps
  (:require [clojure.edn :as edn]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

;; clojure has a -P command, but to bring down all deps we need to specify all aliases
;; bb deps will be brought down just from running bb (which assumedly is how this code is run)

(defn -main [& args]
  (when (main/doc-arg-opt args)
    (let [aliases (->> "deps.edn"
                       slurp
                       edn/read-string
                       :aliases
                       keys)]
      ;; one at a time because aliases with :replace-deps will... well... you know.
      (status/line :detail "Bring down default deps")
      (shell/clojure "-P")
      (doseq [a aliases]
        (status/line :detail "Bring down deps for alias: %s" a)
        (shell/clojure "-P" (str "-M" a))))))
