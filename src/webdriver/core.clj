(ns webdriver.core
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.data.codec.base64 :as b64]
            [clojure.java.io :as io]
            [clojure.test :refer [is deftest]]))

(def url-server "http://127.0.0.1:4444")

(def url-status "/status")
(def url-session "/session")
(def url-session-delete "/session/%s")
(def url-go-url "/session/%s/url")
(def url-go-back "/session/%s/back")
(def url-go-forward "/session/%s/forward")
(def url-get-title "/session/%s/title")
(def url-get-url "/session/%s/url")
(def url-get-cookie "/session/%s/cookie")
(def url-get-cookie-by-name "/session/%s/cookie/%s")
(def url-get-active-element "/session/%s/element/active")
(def url-element-selected? "/session/%s/element/%s/selected")
(def url-get-element-attr "/session/%s/element/%s/attribute/%s")
(def url-get-element-prop "/session/%s/element/%s/property/%s")
(def url-get-element-text "/session/%s/element/%s/text")
(def url-get-element-name "/session/%s/element/%s/name")
(def url-element-enabled? "/session/%s/element/%s/enabled")
(def url-find-element "/session/%s/element")
(def url-find-elements "/session/%s/elements")
(def url-element-click! "/session/%s/element/%s/click")
(def url-element-clear! "/session/%s/element/%s/clear")
(def url-element-value! "/session/%s/element/%s/value")
(def url-execute-script-sync! "/session/%s/execute/sync")
(def url-screenshot "/session/%s/screenshot")

;; /session/{session id}/element/{element id}/element

(def params
  {:as :json
   :accept :json
   :content-type :json
   :form-params {}
   :debug true
   ;; :throw-exceptions false
   })

(def api-params
  {:as :json
   :accept :json
   :content-type :json
   :form-params {}
   :debug true
   ;; :throw-exceptions false
   })

(defn url-item [item]
  (cond
    (keyword? item) (name item)
    (string? item) item
    :else (str item)))

(defn get-path [& args]
  (str/join "/" (map url-item args)))

(defn get-status [server]
  (-> server
      (str "/" (get-path :status))
      (client/get params)
      :body
      :value))

(defn get-session [server opt]
  (-> server
      (str "/" (get-path :session))
      (client/post
       (assoc params :form-params opt))
      :body)) ;; todo erro

(defn delete-session [server session]
  (-> server
      (str "/" (get-path :session (:sessionId session)))
      (client/delete params)
      :body
      :value))

(defn go-url [server session url]
  (-> server
      (str "/" (get-path :session
                         (:sessionId session)
                         :url))
      (client/post
       (assoc params :form-params {:url url}))
      :body))

(defn go-back [session]
  (-> (str url-server
           (format url-go-back (:sessionId session)))
      (client/post params)
      :body))

(defn go-forward [session]
  (-> (str url-server
           (format url-go-forward (:sessionId session)))
      (client/post params)
      :body))

(defn get-title [session]
  (-> (str url-server
           (format url-get-title (:sessionId session)))
      (client/get params)
      :body
      :value))

(defn get-cookie [session]
  (-> (str url-server
           (format url-get-cookie (:sessionId session)))
      (client/get params)
      :body
      :value))

(defn get-cookie-by-name [session name]
  (-> (str url-server
           (format url-get-cookie-by-name
                   (:sessionId session)
                   name))
      (client/get params)
      :body
      :value
      first))

(defn get-active-element [session]
  (-> (str url-server
           (format url-get-active-element
                   (:sessionId session)))
      (client/get params)
      :body
      :value
      first
      second))

(defn element-selected? [session element]
  (-> (str url-server
           (format url-element-selected?
                   (:sessionId session)
                   element))
      (client/get params)
      :body
      :value))

(defn get-element-attr [session element attr]
  (-> (str url-server
           (format url-get-element-attr
                   (:sessionId session)
                   element
                   attr))
      (client/get params)
      :body
      :value))

(defn get-element-prop [session element prop]
  (-> (str url-server
           (format url-get-element-prop
                   (:sessionId session)
                   element
                   prop))
      (client/get params)
      :body
      :value))

(defn get-element-text [session element]
  (-> (str url-server
           (format url-get-element-text
                   (:sessionId session)
                   element))
      (client/get params)
      :body
      :value))

(defn get-element-name [session element]
  (-> (str url-server
           (format url-get-element-name
                   (:sessionId session)
                   element))
      (client/get params)
      :body
      :value))

(defn element-enabled? [session element]
  (-> (str url-server
           (format url-element-enabled?
                   (:sessionId session)
                   element))
      (client/get params)
      :body
      :value))

(defn element-click! [session element]
  (-> (str url-server
           (format url-element-click!
                   (:sessionId session)
                   element))
      (client/post params)
      :body))

(defn element-clear! [session element]
  (-> (str url-server
           (format url-element-clear!
                   (:sessionId session)
                   element))
      (client/post params)
      :body))

(defn element-value! [session element text]
  (-> (str url-server
           (format url-element-value!
                   (:sessionId session)
                   element))
      (client/post
       (assoc params :form-params {:value (vec text)}))
      :body))

(defn find-element [session locator selector]
  (-> (str url-server
           (format url-find-element
                   (:sessionId session)))
      (client/post
       (assoc params :form-params {:using locator :value selector}))
      :body
      :value
      first
      second
      )) ;; todo

