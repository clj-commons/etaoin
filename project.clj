(def VERSION (.trim (slurp "VERSION")))

(defproject etaoin VERSION

  :description "Pure Clojure Webdriver protocol implementation."

  :url "https://github.com/igrishaev/etaoin"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :profiles {:dev {:plugins [[lein-codox "0.10.7"]]
                   :dependencies [[org.clojure/clojure "1.8.0"]
                                  [log4j/log4j "1.2.17"]]

                   :resource-paths ["env/dev/resources"]

                   :global-vars    {*warn-on-reflection* true
                                    *assert*             true}}

             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}}
                                  ;[nrepl "0.6.0"]]}}

  :dependencies [[clj-http "2.3.0"]
                 [cheshire "5.6.3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/data.codec "0.1.0"]]

  ;;
  ;; When running the tests as `lein test2junit`,
  ;; emit XUNIT test reports to enable CircleCI
  ;; to collect statistics over time
  ;;
  :plugins [[test2junit "1.1.2"]]
  :test2junit-output-dir "target/test2junit"

  :codox {:output-path "autodoc"})
