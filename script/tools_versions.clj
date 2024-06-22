(ns tools-versions
  (:require [babashka.fs :as fs]
            [clojure.string :as string]
            [clj-commons.ansi :as ansi]
            [cheshire.core :as json]
            [helper.main :as main]
            [helper.os :as os]
            [helper.shell :as shell]))

(defn- first-line [s]
  (-> s string/split-lines first))

(def tools
  [;; earlier versions of java used -version and spit version info to stderr
   {:oses :all        :name "Java"              :type :bin         :app "java" :args "-version" :shell-opts {:out :string :err :string :continue true}}
   {:oses :all        :name "Babashka"          :type :bin         :app "bb"}

   {:oses [:mac :win] :name "Image Magick"      :type :bin         :app "magick" :version-post-fn first-line}
   {:oses [:unix]     :name "Image Magick"      :type :bin         :app "identify" :version-post-fn first-line}

   {:oses [:unix]     :name "Chrome"            :type :bin         :app ["chrome" "google-chrome"]}
   {:oses [:mac]      :name "Chrome"            :type :mac-app     :app "Google Chrome"}
   {:oses [:win]      :name "Chrome"            :type :win-package :app "Google Chrome"}
   {:oses :all        :name "Chrome Webdriver"  :type :bin         :app "chromedriver"}

   {:oses [:unix]     :name "Firefox"           :type :bin         :app "firefox"}
   {:oses [:mac]      :name "Firefox"           :type :mac-app     :app "Firefox"}
   {:oses [:win]      :name "Firefox"           :type :win-package :app #"Mozilla Firefox .*"}
   {:oses :all        :name "Firefox Webdriver" :type :bin         :app "geckodriver" :version-post-fn first-line}

   {:oses [:mac]      :name "Edge"              :type :mac-app     :app "Microsoft Edge"}
   {:oses [:win]      :name "Edge"              :type :win-package :app "Microsoft Edge"}
   {:oses [:win :mac] :name "Edge Webdriver"    :type :bin         :app "msedgedriver"}

   {:oses [:mac]      :name "Safari"            :type :mac-app     :app "Safari"}
   {:oses [:mac]      :name "Safari Webdriver"  :type :bin         :app "safaridriver"}])

(def tool-defaults {:shell-opts {:out :string :continue true}
                    :args "--version"
                    :version-post-fn identity})

(defn- expected-on-this-os [{:keys [oses]}]
  (or (= :all oses)
      (some #{(os/get-os)} oses)))

(defn- version-cmd-result [shell-opts {:keys [out err exit]}]
  (let [output (cond-> ""
                 (= :string (:out shell-opts)) (str out)
                 (= :string (:err shell-opts)) (str err))]
    (if (not (zero? exit))
      {:error (format "Exit code: %d\n%s" exit output)}
      {:version output})))

(defn- windows-software-list*
  "One way to get a list of installed software on Windows.
  Seems like there are many many ways, but this also often gets the install
  location which is interesting to report."
  []
  (let [reg-keys ["\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*"
                  "\\Software\\Wow6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\*"]]
    (->> (mapcat (fn [reg-key]
                   (-> (shell/command {:out :string :continue true}
                                      (format "Get-ItemProperty HKLM:%s | Select-Object DisplayName, DisplayVersion, InstallLocation | ConvertTo-Json"
                                              reg-key))
                       :out
                       json/parse-string))
                 reg-keys))))

(def windows-software-list (memoize windows-software-list*))

(defmulti resolve-tool :type)

(defmethod resolve-tool :win-package
  [{:keys [app]}]
  (if-let [found-package (->> (windows-software-list)
                              (filter (fn [p]
                                        (when-let [pname (get p "DisplayName")]
                                          (if (string? app)
                                            (= app pname)
                                            (re-matches app pname)))))
                              first)]
    {:path (get found-package "InstallLocation" "?")
     :version (get found-package "DisplayVersion" "?")}
    {:error (format "windows package not found: %s" app)}))

(defmethod resolve-tool :mac-app
  [{:keys [app shell-opts version-post-fn]}]
  (let [app-dir (format "/Applications/%s.app" app)]
    (if (fs/exists? app-dir)
      (let [version-result (->> (shell/command shell-opts (format "defaults read '%s/Contents/Info' CFBundleShortVersionString" app-dir))
                                (version-cmd-result shell-opts))
            version-result (assoc version-result :path app-dir)]
        (if (:error version-result)
          version-result
          (update version-result :version version-post-fn)))
      {:error (format "mac app not found: %s" app)})))

(defmethod resolve-tool :bin
  [{:keys [app shell-opts args version-post-fn]}]
  (let [apps (if (vector? app)
               app
               [app])]
    (if-let [[app found-bin] (reduce (fn [_acc app]
                                       (when-let [found-bin (some-> (fs/which app {:win-exts ["com" "exe" "bat" "cmd" "ps1"]})
                                                                    str)]
                                         (reduced [app found-bin])))
                                     nil
                                     apps)]
      ;; call with app rather than found-bin to avoid Windows headaches
      (let [version-result (->> (shell/command shell-opts app args)
                                (version-cmd-result shell-opts))
            version-result (assoc version-result :path found-bin)]
        (if (:error version-result)
          version-result
          (update version-result :version version-post-fn)))
      {:error (format "bin not found: %s" app)})))

(defn short-version [long-version]
  (re-find #"\d+[+.a-zA-Z0-9]+" long-version))

(defn versions []
  (for [{:keys [name] :as t} (map #(merge tool-defaults %) tools)
               :when (expected-on-this-os t)
               :let [{:keys [version] :as r} (resolve-tool t)]]
    (cond-> (assoc r :name name)
      version (assoc :short-version (short-version version)))))

(defn tool-version [tools tool-name]
  (some->> tools
           (filter #(= tool-name (:name %)))
           first
           :short-version))

(defn version-mismatch [tools tool1-name tool2-name]
  (let [tool1-version (tool-version tools tool1-name)
        tool2-version (tool-version tools tool2-name)]
    (when (not= tool1-version tool2-version)
      (format "Version mismatch: %s %s != %s %s"
              tool1-name (or tool1-version "<not found>")
              tool2-name (or tool2-version "<not found>")))))

(defn -main
  "Report on tools versions based the the OS the script it is run from.
  Currently informational only, should always return 0 unless, of course,
  something exceptional happens."
  [& args]
  (when (main/doc-arg-opt args)
    (println (ansi/compose "\n"
                           [:green (System/getProperty "os.name")] "\n"
                           "  version: " (System/getProperty "os.version") "\n"
                           "  arch: " (System/getProperty "os.arch")))
    (let [tools (versions)]
      (doseq [{:keys [error name short-version path version]} tools]
        (if error
          (println (ansi/compose [:red name " - Error"] "\n"
                                 [:red (-> error string/trim (string/replace #"(?m)^" "  "))]))
          (println (ansi/compose [:green name " " short-version] " - " path "\n"
                                 (-> version string/trim (string/replace #"(?m)^" "  "))))))
      ;; chrome/chromedriver and edge/edgedriver versions should match
      (let [warnings (filter some?
                             [(version-mismatch tools "Chrome" "Chrome Webdriver")
                              (version-mismatch tools "Edge" "Edge Webdriver")])]
        (when (seq warnings)
          (println (ansi/compose "\n" [:red "Warnings"]))
          (doseq [w warnings]
            (println "-" w)))))))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
