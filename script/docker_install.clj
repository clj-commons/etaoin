(ns docker-install
  (:require [babashka.http-client :as http]
            [babashka.fs :as fs]
            [babashka.process :as p]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn- chrome-for-testing-catalog []
  (let [stable-releases (-> (http/get "https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions-with-downloads.json")
                            :body
                            (json/parse-string true)
                            :channels
                            :Stable)
        version (:version stable-releases)]
    (->> (for [asset [:chrome :chromedriver]]
           (->> stable-releases
                :downloads
                asset
                (keep #(when (= "linux64" (:platform %))
                         {:id (name asset) :version version :url (:url %)}))))
         (mapcat identity)
         (group-by :id)
         (reduce-kv (fn [m k v] (assoc m (keyword k) (first v)))
                    {}))))

(defn- report-download-progress [file-size total-bytes-read]
  (if file-size
    (status/line :detail (str "Downloaded: %" (count (str file-size)) "s of %s %.2f%%")
                 total-bytes-read file-size
                 (double (/ (* total-bytes-read 100) file-size)))
    (status/line :detail  "Downloaded: %s of ?" total-bytes-read)))

(defn- download [url to-file]
  (with-open [out (io/output-stream to-file)]
    (let [response (http/get url {:as :stream})
          file-size (some-> response :headers (get "content-length") parse-long)
          file-size (and file-size (pos? file-size) file-size)
          input-stream (:body response)
          buffer (byte-array 8192)]
      (loop [total-bytes-read 0
             last-report-time (System/currentTimeMillis)]
        (let [bytes-read (.read input-stream buffer)]
          (if (pos? bytes-read)
            (do
              (.write out buffer 0 bytes-read)
              (let [total-bytes-read (+ total-bytes-read bytes-read)
                    report-time (when (> (System/currentTimeMillis) (+ last-report-time 1000))
                                  (System/currentTimeMillis))]
                (when report-time
                  (report-download-progress file-size total-bytes-read))
                (recur total-bytes-read (or report-time last-report-time))))
            (report-download-progress file-size total-bytes-read)))))))

(defn- install-chrome-asset
  "`id` is also name of executable"
  [{:keys [id version url]}]
  (status/line :head "Installing %s %s" id version)
  (let [dl-file "/tmp/chrome-installer.zip"
        install-dir (str "/opt/" id "-" version)]
    (fs/create-dirs install-dir)
    (status/line :head "%s: downloading: %s" id url)
    (download url dl-file)
    (status/line :head "%s: installing" id)
    (p/shell "unzip" dl-file "-d" install-dir)
    (fs/create-sym-link (str "/usr/local/bin/" id) (str install-dir "/" id "-linux64/" id)))  )

(defn- install-geckodriver []
  (status/line :head "Installing geckodriver")
  (let [dl-file "/tmp/geckodriver_linux64.tar.gz"
        dl-url (-> (http/get "https://api.github.com/repos/mozilla/geckodriver/releases/latest")
                   :body
                   (json/parse-string true)
                   :assets
                   (->> (map :browser_download_url)
                        (filter #(re-find #"linux64" %))
                        first))]
    (download dl-url dl-file)
    (shell/command "tar xf" dl-file "-C" "/usr/local/bin")))

(defn- install-firefox []
  (status/line :head "Installing firefox")
  (let [dl-file "/tmp/firefox_linux64.tar.bz2"]
    (download "https://download.mozilla.org/?product=firefox-latest&os=linux64&lang=en-US"
              dl-file)
    (shell/command "tar xf" dl-file "-C" "/usr/local")
    (fs/create-sym-link "/usr/local/bin/firefox" "/usr/local/firefox/firefox")))

(defn- docker-build? []
  ;; Stackoverflow was rife with examples on how to determine we are running from a docker build,
  ;; but none them worked for me anymore.
  ;; Go simple, see Dockerfile where we create this file as a signal
  (fs/exists? "/tmp/in_a_docker_build_for_etaoin"))

(defn -main [& _args]
  (status/line :head "Are we ok to run?")
  (when (not (docker-build?))
    (status/die 1 "Expected to be run from DockerFile build"))
  (let [chrome-catalog (chrome-for-testing-catalog)]
    (install-chrome-asset (:chrome chrome-catalog))
    (install-chrome-asset (:chromedriver chrome-catalog)))
    (install-geckodriver)
    (install-firefox))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
