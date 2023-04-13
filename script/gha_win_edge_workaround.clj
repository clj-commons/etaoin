(ns gha-win-edge-workaround
  (:require [babashka.fs :as fs]
            [babashka.http-client :as http]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [lread.status-line :as status]
            [tools-versions]))

;; To be deleted after GitHub Actions team fixes the issue

(defn- tool-info [versions tool-name]
  (->> versions
       (filter #(= tool-name (:name %)))
       first))

(defn- major-version [tool-info]
  (->> tool-info
       :version
       (re-find #"(\d+)\.")
       last))

(defn workaround []
  (status/line :head "GHA Windows Edge workaround for mismatch Edge Browser/Edge Webdriver versions")
  (let [versions (tools-versions/versions)
        edge (tool-info versions "Edge")
        edge-webdriver (tool-info versions "Edge Webdriver")]
    (status/line :detail "Edge info: %s" edge)
    (status/line :detail "Edge Webdriver info: %s" edge-webdriver)
    (let [edge-major (major-version edge)
          edge-webdriver-major (major-version edge-webdriver)]
      (if (= edge-major edge-webdriver-major)
        (status/line :detail "Major versions seem to match, all good, nothing to do.")
        (do
          (status/line :warn "Edge WebDriver major version %s does not match Edge Browser major version %s\nWill attemp to address."
                       edge-webdriver-major edge-major)
          (let [webdriver-version-url (format "https://msedgewebdriverstorage.blob.core.windows.net/edgewebdriver/LATEST_RELEASE_%s"
                                              edge-major)
                webdriver-version (-> (http/get webdriver-version-url {:as :bytes})
                                      :body
                                      (String. "UTF16")
                                      str/trim)
                dl-file "edgedriver_win64.zip"
                dl-url (format "https://msedgedriver.azureedge.net/%s/%s" webdriver-version dl-file)
                target-path (fs/parent (:path edge-webdriver))]
            (status/line :detail "Current matching Edge Webdriver version: %s" webdriver-version)
            (status/line :detail "Downloading: %s" dl-url)
            (io/copy
              (:body (http/get dl-url {:as :stream}))
              (io/file dl-file))
            (status/line :detail "Replacing Edge Webdriver in %s" target-path)
            (fs/unzip dl-file target-path {:replace-existing true})))))))
