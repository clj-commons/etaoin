(ns webdriver.drafts
  ;; (:require [clj-http.client :as client]
  ;;           [clojure.string :as str]
  ;;           [clojure.data.codec.base64 :as b64]
  ;;           [clojure.java.io :as io]
  ;;           [clojure.test :refer [is deftest]])
  )


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

(deftest baz
  (doto (make-browser)
    (click "sdfs")
    (wait-for-element-visible "sdfsdf" 1)

    ))

;; browser
;; +            .click('//a[@id="sidebar_accounts"]')
;; +            .waitForElementVisible('//button[@id="invite_user_button"]', 1000)
;; +            .click('//button[contains(@id,"accounts_")]')
;; +            .waitForElementVisible('//ul[contains(@for, "accounts_")]', 1000)
;; +            .click("//li[text() = 'Manage']")
;; +            .waitForElementVisible('//dialog[contains(@class, "invite-user")]', 1000)
;; +            .setValue('//input[@id="lastname"]', "Jackson")
;; +            .click('//button[text()="Update"]')
;; +            .waitForElementVisible('//div[@class="flash-success"]', 1000)
;; +            .assert.containsText("//div[@id='content']", "Jackson");
;; +          browser.end();


(deftest foo
  (with-foo (make-server)
    (with-bar (sdf)
      (go "ya.ru")
      (reload)
      (go-back)
      (go-fwd)
      (fill-in "search" "What is clojure?")
      (wait 3)
      (is-exists? "navbar")
      (-> (exists? "navbar") is)
      (wait-for-element "message" {:timeout 30})
      (click "submit")
      )
        (with-trx sdfsdf))
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
