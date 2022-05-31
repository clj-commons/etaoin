(ns docker-install
  (:require [babashka.curl :as curl]
            [babashka.fs :as fs]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn- chromedriver-version []
  (-> (curl/get "chromedriver.storage.googleapis.com/LATEST_RELEASE")
      :body))

(defn- download [url to-file]
  (io/copy
    (-> url (curl/get {:as :bytes}) :body)
    (io/file to-file)))

(defn- install-chromedriver []
  (status/line :head "Installing chromedriver")
  (let [version (chromedriver-version)
        dl-file "/tmp/chromedriver_linux64.zip"
        dl-url (format "http://chromedriver.storage.googleapis.com/%s/chromedriver_linux64.zip"
                       version)
        install-dir (str "/opt/chromedriver-" version)]
    (fs/create-dirs install-dir)
    (status/line :head "chromedriver: downloading: %s" dl-url)
    (download dl-url dl-file)
    (status/line :head "chromedriver: installing")
    (fs/unzip dl-file install-dir)
    (shell/command "chmod +x" (str install-dir "/chromedriver"))
    (fs/create-sym-link "/usr/local/bin/chromedriver" (str install-dir "/chromedriver"))))

(defn- install-chrome []
  (status/line :head "Installing chrome")
  (let [key-file "/tmp/linux_signing_key.pub"]
    (download "https://dl-ssl.google.com/linux/linux_signing_key.pub" key-file)
    (shell/command "apt-key add" key-file)
    (spit "/etc/apt/sources.list.d/google-chrome.list"
          "deb http://dl.google.com/linux/chrome/deb/ stable main\n"
          :append true)
    (shell/command "apt-get -yqq update")
    (shell/command "apt-get -yqq install google-chrome-stable")))

(defn- install-geckodriver []
  (status/line :head "Installing geckodriver")
  (let [dl-file "/tmp/geckodriver_linux64.tar.gz"
        dl-url (-> (curl/get "https://api.github.com/repos/mozilla/geckodriver/releases/latest")
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
  (let [cgroup-file "/proc/1/cgroup"]
    (when (fs/exists? cgroup-file)
      (let [cgroup (slurp (java.io.FileReader. cgroup-file))]
        ;; example cgroup:
        ;; 0::/docker/buildkit/n4eu0udvbz9vfzjy71jkvgkwh
        (re-find #"::/docker/buildkit/" cgroup)))))

(defn -main [& _args]
  (status/line :head "Are we ok to run?")
  (when (not (docker-build?))
    (status/die 1 "Expected to be run from DockerFile"))
  (install-chromedriver)
  (install-chrome)
  (install-geckodriver)
  (install-firefox))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
