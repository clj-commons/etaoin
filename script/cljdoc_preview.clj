#!/usr/bin/env bb

(ns cljdoc-preview
  (:require [babashka.fs :as fs]
            [build-shared]
            [babashka.http-client :as http]
            [clojure.java.browse :as browse]
            [clojure.string :as string]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]))

;;
;; constants
;;

(def cljdoc-root-temp-dir "./.cljdoc-preview")
(def cljdoc-db-dir (str cljdoc-root-temp-dir  "/db"))
(def cljdoc-container {:name "cljdoc-server"
                       :image "cljdoc/cljdoc"
                       :port 8000})

;;
;; Prerequisites
;;

(defn check-prerequisites []
  (let [missing-cmds (doall (remove fs/which ["git" "docker"]))]
    (when (seq missing-cmds)
      (status/die 1 (string/join "\n" ["Required commands not found:"
                                       (string/join "\n" missing-cmds)])))))

;;
;; project build info
;;

(defn local-install [canary-version]
  (status/line :head "installing lib to local maven repo")
  (shell/command "clojure -T:build install :version-override" (pr-str canary-version)))

(defn get-project []
  (str (build-shared/lib-artifact-name)))

;;
;; git
;;

(defn git-sha []
  (-> (shell/command {:out :string}
                     "git rev-parse HEAD")
      :out
      string/trim))

(defn https-uri
  ;; stolen from cljdoc's http-uri
  "Given a URI pointing to a git remote, normalize that URI to an HTTP one."
  [scm-url]
  (cond
    (.startsWith scm-url "http")
    scm-url

    (or (.startsWith scm-url "git@")
        (.startsWith scm-url "ssh://"))
    (-> scm-url
        (string/replace #":" "/")
        (string/replace #"\.git$" "")
        ;; three slashes because of prior :/ replace
        (string/replace #"^(ssh///)*git@" "https://"))))

(defn git-origin-url-as-https []
  (-> (shell/command {:out :string}
                     "git config --get remote.origin.url")
      :out
      string/trim
      https-uri))

(defn uncommitted-code? []
  (-> (shell/command {:out :string}
                     "git status --porcelain")
      :out
      string/trim
      seq))

(defn unpushed-commits? []
  (let [{:keys [:exit :out]} (shell/command {:continue true :out :string}
                                            "git cherry -v")]
    (if (zero? exit)
      (-> out string/trim seq)
      (status/die 1 "Failed to check for unpushed commits to branch, is your branch pushed?"))))

;;
;; docker
;;

(defn status-server [ container ]
  (let [container-id (-> (shell/command {:out :string}
                                        "docker ps -q -f" (str "name=" (:name container)))
                         :out
                         string/trim)]
    (if (string/blank? container-id) "down" "up")))

(defn docker-pull-latest [ container ]
  (shell/command "docker pull" (:image container)))

(defn stop-server [ container ]
  (when (= "down" (status-server container))
    (status/die 1
                "%s does not appear to be running"
                (:name container)))
  (shell/command "docker" "stop" (:name container) "--time" "0"))

(defn wait-for-server
  "Wait for container's http server to become available, assumes server has valid root page"
  [container]
  (status/line :head "Waiting for %s to become available" (:name container))
  (when (= "down" (status-server container))
    (status/die 1
                "%s does not seem to be running.\nDid you run the start command yet?"
                (:name container)))
  (status/line :detail "%s container is running" (:name container))
  (let [url (str "http://localhost:" (:port container))]
    (loop []
      (if-not (try
                (http/get url)
                url
                (catch Exception _e
                  (Thread/sleep 4000)))
        (do (println "waiting on" url " - hit Ctrl-C to give up")
            (recur))
        (println "reached" url)))))

(defn status-server-print [container]
  (status/line :detail (str (:name container) ": " (status-server container))))

;;
;; cljdoc server in docker
;;

(defn cljdoc-ingest [container project version]
  (status/line :head "Ingesting project %s %s\ninto local cljdoc database" project version)
  (shell/command "docker"
                 "run" "--rm"
                 "-v" (str cljdoc-db-dir ":/app/data")
                 "-v" (str (fs/home) "/.m2:/root/.m2")
                 "-v" (str (fs/cwd) ":" (fs/cwd) ":ro")
                 "--entrypoint" "clojure"
                 (:image container)
                 "-M:cli"
                 "ingest"
                  ;; project and version are used to locate the maven artifact (presumably locally)
                 "--project" project "--version" version
                  ;; use git origin to support folks working from forks/PRs
                 "--git" (git-origin-url-as-https)
                  ;; specify revision to allow for previewing when working from branch
                 "--rev" (git-sha)))

(defn start-cljdoc-server [container]
  (when (= "up" (status-server container))
    (status/die 1
                "%s is already running"
                (:name container)))
  (status/line :head "Checking for updates")
  (docker-pull-latest container)
  (status/line :head "Starting %s on port %d" (:name container) (:port container))
  (shell/command "docker"
                 "run" "--rm"
                 "--name" (:name container)
                 "-d"
                 "-p" (str (:port container) ":8000")
                 "-v" (str cljdoc-db-dir ":/app/data")
                 "-v" (str (fs/home) "/.m2:/root/.m2")
                 "-v" (str (fs/cwd) ":" (fs/cwd) ":ro")
                 (:image container)))

(defn view-in-browser [url]
  (status/line :head "opening %s in browser" url)
  (when (not= 200 (:status (http/get url {:throw false})))
    (status/die 1 "Could not reach:\n%s\nDid you run the ingest command yet?" url))
  (browse/browse-url url))


;;
;; main
;;

(defn git-warnings []
  (let [warnings (remove nil?
                         [(when (uncommitted-code?)
                            "There are changes that have not been committed, they will not be previewed")
                          (when (unpushed-commits?)
                            "There are commits that have not been pushed, they will not be previewed")])]
    (when (seq warnings)
      (status/line :warn (string/join "\n" warnings)))))

(defn cleanup-resources []
  (when (fs/exists? cljdoc-db-dir)
    (fs/delete-tree cljdoc-db-dir)))

(def args-usage "Valid args: (start|ingest|view|stop|status|--help)

Commands:
  start   Start docker containers supporting cljdoc preview
  ingest  Locally publishes your project for cljdoc preview
  view    Opens cljdoc preview in your default browser
  stop    Stops docker containers supporting cljdoc preview
  status  Status of docker containers supporting cljdoc preview

Options:
  --help  Show this help

Must be run from project root directory.")

(defn -main [& args]
  (check-prerequisites)
  (let [canary-version (str (build-shared/lib-version) "-cljdoc-preview")]
    (when-let [opts (main/doc-arg-opt args-usage args)]
      (cond
        (get opts "start")
        (do
          (start-cljdoc-server cljdoc-container)
          nil)

        (get opts "ingest")
        (do
          (git-warnings)
          (local-install canary-version)
          (cljdoc-ingest cljdoc-container (get-project) canary-version)
          nil)

        (get opts "view")
        (do
          (wait-for-server cljdoc-container)
          (view-in-browser (str "http://localhost:" (:port cljdoc-container) "/d/" (get-project) "/" canary-version))
          nil)

        (get opts "status")
        (status-server-print cljdoc-container)

        (get opts "stop")
        (do
          (stop-server cljdoc-container)
          (cleanup-resources)
          nil)))))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
