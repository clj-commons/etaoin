(ns build
  (:require [clojure.tools.build.api :as b]
            [clojure.string :as string]))

(defn- num-releases
  "We'll assume num tags = num releases"
  []
  (-> (b/git-process {:git-args "tags"})
      (string/split-lines)
      count))

(def lib 'etaoin/etaoin)
(def version (format "1.0.%d" (inc (num-releases))))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))
(def built-jar-version-file "target/built-jar-version.txt")

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar
  "Build library jar file.
  Also writes built version to target/built-jar-version.txt for easy peasy pickup by any interested downstream operation.

  We use the optional :version-suffix to optionally distinguish local installs from productin releases.
  For example, when installing for a cljdoc preview suffix is: cljdoc-preview."
  [{:keys [version-suffix] :as opts}]
  (let [version (if version-suffix
                  (format "%s-%s" version version-suffix)
                  version)]

    (b/write-pom {:class-dir class-dir
                  :lib lib
                  :version version
                  :scm {:tag version}
                  :basis basis
                  :src-dirs ["src"]})
    (b/copy-dir {:src-dirs ["src" "resources"]
                 :target-dir class-dir})
    (b/jar {:class-dir class-dir
            :jar-file jar-file})
    (spit built-jar-version-file version)
    (assoc opts :built-jar-version version)))

(defn install [opts]
  (clean opts)
  (let [{:keys [built-jar-version]} (jar opts)]
    (b/install {:class-dir class-dir
                :lib lib
                :version built-jar-version
                :basis basis
                :jar-file jar-file})))

(defn publish [opts]
  (clean opts)
  (jar opts)
  ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
    (merge {:installer :remote
                       :artifact jar-file
                       :pom-file (b/pom-path {:lib lib :class-dir class-dir})}
                    opts))
  opts)
