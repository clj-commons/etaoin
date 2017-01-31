;; (defn get-timeout [server session]
;;   "https://www.w3.org/TR/webdriver/#dfn-get-timeout"
;;   (client/call server :get
;;                [:session session :timeouts]))

;; (defn execute-async-script [server session script & args]
;;   "https://www.w3.org/TR/webdriver/#dfn-execute-async-script"
;;   (-> server
;;       (client/call :post
;;                    [:session session :execute :async]
;;                    {:script script :args args})))

;; (defn set-timeout [server session type msec]
;;   "https://www.w3.org/TR/webdriver/#dfn-set-timeouts"
;;   (client/call server :post
;;                [:session session :timeouts]
;;                {:type type :ms msec}))

;; (defn ^:not-implemented
;;   get-window-rect [server session]
;;   "https://www.w3.org/TR/webdriver/#dfn-get-window-rect"
;;   (-> server
;;       (client/call :get [:session session :window :rect])))

;; (defn ^:not-implemented
;;   set-window-rect [server session x y width height]
;;   "https://www.w3.org/TR/webdriver/#dfn-set-window-rect"
;;   (client/call :post
;;                [:session session :window :rect]
;;                {:x x :y y :width width :height height}))

;; (defn switch-to-frame [server session frame]
;;   "https://www.w3.org/TR/webdriver/#dfn-switch-to-frame"
;;   (-> server
;;       (client/call
;;        :post [:session session :frame]
;;        {:id frame})))

;; (defn switch-to-parent-frame [server session]
;;   "https://www.w3.org/TR/webdriver/#dfn-switch-to-parent-frame"
;;   (-> server
;;       (client/call
;;        :post [:session session :frame :parent])))


;;
;; todos
;;
;; forms support textarea, etc
;; xpath in elements
;; text present on page
;; page src
;; todo move to tests
;; todo unused imports
;; todo variable bound checks?
;; todo: on exception return source code and screenshot
;; scenarios
;; multi-browser run in threads
;; process logs
;; todo fill keys
;; skip decorator
;; todo add local html test
;; custom HTML files for tests
;; js clear local storage
;; todo elemet size
;; todo elemet rect
;; element location
;; resize
;; position
;; get-hash
;; check if process is alive
;; wait for (not) present/visible/enabled
;;

;; todo read form
;; ;; todo submit form
;; ;; todo multi-form
;; ;; todo fill form human
;; ;; todo submit form
;; (defn fill-form [form]
;;   (doseq [[field text] form]
;;     (let [term (format "//input[@name='%s']" (name field))]
;;       (with-element term
;;         (fill text)))))

;; (defmacro with-server-multi [servers & body]
;;   `(doseq [[host# port#] ~servers]
;;      (binding [*server* (make-server host# port#)]
;;        ~@body)))

;; ;; (defmacro with-start [host port & body]
;; ;;   `(with-server ~host ~port
;; ;;      (with-process ~host ~port
;; ;;        ~@body)))

;; ;; (defmacro with-start-multi [connections & body]
;; ;;   `(doseq [[host# port#] ~connections]
;; ;;      (with-server host# port#
;; ;;        (with-process host# port#
;; ;;          ~@body))))

;; ;; todo multi-futures

        <!-- <div style=" -->
        <!--     display: block; -->
        <!--     width: 100px; -->
        <!--     height: 100px; -->
        <!--     background-color: yellow; -->
        <!--     position: relative; -->
        <!--     z-index: 990; -->
        <!-- "></div> -->
        <!-- <div style=" -->
        <!--     width: 50px; -->
        <!--     height: 50px; -->
        <!--     background-color: black; -->
        <!--     position: relative; -->
        <!--     left: 25px; -->
        <!--     top: -75px; -->
        <!--     z-index: 1; -->
        <!-- " id="div-covered"></div> -->
