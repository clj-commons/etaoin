(ns build
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.tools.build.api :as b]))

(defn version-string []
  (let [{:keys [major minor release qualifier]} (-> "version.edn"
                                                    slurp
                                                    edn/read-string)]
    (format "%s.%s.%s%s"
            major minor release (if qualifier
                                  (str "-" qualifier)
                                  ""))))

(def lib 'etaoin/etaoin)
(def version (version-string)) ;; the expectations is some pre-processing has bumped the version when releasing
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s.jar" (name lib)))
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
                  :basis basis
                  :src-dirs ["src"]
                  :scm {:url "https://github.com/clj-commons/etaoin"
                        :connection "scm:git:git://github.com/clj-commons/etaoin.git"
                        :developerConnection "scm:git:ssh://git@github.com/clj-commons/etaoin.git"
                        :tag (format "v%s" version)}
                  :pom-data [[:description "Pure Clojure Webdriver protocol implementation"]
                             [:url "https://github.com/clj-commons/etaoin"]
                             [:licenses
                              [:license
                               [:name "Eclipse Public License"]
                               [:url "http://www.eclipse.org/legal/epl-v10.html"]]]
                             [:properties
                              [:project.build.sourceEncoding "UTF-8"]]]})
    (b/copy-dir {:src-dirs ["src" "resources"]
                 :target-dir class-dir})
    (b/jar {:class-dir class-dir
            :jar-file jar-file})
    (spit built-jar-version-file version)
    (assoc opts :built-jar-version version)))

(defn- built-version* []
  (when (not (.exists (io/file built-jar-version-file)))
    (throw (ex-info (str "Built jar version file not found: " built-jar-version-file) {})))
  (slurp built-jar-version-file))

(defn built-version
  ;; NOTE: Used by release script and github workflow
  "Spit out version of jar built (with no trailing newline).
  A separate task because I don't know what build.tools might spit to stdout."
  [_]
  (print (built-version*))
  (flush))

(defn install [opts]
  (clean opts)
  (let [{:keys [built-jar-version]} (jar opts)]
    (b/install {:class-dir class-dir
                :lib lib
                :version built-jar-version
                :basis basis
                :jar-file jar-file})))

(defn deploy [opts]
  ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
   {:installer :remote
    :artifact jar-file
    :pom-file (b/pom-path {:lib lib :class-dir class-dir})})
  opts)

(defn download-deps
  "Download all deps for all aliases"
  [_]
  (let [aliases (->> "deps.edn"
                     slurp
                     edn/read-string
                     :aliases
                     keys
                     sort)]
    ;; one at a time because aliases with :replace-deps will... well... you know.
    (println "Bring down default deps")
    (b/create-basis {})
    (doseq [a (sort aliases)]
      (println "Bring down deps for alias" a)
      (b/create-basis {:aliases [a]}))))
