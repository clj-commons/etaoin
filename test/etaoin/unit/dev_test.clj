(ns etaoin.unit.dev-test
  "Unit tests for `etaoin.dev` performance log parsing.

  These are hermetic: they feed in Chrome DevTools performance log entries of the shape
  `etaoin.dev/get-performance-logs` returns, so no browser is required."
  (:require
   [cheshire.core :as json]
   [clojure.test :refer [deftest is testing]]
   [etaoin.dev :as dev]
   [etaoin.test-report]))

;; ---------------------------------------------------------------------------
;; helpers to build performance log entries as Chrome emits them

(defn- log-entry
  "Build a performance log entry carrying DevTools `method` and `params`.

  Chrome hands us `:message` as a JSON string, which is what `etaoin.dev` parses."
  [method params]
  {:level     :info
   :timestamp 1654315896000
   :message   (json/generate-string {:message {:method method :params params}
                                     :webview "9EB2E2C4E7C3D0C48D8E8D2B6E1F0A11"})})

(defn- process
  "Run raw log entries through the same private parsing step `get-performance-logs` uses."
  [entries]
  (mapv #'dev/process-log entries))

(defn- request-sent [request-id {:keys [url method type has-post-data headers]}]
  (log-entry "Network.requestWillBeSent"
             {:requestId  request-id
              :documentURL "https://www.google.com/"
              :type       (or type "Image")
              :request    {:url         url
                           :method      (or method "GET")
                           :headers     (or headers {:Referer "https://www.google.com/"})
                           :hasPostData has-post-data}}))

(defn- response-received [request-id {:keys [status headers mime remote-ip type]}]
  (log-entry "Network.responseReceived"
             {:requestId request-id
              :type      (or type "Image")
              :response  {:url             "https://www.google.com/images/x.webp"
                          :status          status
                          :statusText      ""
                          :headers         (or headers {:content-type "image/webp"
                                                        :content-length "660"})
                          :mimeType        (or mime "image/webp")
                          :remoteIPAddress (or remote-ip "142.250.185.68")
                          :remotePort      443}}))

(defn- loading-finished [request-id]
  (log-entry "Network.loadingFinished"
             {:requestId request-id :encodedDataLength 660}))

(defn- loading-failed [request-id]
  (log-entry "Network.loadingFailed"
             {:requestId request-id :errorText "net::ERR_ABORTED" :canceled false}))

(defn- request-by-id [requests id]
  (some #(when (= id (:id %)) %) requests))

;; ---------------------------------------------------------------------------

(deftest response-status-is-read-from-the-response
  (testing "status comes from the DevTools response object"
    ;; Regression test for #98: status was previously destructured from the response
    ;; *headers*, where DevTools never puts it, so :status was always nil.
    (let [requests (dev/logs->requests
                    (process [(request-sent "1.1" {:url "https://www.google.com/images/x.webp"})
                              (response-received "1.1" {:status 200})
                              (loading-finished "1.1")]))
          request  (request-by-id requests "1.1")]
      (is (= 1 (count requests)))
      (is (= 200 (-> request :response :status)))))

  (testing "status is not taken from the response headers"
    ;; A header literally named `status` must not win over the real response status.
    (let [requests (dev/logs->requests
                    (process [(request-sent "2.1" {:url "https://example.org/"})
                              (response-received "2.1" {:status 404
                                                        :headers {:status "599"
                                                                  :content-type "text/html"}})
                              (loading-finished "2.1")]))
          request  (request-by-id requests "2.1")]
      (is (= 404 (-> request :response :status)))))

  (testing "a string status is parsed to an int, anything unparseable is passed through"
    (let [status-of (fn [id raw]
                      (-> (dev/logs->requests
                           (process [(request-sent id {:url "https://example.org/"})
                                     (response-received id {:status raw})
                                     (loading-finished id)]))
                          (request-by-id id)
                          :response
                          :status))]
      (is (= 301 (status-of "3.1" "301")))
      (is (= "nonsense" (status-of "3.2" "nonsense")))
      (is (nil? (status-of "3.3" nil))))))

(deftest request-is-assembled-across-log-entries
  (testing "a full request/response/finish sequence produces the documented shape"
    (let [requests (dev/logs->requests
                    (process [(request-sent "4.1" {:url "https://www.google.com/images/x.webp"
                                                   :method "GET"
                                                   :type "Image"
                                                   :has-post-data false})
                              (response-received "4.1" {:status 200
                                                        :mime "image/webp"
                                                        :remote-ip "142.250.185.68"})
                              (loading-finished "4.1")]))
          request  (request-by-id requests "4.1")]
      (is (= {:state      4
              :id         "4.1"
              :type       :image
              :xhr?       false
              :url        "https://www.google.com/images/x.webp"
              :with-data? false
              :done?      true}
             (dissoc request :request :response)))
      (is (= {:method :get
              :headers {:Referer "https://www.google.com/"}}
             (:request request)))
      (is (= {:status    200
              :headers   {:content-type "image/webp" :content-length "660"}
              :mime      "image/webp"
              :remote-ip "142.250.185.68"}
             (:response request)))
      (is (dev/request-done? request))
      (is (not (dev/request-failed? request)))
      (is (dev/request-success? request)))))

(deftest failed-request-is-reported-as-failed
  (let [requests (dev/logs->requests
                  (process [(request-sent "5.1" {:url "https://example.org/gone" :type "XHR"})
                            (loading-failed "5.1")]))
        request  (request-by-id requests "5.1")]
    (is (dev/request-failed? request))
    (is (not (dev/request-done? request)))
    (is (not (dev/request-success? request)))))

(deftest ajax-requests-are-selected-by-type
  (let [logs (process [(request-sent "6.1" {:url "https://example.org/img.png" :type "Image"})
                       (response-received "6.1" {:status 200})
                       (loading-finished "6.1")
                       (request-sent "6.2" {:url "https://example.org/api" :type "XHR"})
                       (response-received "6.2" {:status 200 :type "XHR"})
                       (loading-finished "6.2")])]
    (is (= 2 (count (dev/logs->requests logs))))
    (is (= ["6.2"] (mapv :id (dev/logs->ajax logs))))
    (is (every? dev/ajax? (dev/logs->ajax logs)))))

(deftest non-network-logs-are-ignored
  (let [logs (process [(log-entry "Page.frameStartedLoading" {:frameId "F1"})
                       (request-sent "7.1" {:url "https://example.org/"})
                       (response-received "7.1" {:status 200})
                       (loading-finished "7.1")])]
    (is (= ["7.1"] (mapv :id (dev/logs->requests logs))))))
