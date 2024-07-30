(ns build-shared
  (:require
   [clojure.edn :as edn]
   [clojure.string :as string]))

(defn- project-info []
  (-> (edn/read-string (slurp "deps.edn"))
      :aliases :neil :project))

(def version-tag-prefix "v")

(defn lib-version []
  (-> (project-info) :version))

(defn lib-artifact-name []
  (-> (project-info) :name))

(defn lib-github-coords []
  (-> (project-info) :github-coords))

(defn version->tag [version]
  (str version-tag-prefix version))

(defn tag->version [ci-tag]
  (and (string/starts-with? ci-tag version-tag-prefix)
       (string/replace-first ci-tag version-tag-prefix "")))
