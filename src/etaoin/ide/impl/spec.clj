(ns ^:no-doc etaoin.ide.impl.spec
  "
  Parsing IDE flow with spec.
  "
  (:require
   [clojure.spec.alpha :as s]))

(set! *warn-on-reflection* true)

(def control-flow-commands
  #{:do :times :while :forEach :if :elseIf :else :end :repeatIf})


(defn cmd? [cmd]
  (fn [command]
    (some-> command :command (= cmd))))


(s/def ::command-if
  (s/cat :if (s/cat :this (cmd? :if)
                    :branch ::commands)
         :else-if (s/* (s/cat :this (cmd? :elseIf)
                              :branch ::commands))
         :else (s/? (s/cat :this (cmd? :else)
                           :branch ::commands))
         :end (cmd? :end)))


(s/def ::command-times
  (s/cat :this (cmd? :times)
         :branch ::commands
         :end (cmd? :end)))


(s/def ::command-while
  (s/cat :this (cmd? :while)
         :branch ::commands
         :end (cmd? :end)))


(s/def ::command-do
  (s/cat :this (cmd? :do)
         :branch ::commands
         :repeat-if (cmd? :repeatIf)))


(s/def ::command-for-each
  (s/cat :this (cmd? :forEach)
         :branch ::commands
         :end (cmd? :end)))


(s/def ::cmd-with-open-window
  (fn [{:keys [opensWindow]}]
    (true? opensWindow)))


(s/def ::command
  (fn [{:keys [command]}]
    (and (some? command)
         (nil? (get control-flow-commands command)))))


(s/def ::commands
  (s/+ (s/alt
        :if ::command-if
        :times ::command-times
        :while ::command-while
        :do ::command-do
        :for-each ::command-for-each
        :cmd-with-open-window ::cmd-with-open-window
        :cmd ::command)))
