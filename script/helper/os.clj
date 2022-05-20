(ns helper.os
  (:require [clojure.string :as string]))

(defn get-os []
  (let [os-name (string/lower-case (System/getProperty "os.name"))]
    (condp re-find os-name
      #"win" :win
      #"mac" :mac
      #"(nix|nux|aix)" :unix
      #"sunos" :solaris
      :unknown)))
