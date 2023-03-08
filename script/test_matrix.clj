(ns test-matrix
  (:require [babashka.cli :as cli]
            [cheshire.core :as json]
            [clojure.string :as string]
            [doric.core :as doric]
            [helper.main :as main]
            [lread.status-line :as status]))

(defn- test-def [os id platform browser]
  {:os os
   :cmd (->> ["bb" (str "test:" platform)
              "--suites" id
              (when browser (str "--browsers " browser))
              (when (= "ubuntu" os) "--launch-virtual-display")]
             (remove nil?)
             (string/join " "))
   :desc (->> [id os browser platform]
              (remove nil?)
              (string/join " "))})

(defn- github-actions-matrix []
  (let [oses ["macos" "ubuntu" "windows"]
        ide-browsers ["chrome" "firefox"]
        api-browsers ["chrome" "firefox" "edge" "safari"]
        platforms ["jvm" "bb"]]
    (->> (concat
          (for [os oses
                platform platforms]
            (test-def os "unit" platform nil))
          (for [os oses
                platform platforms
                browser ide-browsers]
            (test-def os "ide" platform browser))
          (for [os oses
                platform platforms
                browser api-browsers
                :when (not (or (and (= "ubuntu" os) (some #{browser} ["edge" "safari"]))
                               (and (= "windows" os) (= "safari" browser))))]
            (test-def os "api" platform browser)))
         (sort-by :desc)
         (into [{:os "ubuntu" :cmd "bb lint" :desc "lint"}
                {:os "macos" :cmd "bb test-doc" :desc "test-doc"}]))))

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
      (let [matrix (->> (github-actions-matrix)
                        (filter #(= "test-doc" (:desc %))))]
        (status/line :detail
                     (if (= "json" (:format opts))
                       (json/generate-string matrix)
                       (doric/table [:os :cmd :desc] matrix)))))))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
