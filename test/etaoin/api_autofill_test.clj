(ns etaoin.api-autofill-test
  "Browser AutoFill behaviour that etaoin's fill tests depend on.

  A browser that moves focus on its own corrupts `fill-active`, surfacing much later
  as a wrong field value. Asserted here per browser, so a uniform pass does not hide
  which browser actually has the quirk.

  No WebKit bug tracks it: ignoring `autocomplete=off` on password forms is intended
  behaviour. Safari half of https://github.com/clj-commons/etaoin/issues/646"
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [etaoin.api :as e]
   [etaoin.api-test :as api-test]
   [etaoin.test-report]))

(use-fixtures :once api-test/test-server)
(use-fixtures :each api-test/fixture-browsers)

;; Safari AutoFill fires ~260ms after load (Safari 26.5.2)
(def ^:private autofill-window-ms 1000)
(def ^:private suggestion-window-ms 2000)

(def ^:private fixtures
  "Pages differ only in where the password field sits. `:keeps-focus` is what each
  browser does. Safari classifies a password next to a text input as a login form;
  proximity is the trigger, not form membership."
  [{:page        "autofill-off.html"
    :markup      "in form, next to username, autocomplete=off"
    :keeps-focus {:safari false, :chrome true, :firefox true, :edge true}}
   {:page        "autofill-distant-password.html"
    :markup      "in form, after submit button, autocomplete=off"
    :keeps-focus {:safari true, :chrome true, :firefox true, :edge true}}
   {:page        "autofill-detached-password.html"
    :markup      "outside form, via form= attribute"
    :keeps-focus {:safari true, :chrome true, :firefox true, :edge true}}
   {:page        "autofill-new-password.html"
    :markup      "next to username, autocomplete=new-password"
    :keeps-focus {:safari true, :chrome true, :firefox true, :edge true}}])

;; test.html uses new-password. Moving the field works too, but controls serialize in
;; DOM order, so reordering changes the query string its submit assertions check.

(defn- active-id [driver]
  (e/js-execute driver "var a = document.activeElement;
                        return a ? (a.id || a.tagName) : 'null'"))

(defn- focus-then-wait
  "Click the password field on `page`, wait out the AutoFill window, return what is
  focused.

  Safari drops clicks (https://github.com/clj-commons/etaoin/issues/683), leaving BODY
  focused. Reload and retry rather than reporting a dropped click as an AutoFill result."
  [driver page]
  (loop [tries 3]
    (e/go driver (api-test/test-server-url page))
    (e/wait-visible driver {:id :af-end})
    (e/click driver :af-pass)
    (if (and (= "BODY" (active-id driver)) (pos? tries))
      (recur (dec tries))
      (do (Thread/sleep (long autofill-window-ms))
          (active-id driver)))))

(deftest autofill-moves-focus-only-on-safari-and-only-next-to-a-username
  (doseq [{:keys [page markup keeps-focus]} fixtures]
    (testing markup
      (let [browser (e/driver-type api-test/*driver*)
            keeps?  (get keeps-focus browser ::unknown)
            landed  (focus-then-wait api-test/*driver* page)]
        (cond
          (= ::unknown keeps?)
          (is false (format "%s: no recorded behaviour for %s, landed on %s"
                            (name browser) page landed))

          keeps?
          (is (= "af-pass" landed)
              (format "%s should keep focus with %s but moved it to %s; fill-active now types into the wrong element"
                      (name browser) markup landed))

          :else
          (is (= "af-user" landed)
              (format "%s should ignore autocomplete=off and pull focus to the username field, but focus stayed on %s; if fixed, test.html no longer needs new-password"
                      (name browser) landed)))))))

(deftest automation-clicks-count-as-genuine-user-activation
  (testing "a WebDriver click is user-initiated as far as the browser is concerned"
    ;; Gives the suggestion test its meaning: an absent suggestion could otherwise just
    ;; mean the browser never believed a user touched the field.
    (e/go api-test/*driver* (api-test/test-server-url "autofill-new-password.html"))
    (e/wait-visible api-test/*driver* {:id :af-end})
    (let [activation #(e/js-execute api-test/*driver*
                                    "return navigator.userActivation
                                              ? navigator.userActivation.hasBeenActive
                                              : nil")]
      (if (nil? (activation))
        (is (not (e/driver? api-test/*driver* :safari))
            "Safari should support navigator.userActivation")
        (do
          (is (false? (activation)) "no activation before any input")
          (e/click api-test/*driver* :af-pass)
          (is (true? (activation)) "a WebDriver click is a genuine user activation"))))))

(deftest automation-sees-no-strong-password-suggestion
  (testing "dwelling on a focused new-password field does not fill it"
    ;; Hand-driven Safari offers a strong password here; automation does not, because an
    ;; Automation window "cannot access Safari's normal browsing history, AutoFill data,
    ;; or other sensitive information"
    ;; -- https://webkit.org/blog/6900/webdriver-support-in-safari-10/
    ;; The classification still runs though: the focus theft above happens in the same
    ;; window. Only the data is withheld.
    ;;
    ;; The panel is native UI, invisible to the DOM and to screenshots, so this asserts
    ;; only that no value appears and typing still lands.
    (e/go api-test/*driver* (api-test/test-server-url "autofill-new-password.html"))
    (e/wait-visible api-test/*driver* {:id :af-end})
    (e/click api-test/*driver* :af-pass)
    (Thread/sleep (long suggestion-window-ms))
    (is (= "" (e/get-element-value api-test/*driver* :af-pass))
        (format "%s filled the field under automation" (name (e/driver-type api-test/*driver*))))
    (e/fill-active api-test/*driver* "Secret123")
    (is (= "Secret123" (e/get-element-value api-test/*driver* :af-pass))
        "typing after the dwell should still land in the password field")))
