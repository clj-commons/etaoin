(ns tools-versions
  (:require [babashka.fs :as fs]
            [clojure.string :as string]
            [doric.core :as doric]
            [cheshire.core :as json]
            [helper.main :as main]
            [helper.os :as os]
            [helper.shell :as shell]))

(def tools
  [;; earlier versions of java used -version and spit version info to stderr
   {:oses :all        :name "Java"              :type :bin         :app "java" :args "-version" :shell-opts {:out :string :err :string :continue true}}
   {:oses :all        :name "Babashka"          :type :bin         :app "bb"}

   {:oses [:unix]     :name "Chrome"            :type :bin         :app "google-chrome"} ;; only handling nix for now
   {:oses [:mac]      :name "Chrome"            :type :mac-app     :app "Google Chrome"}
   {:oses [:win]      :name "Chrome"            :type :win-package :app "Google Chrome"}
   {:oses :all        :name "Chrome Webdriver"  :type :bin         :app "chromedriver"}

   {:oses [:unix]     :name "Firefox"           :type :bin         :app "firefox"} ;; only handling nix for now
   {:oses [:mac]      :name "Firefox"           :type :mac-app     :app "Firefox"}
   {:oses [:win]      :name "Firefox"           :type :win-package :app #"Mozilla Firefox .*"}
   {:oses :all        :name "Firefox Webdriver" :type :bin         :app "geckodriver" :version-post-fn #(->> % string/split-lines first)}

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
  (if (not (zero? exit))
    {:error (format "exit code %d" exit)}
    {:version (cond-> ""
                (= :string (:out shell-opts)) (str out)
                (= :string (:err shell-opts)) (str err))}))

(defn- table-multilines->rows
  "Convert a seq of maps from [{:a \"one\n\two\" :b \"a\nb\nc\"}]
  to: [{:a \"one\" :b \"a\"}
       {:a \"two\" :b \"b\"}
       {:a \"\"    :b \"c\"}]
  in preparation for printing with doric."
  [results]
  (reduce (fn [acc n]
            (let [n (reduce-kv (fn [m k v]
                                 (assoc m k (when v (string/split-lines v))))
                               {}
                               n)
                  max-lines (apply max (map #(count (val %)) n))]
              (concat acc
                      (for [ln (range max-lines)]
                        (reduce-kv (fn [m k _v]
                                     (assoc m k (get (k n) ln "")))
                                   {}
                                   n)))))
          []
          results))

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
  (if-let [found-bin (some-> (fs/which app {:win-exts ["com" "exe" "bat" "cmd" "ps1"]})
                             str)]
    (let [version-result (->> (shell/command shell-opts found-bin args)
                              (version-cmd-result shell-opts))
          version-result (assoc version-result :path found-bin)]
      (if (:error version-result)
        version-result
        (update version-result :version version-post-fn)))
    {:error (format "bin not found: %s" app)}))

(defn -main
  "Report on tools versions based the the OS the script it is run from.
  Currently informational only, should always return 0 unless, of course,
  something exceptional happens."
  [& args]
  (when (main/doc-arg-opt args)
    (->> (for [{:keys [name] :as t} (map #(merge tool-defaults %) tools)
               :when (expected-on-this-os t)
               :let [{:keys [error path version]} (resolve-tool t)]]
           (if error
             {:name name :path path :version (format "** ERROR: %s **",error)}
             {:name name :path path :version version}))
         table-multilines->rows
         (doric/table [:name :version :path])
         println)))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
