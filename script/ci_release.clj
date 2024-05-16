#!/usr/bin/env bb

;;
;; This script is ultimately run from GitHub Actions
;;

(ns ci-release
  (:require [babashka.fs :as fs]
            [clojure.string :as string]
            [helper.main :as main]
            [helper.shell :as shell]
            [lread.status-line :as status]
            [rewrite-clj.zip :as z]))

(defn clean! []
  (doseq [dir ["target" ".cpcache"]]
    (when (fs/exists? dir)
      (fs/delete-tree dir))))

(defn- last-release-tag []
  (->>  (shell/command {:out :string}
                       "git tag --sort=-creatordate")
        :out
        string/split-lines
        ;; support old and new tag version scheme
        ;; old had no prefix: 0.4.6
        ;; new prefixes with v: v1.0.37
        (filter #(re-matches #"v?\d+\..*" %))
        first))

(defn- update-file! [fname match-replacements]
  (let [old-content (slurp fname)
        new-content (reduce (fn [in [desc match replacement]]
                              (let [out (string/replace-first in match replacement)]
                                (if (= in out)
                                  (status/die 1 "Expected to %s in %s" desc fname)
                                  out)))
                            old-content
                            match-replacements)]
    (spit fname new-content)))

(defn- update-user-guide! [version]
  (status/line :head (str "Updating project version in user guide to " version))
  (update-file! "doc/01-user-guide.adoc"
                [["update :lib-version: adoc attribute"
                  #"(?m)^(:lib-version: )(.*)$"
                  (str "$1"version)]]))

(defn- validate-changelog
  "Certainly not fool proof, but should help for common mistakes"
  []
  (status/line :head "Validating change log")
  (let [content (slurp "CHANGELOG.adoc")
        valid-attrs ["[minor breaking]" "[breaking]"]
        [_ suffix desc :as match] (re-find #"(?ims)^== Unreleased ?(.*?)$(.*?)(== v\d|\z)" content)]
    (cond
      (not match)
      (status/die 1 "Unreleased section not found.")

      (and suffix
           (not (string/blank? suffix))
           (not (contains? (set valid-attrs) suffix)))
      (status/die 1 "Unreleased section suffix must be absent or one of:\n %s\nBut found:\n %s"
                  (string/join ", " valid-attrs)
                  (pr-str suffix))

      (string/blank? desc)
      (status/die 1 "Unreleased section has no text describing release")

      :else
      (status/line :detail "✅ Unreleased section found with text describing release."))))

(defn bump-version
  "Bump :release in version.edn file while preserving any formatting and comments"
  []
  (spit "version.edn"
        (-> "version.edn"
            z/of-file
            (z/find-value z/next :release)
            z/right
            (z/edit inc)
            z/root-string)))

(defn- update-changelog! [version last-version]
  (status/line :head (str "Updating change log unreleased header to release " version))
  (update-file! "CHANGELOG.adoc"
                [["update unreleased header"
                  #"(?ims)^== Unreleased( ?.*?)(== v\d|\z)"
                  (str
                    ;; add Unreleased section for next released
                    "== Unreleased\n\n"
                    ;; replace "Unreleased" with actual version
                    "== v" version
                    ;; followed by any suffix and section content
                    "$1"
                    ;; followed by link to commit log
                    (when last-version
                      (str
                        "https://github.com/clj-commons/etaoin/compare/"
                        last-version
                        "\\\\...v"  ;; single backslash is escape for AsciiDoc
                        version
                        "[Full commit log]\n\n"))
                    ;; followed by next section indicator
                    "$2")]]))

(defn- create-jar! []
  (status/line :head "Creating jar for release")
  (shell/clojure "-T:build jar")
  nil)

(defn- built-version []
  (-> (shell/clojure {:out :string}
                     "-T:build built-version")
      :out
      string/trim))

(defn- assert-on-ci
  "Little blocker to save myself from myself when testing."
  [action]
  (when (not (System/getenv "CI"))
    (status/die 1 "We only want to %s from CI" action)))

(defn- deploy-jar!
  "For this to work, appropriate CLOJARS_USERNAME and CLOJARS_PASSWORD must be in environment."
  []
  (status/line :head "Deploying jar to clojars")
  (assert-on-ci "deploy a jar")
  (shell/clojure "-T:build:deploy deploy")
  nil)

(defn- commit-changes! [version]
  (let [tag-version (str "v" version)]
    (status/line :head (str  "Committing and pushing changes made for " tag-version))
    (assert-on-ci "commit changes")
    (status/line :detail "Adding changes")
    (shell/command "git add doc/01-user-guide.adoc CHANGELOG.adoc version.edn")
    (status/line :detail "Committing")
    (shell/command "git commit -m" (str  "Release job: updates for version " tag-version))
    (status/line :detail "Version tagging")
    (shell/command "git" "tag" "-a" tag-version "-m" (str  "Release " tag-version))
    (status/line :detail "Pushing commit")
    (shell/command "git push")
    (status/line :detail "Pushing version tag")
    (shell/command "git push origin" tag-version)
    nil))

(defn- inform-cljdoc! [version]
  (status/line :head (str "Informing cljdoc of new version " version))
  (assert-on-ci "inform cljdoc")
  (let [exit-code (->  (shell/command {:continue true}
                                      "curl" "-X" "POST"
                                      "-d" "project=etaoin/etaoin"
                                      "-d" (str  "version=" version)
                                      "https://cljdoc.org/api/request-build2")
                       :exit)]
    (when (not (zero? exit-code))
      (status/line :warn (str  "Informing cljdoc did not seem to work, exited with " exit-code)))))

(def args-usage "Valid args: (prep|deploy-remote|commit|validate|--help)

Commands:
  prep           Update version, user guide, changelog and create jar
  deploy-remote  Deploy jar to clojars
  commit         Commit changes made back to repo, inform cljdoc of release

These commands are expected to be run in order from CI.
Why the separation?
To restrict the exposure of our CLOJARS secrets during deploy workflow

Additional commands:
  validate      Verify that change log is good for release

Options
  --help        Show this help")

(defn -main [& args]
  (when-let [opts (main/doc-arg-opt args-usage args)]
    (cond
      (get opts "prep")
      (do (clean!)
          (validate-changelog)
          (let [last-version (last-release-tag)]
            (status/line :detail (str "Last version released: " (or last-version "<none>")))
            (fs/create-dirs "target")
            (bump-version)
            (create-jar!)
            (let [version (built-version)]
              (status/line :detail (str "Built version: " version))
              (update-user-guide! version)
              (update-changelog! version last-version))))

      (get opts "deploy-remote")
      (deploy-jar!)

      (get opts "commit")
      (let [version (built-version)]
        (commit-changes! version)
        (inform-cljdoc! version))

      (get opts "validate")
      (do (validate-changelog)
          nil))))

(main/when-invoked-as-script
 (apply -main *command-line-args*))
