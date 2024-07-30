(ns build
  (:require [build-shared]
            [clojure.edn :as edn]
            [clojure.tools.build.api :as b]))

(def version (build-shared/lib-version))
(def lib (build-shared/lib-artifact-name))

(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s.jar" (name lib)))

(defn jar
  "Build library jar file.
  Supports `:version-override` for local testing, otherwise official version is used.
  For example, when previewing for cljdoc we use a the version with  -cljdoc-preview suffix."
  [{:keys [version-override] :as opts}]
  (b/delete {:path class-dir})
  (b/delete {:path jar-file})
  (let [version (or version-override version)]
    (println "jarring version" version)
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
    (assoc opts :built-version version)))

(defn install
  "Install built jar to local maven repo, optionally specify `:version-override` for local testing."
  [opts]
  (let [{:keys [built-version]} (jar opts)]
    (println "installing version" built-version)
    (b/install {:class-dir class-dir
                :lib lib
                :version built-version
                :basis basis
                :jar-file jar-file})))

(defn deploy [_]
  (jar {})
  (println "deploy")
  ((requiring-resolve 'deps-deploy.deps-deploy/deploy)
   {:installer :remote
    :artifact jar-file
    :pom-file (b/pom-path {:lib lib :class-dir class-dir})}))

(defn download-deps
  "Download all deps for all aliases"
  [_]
  (let [aliases (->> "deps.edn"
                     slurp
                     edn/read-string
                     :aliases
                     keys)]
    ;; one at a time because aliases with :replace-deps will... well... you know.
    (println "Bring down default deps")
    (b/create-basis {})
    (doseq [a (sort aliases)]
      (println "Bring down deps for alias" a)
      (b/create-basis {:aliases [a]}))))
