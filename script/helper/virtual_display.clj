(ns helper.virtual-display
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [helper.shell :as shell]
            [lread.status-line :as status]))

(defn- launch-xvfb []
  (if (fs/which "Xvfb")
    (process/process "Xvfb :99 -screen 0 1024x768x24" {:out (fs/file "/dev/null")
                                                       :err (fs/file "/dev/null")})
    (status/die 1 "Xvfb not found"))
  (let [deadline (+ (System/currentTimeMillis) 10000)]
    (loop []
      (let [{:keys [exit]} (shell/command {:out (fs/file "/dev/null")
                                           :err (fs/file "/dev/null")
                                           :continue true}
                                          "xdpyinfo -display :99")]
        (if (zero? exit)
          (status/line :detail "Xvfb process looks good.")
          (if (> (System/currentTimeMillis) deadline)
            (status/die 1 "Failed to get status from Xvfb process")
            (do
              (status/line :detail "Waiting for Xvfb process.")
              (Thread/sleep 500)
              (recur))))))))

(defn- launch-fluxbox []
  (if (fs/which "fluxbox")
    (process/process "fluxbox -display :99" {:out (fs/file "/dev/null")
                                             :err (fs/file "/dev/null")})
    (status/die 1 "fluxbox not found")))

(defn launch []
  (status/line :head "Launching virtual display")
  (launch-xvfb)
  (launch-fluxbox))

(defn extra-env "Returns env vars required for programs using launched virtual display"
  []
  {"DISPLAY" ":99.0"})
