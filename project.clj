(defproject etaoin "0.1.1"
  :description "Pure Clojure Webdriver protocol implementation."
  :url "https://github.com/igrishaev/etaoin"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [log4j/log4j "1.2.17"]]}}

  :dependencies [[clj-http "2.3.0"]
                 [cheshire "5.6.3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/data.codec "0.1.0"]])
