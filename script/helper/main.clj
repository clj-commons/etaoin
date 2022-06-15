(ns helper.main
  (:require  [clojure.string :as string]
             [docopt.core :as docopt]
             [lread.status-line :as status]))

(defmacro when-invoked-as-script
  "Runs `body` when clj was invoked from command line as a script."
  [& body]
  `(when (= *file* (System/getProperty "babashka.file"))
     ~@body))

(defn- args-usage-to-docopt-usage
  "We specify

     Valid args:
       cmd1
       cmd2
       --opt1

   Or:

     Valid args: [options]

   But docopt expects:

     Usage:
       foo cmd1
       foo cmd2
       foo -opt1

   Or:

     Usage: [options]

   This little fn converts from our args usage to something docopt can understand"
  [usage]
  (let [re-arg-usage #"(?msi)^Valid args:.*?(?=\n\n)"]
    (if-let [args-usage (re-find re-arg-usage usage)]
      (let [[label-line & variant-lines] (-> args-usage string/split-lines)
            docopt-usage-block (string/join "\n" (concat [(if (re-find #"(?i)Valid args:( +\S.*)" label-line)
                                                            (string/replace label-line #"(?i)Valid args:( +\S.*)" "Usage: foo $1")
                                                            "Usage:")]
                                                         (map #(string/replace % #"^  " "  foo ") variant-lines)))
            docopt-usage-block (str docopt-usage-block "\n")]
        (string/replace usage re-arg-usage docopt-usage-block))
      (throw (ex-info "Did not find expected 'Valid args:' in usage" {})))))

(def default-arg-usage "Valid args: [--help]

This command accepts no arguments.")

(defn doc-arg-opt
  "Args usage wrapper for docopt.

   You'll need to specify --help in your arg-usage, but code to handle --help is provided here."
  ([args]
   (doc-arg-opt default-arg-usage args))
  ([arg-usage args]
   (let [opts (docopt/docopt (args-usage-to-docopt-usage arg-usage)
                             args
                             identity
                             (fn usage-error [_docopt-usage] (status/die 1 arg-usage)))]
     (if (get opts "--help")
       (do
         (status/line :detail arg-usage)
         nil)
       opts))))
