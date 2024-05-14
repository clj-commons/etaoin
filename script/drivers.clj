(ns drivers
  (:require [babashka.fs :as fs]
            [doric.core :as doric]
            [helper.ps :as ps]
            [helper.os :as os]
            [helper.main :as main]
            [lread.status-line :as status]))

(def web-drivers
  [{:name "Chrome" :bin "chromedriver"}
   {:name "Firefox":bin "geckodriver"}
   {:name "Microsoft Edge" :bin "msedgedriver"}
   {:name "Safari" :bin "safaridriver"}
   {:name "PhantomJS" :bin "phantomjs"}])

(defn driver-processes []
  (->> (ps/all-processes)
       (keep (fn [p]
               (let [pfname (-> p :command fs/file-name)
                     pfname (if (= :win (os/get-os))
                              (fs/strip-ext pfname)
                              pfname)]
                 (when-let [driver (first (filter #(= pfname (:bin %)) web-drivers))]
                   (assoc p :name (:name driver))))))))

(defn kill [{:keys [handle]}]
  (.destroy handle))

(defn is-alive? [{:keys [handle]}]
  (.isAlive handle))

(defn wait-for-death [{:keys [handle]}]
  (let [deadline (+ (System/currentTimeMillis) 2000)]
    (loop []
      (when (and (.isAlive handle)
                 (< (System/currentTimeMillis) deadline))
        (Thread/sleep 100)
        (recur)))))

(defn report[processes]
  (->> processes
       (doric/table (keep identity [{:name :name :title "WebDriver"}
                                    :pid
                                    :command
                                    ;; arguments are don't seem to populate on Windows
                                    (when (not= :win (os/get-os)) :arguments)]))
       println))

(def args-usage "Valid args:
  [list|kill]

Commands:
  list  List running WebDriver processes (default)
  kill  Kill running WebDriver processes")

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (let [drivers (driver-processes)
          kill? (get opts "kill")]
      (if (not (seq drivers))
        (status/line :detail "No WebDrivers seem to be running.")
        (if (not kill?)
          (do (status/line :head "List of running WebDrivers")
              (report drivers))
          (do (status/line :head "Attempting to kill running WebDrivers")
              (report drivers)
              (run! kill drivers)
              (run! wait-for-death drivers)
              (let [still-running (filter is-alive? drivers)]
                (if (seq still-running)
                  (do (status/line :warn "Did not manage to kill the following WebDrivers")
                      (report still-running))
                  (status/line :detail "Success!")))))))))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
