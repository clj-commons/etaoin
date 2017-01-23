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
