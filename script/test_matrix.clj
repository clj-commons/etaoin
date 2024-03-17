(ns test-matrix
  (:require [babashka.cli :as cli]
            [cheshire.core :as json]
            [clojure.string :as string]
            [doric.core :as doric]
            [helper.main :as main]
            [lread.status-line :as status]))

(defn- test-def [{:keys [os id platform browser jdk-version]}]
  {:os os
   :jdk-version jdk-version
   :cmd (->> ["bb" (str "test:" platform)
              "--suites" id
              (when browser (str "--browsers " browser))
              (when (= "ubuntu" os) "--launch-virtual-display")]
             (remove nil?)
             (string/join " "))
   :desc (->> [id os browser (if (= "jvm" platform)
                               (str "jdk" jdk-version)
                               platform)]
              (remove nil?)
              (string/join " "))})

(defn- test-doc [{:keys [os jdk-version]}]
  {:os os
   :jdk-version jdk-version
   :cmd (if (= "ubuntu" os)
          "bb test-doc --launch-virtual-display"
          "bb test-doc")
   :desc (str "test-doc " os " jdk" jdk-version)} )

(defn- github-actions-matrix []
  (let [oses ["macos" "ubuntu" "windows"]
        ide-browsers ["chrome" "firefox"]
        api-browsers ["chrome" "firefox" "edge" "safari"]
        platforms ["jvm" "bb"]
        jdk-versions ["8" "11" "17" "21"]
        default-opts {:jdk-version "21"}]
    (->> (concat
           (for [os oses
                 platform platforms]
             (test-def (merge default-opts {:os os :id "unit" :platform platform})))
           (for [os oses
                 platform platforms
                 browser ide-browsers]
             (test-def (merge default-opts {:os os :id "ide" :platform platform :browser browser})))
           (for [os oses
                 platform platforms
                 browser api-browsers
                 :when (not (or (and (= "ubuntu" os) (some #{browser} ["edge" "safari"]))
                                (and (= "windows" os) (= "safari" browser))))]
             (test-def (merge default-opts {:os os :id "api" :platform platform :browser browser})))
           ;; for jdk coverage we don't need to run across all oses and browsers
           (for [id ["unit" "ide" "api"]
                 jdk-version jdk-versions
                 :when (not= jdk-version (:jdk-version default-opts))]
             (test-def {:jdk-version jdk-version :os "ubuntu" :id id :platform "jvm" :browser "firefox"}))
           (for [os oses]
             (test-doc (merge default-opts {:os os})))
           (for [jdk-version jdk-versions
                 :when (not= jdk-version (:jdk-version default-opts))]
             (test-doc {:jdk-version jdk-version :os "ubuntu"})))
         (sort-by (juxt #(parse-long (:jdk-version %)) :desc))
         (into [(merge default-opts {:os "ubuntu" :cmd "bb lint" :desc "lint"})]))))

(def valid-formats ["json" "table"])
(def cli-spec {:help {:desc "This usage help"}
               :format {:ref "<format>"
                        :desc (str "Output format for matrix, specify one of: " (string/join ", " valid-formats))
                        :coerce :string
                        :default-desc "table"
                        :validate {:pred (set valid-formats)
                                   :ex-msg (fn [_m]
                                             (str "--format must be one of: " valid-formats))}}})

(defn- usage-help []
  (status/line :head "Usage help")
  (status/line :detail "Print the test matrix for GitHub Actions\n")
  (status/line :detail (cli/format-opts {:spec cli-spec :order [:format :help]})))

(defn -main [& args]
  (let [opts (cli/parse-opts args {:spec cli-spec
                                   :restrict true
                                   :error-fn (fn [{:keys [msg]}]
                                               (status/line :error msg)
                                               (usage-help)
                                               (System/exit 1))})]
    (if (:help opts)
      (usage-help)
      (let [matrix (github-actions-matrix)]
        (status/line :detail
                     (if (= "json" (:format opts))
                       (json/generate-string matrix)
                       (doric/table [:os :jdk-version :cmd :desc] matrix)))))))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