(defn find-elements [session locator selector]
  (-> (str url-server
           (format url-find-elements
                   (:sessionId session)))
      (client/post
       (assoc params :form-params {:using locator :value selector}))
      :body
      :value ;; todo keys


      ))

(defn execute-script-sync! [session script & args]
  (-> (str url-server
           (format url-execute-script-sync!
                   (:sessionId session)))
      (client/post
       (assoc params :form-params {:script script :args args}))
      :body
      :value))

(defn inject-script! [session url]
  (let [script (str "var s = document.createElement('script');"
                    "s.type = 'text/javascript';"
                    "s.src = arguments[0];"
                    "document.head.appendChild(s);")]
    (execute-script-sync! session script url)))

(defn get-url [session]
  (-> (str url-server
           (format url-get-url
                   (:sessionId session)))
      (client/get params)
      :body
      :value))

(defn get-screenshot [session]
  (-> (str url-server
           (format url-screenshot
                   (:sessionId session)))
      (client/get params)
      :body
      :value
      .getBytes
      b64/decode
      ;; byte-array
      (io/copy "foo.png")
))

(defn is-url-matches? [session url-regex]
  (->> session
       get-url
       (re-matches url-regex)
       is))

(defn fill-in [browser selector text]
  (let [element (find-element browser :foo :bar)]
    (element-value! browser element text)))

(defn connect [host port])

;; DELETE	/session/{session id}

(def host "127.0.0.1")
(def port 4444)



(defn start-server [host port]
  {:host "127.0.0.1"
   :port 4444
   :url "http://127.0.0.1:4444"
   :process {:_process :todo
             :id 15324
             :in :todo
             :out :todo
             :terminate (-> :todo delay)}})

(def server "http://127.0.0.1:4444")

(defn make-server [])

(defn start-server [])

(defn start-session [browser opt]
  (-> browser
      :server
      :url
      (str (get-path :session))
      (client/post
       (assoc params :form-params opt))
      :body
      ))

(defn api [browser method path-args payload]
  (let [url (-> browser :server :url
                (str "/" (get-path path-args)))
        params (merge api-params
                      {:url url
                       :method method})]
    (-> params
        client/request
        :body)))

(defn go-url [browser url]
  (api browser
       :post
       [:session (-> browser :session :sessionId) :url]
       {:url url}))

(defn go-fwd [browser]
  (api browser :post
       [:session (-> browser :session :sessionId) :forward]
       {}))

(def browser {:server {:host "127.0.0.1"
                       :port 4444
                       :url "http://127.0.0.1:4444"}
              :process {:__process :todo
                        :id 15324
                        :env {}
                        :cmd "geckodriver"
                        :args []
                        :in :todo
                        :out :todo
                        :terminate (-> :todo delay)}
              :session {:sessionId "29538feb-c7b1-a848-9d8c-29f1aad1d008"
                        :value {:browserName "firefox"
                                :platformVersion "15.6.0"
                                :takesElementScreenshot true
                                :specificationLevel 0
                                :acceptSslCerts false
                                :appBuildId "20161208153507"
                                :processId 68410
                                :takesScreenshot true
                                :proxy {}
                                :raisesAccessibilityExceptions false
                                :command_id 1
                                :browserVersion "50.1.0"
                                :rotatable false
                                :platformName "darwin"
                                :version "50.1.0"
                                :XULappId "{ec8030f7-c20a-464f-9b0e-13a3a9e97384}"
                                :platform "DARWIN"}}})

(def session {:sessionId "29538feb-c7b1-a848-9d8c-29f1aad1d008"
              :value {:browserName "firefox"
                      :platformVersion "15.6.0"
                      :takesElementScreenshot true
                      :specificationLevel 0
                      :acceptSslCerts false
                      :appBuildId "20161208153507"
                      :processId 68410
                      :takesScreenshot true
                      :proxy {}
                      :raisesAccessibilityExceptions false
                      :command_id 1
                      :browserVersion "50.1.0"
                      :rotatable false
                      :platformName "darwin"
                      :version "50.1.0"
                      :XULappId "{ec8030f7-c20a-464f-9b0e-13a3a9e97384}"
                      :platform "DARWIN"}})

(deftest bar
  (let [host "127.0.0.1"
        port 4444
        server (make-server host port)
        process (make-process)

        ]))

(deftest foo
  (-> server
      get-status
      :ready
      is)
  (let [params {}
        session (-> server (get-session params))]
    (doto server
      (-> get-status :ready not is)
      (go-url session "http://ya.ru")
      (delete-session session)
      (-> get-status :ready is)
      )))

;; (deftest foo
;;   (let [browser :todo]
;;     (doto browser
;;       (goto "http://ya.ru")
;;       (go-back)
;;       (go-forward)
;;       (-> (has-text "hello") is)

;;       )

;;     (doto browser
;;       (-> )
;;       (has-text "hello")
;;       )

;;     (-> (has-text "hello") is)

;;     (is (has-text browser "hello"))

;;     (-> browser
;;         (goto "http://ya.ru")
;;         (go-back)
;;         (go-forward)
;;         (is-url-matches? #"todo")
;;         (click "todo")
;;         (fill-in "name" "Ivan")
;;         (fill-in "password" "test")
;;         (set-cookie "foo" "bar" "baz")
;;         (delete-cookie "foo")
;;         (has-cookie! "name")
;;         (has-text! "test")
;;         (click "submit")
;;         (wait-for-element "message")
;;         (exists "ready")
;;         (pause 1000)
;;         (assert )
;;         (end))))
