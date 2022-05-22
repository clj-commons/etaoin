(ns etaoin.ide.api
  "
  Common Selenium IDE implementation.
  https://www.selenium.dev/selenium-ide/docs/en/api/commands
  "
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [clojure.test :refer [is]]
   [clojure.tools.logging :as log]
   [etaoin.api :refer :all]
   [etaoin.keys :as k]
   [etaoin.util :refer [defmethods]]))


(defn absolute-path?
  [path]
  (-> path
      str/lower-case
      (str/starts-with? "http")))


(defn str->var
  "
  Turn ${Foo} into Foo.
  "
  [var]
  (if (str/starts-with? var "$")
    (keyword (subs var 2 (-> var count dec)))
    (keyword var)))


(def special-keys
  {"ADD"          k/num-+
   "ALT"          k/alt-left
   "ARROW_LEFT"   k/arrow-left
   "ARROW_RIGHT"  k/arrow-right
   "ARROW_UP"     k/arrow-up
   "BACKSPACE"    k/backspace
   "BACK_SPACE"   k/backspace
   "CANCEL"       k/cancel
   "CLEAR"        k/clear
   "COMMAND"      k/command
   "CONTROL"      k/control-left
   "CTRL"         k/control-left
   "DECIMAL"      k/num-dot
   "DELETE"       k/delete
   "DIVIDE"       k/num-slash
   "DOWN"         k/arrow-down
   "END"          k/end
   "ENTER"        k/enter
   "EQUALS"       k/equal
   "ESCAPE"       k/escape
   "F1"           k/f1
   "F10"          k/f10
   "F11"          k/f11
   "F12"          k/f12
   "F2"           k/f2
   "F3"           k/f3
   "F4"           k/f4
   "F5"           k/f5
   "F6"           k/f6
   "F7"           k/f7
   "F8"           k/f8
   "F9"           k/f9
   "HELP"         k/help
   "HOME"         k/home
   "INSERT"       k/insert
   "LEFT"         k/arrow-left
   "LEFT_ALT"     k/alt-left
   "LEFT_CONTROL" k/control-left
   "LEFT_SHIFT"   k/shift-left
   "META"         k/meta-left
   "MULTIPLY"     k/num-*
   "N0"           k/num-0
   "N1"           k/num-1
   "N2"           k/num-2
   "N3"           k/num-3
   "N4"           k/num-4
   "N5"           k/num-5
   "N6"           k/num-6
   "N7"           k/num-7
   "N8"           k/num-9
   "N9"           k/num-9
   "NULL"         k/unidentified
   "NUMPAD0"      k/num-0
   "NUMPAD1"      k/num-1
   "NUMPAD2"      k/num-2
   "NUMPAD3"      k/num-3
   "NUMPAD4"      k/num-4
   "NUMPAD5"      k/num-5
   "NUMPAD6"      k/num-6
   "NUMPAD7"      k/num-7
   "NUMPAD8"      k/num-9
   "NUMPAD9"      k/num-9
   "NUM_PERIOD"   k/num-dot
   "NUM_PLUS"     k/num-+
   "NUM_DIVISION" k/num-slash
   "NUM_MULTIPLY" k/num-*
   "NUM_MINUS"    k/num--
   "PAGE_DOWN"    k/pagedown
   "PAGE_UP"      k/pageup
   "PAUSE"        k/pause
   "RETURN"       k/return
   "RIGHT"        k/arrow-right
   "SEMICOLON"    k/semicolon
   "SEP"          k/num-comma
   "SEPARATOR"    k/num-comma
   "SHIFT"        k/shift-left
   "SPACE"        k/space
   "SUBTRACT"     k/num--
   "TAB"          k/tab
   "UP"           k/arrow-up})


(defn fill-str-with-vars
  [string vars]
  (reduce (fn [acc [k v]]
            (let [pattern (re-pattern (format "\\$\\{%s\\}" (name k)))
                  value   (str v)]
              (str/replace acc pattern value))) string vars))


(defn gen-send-key-input
  [input]
  (let [pattern #"\$\{KEY_([^}]+)\}"
        keys    (->> (re-seq pattern input)
                     (map second)
                     set)]
    (reduce (fn [acc key]
              (let [pattern (re-pattern (format "\\$\\{KEY_%s\\}" key))
                    sp-key  (str (get special-keys key))]
                (str/replace acc pattern sp-key))) input keys)))


(defn gen-script-arguments
  [script vars]
  (reduce (fn [acc [k v]]
            (let [pattern (re-pattern (format "\\$\\{%s\\}" (name k)))
                  js-val  (str/replace (json/generate-string v) #"\"" "'")]
              (str/replace acc pattern js-val))) script vars))


(defn gen-expession-script
  [script vars]
  (str "return " (gen-script-arguments script vars)))


(defn make-query
  [target]
  (let [[type val] (str/split target #"=" 2)]
    (case type
      "css"      {:css val}
      "xpath"    {:xpath val}
      "linkText" {:tag :a :fn/has-text val}
      {:css (format "[%s]" target)})))


(defn make-absolute-url
  [base-url target]
  (let [target   (if (str/starts-with? target "/")
                   (subs target 1)
                   target)
        base-url (if (str/ends-with? base-url "/")
                   (subs base-url 0 (-> base-url count dec))
                   base-url)]
    (str base-url "/" target)))


(defn make-assert-msg
  [command actual expected]
  (format "\nAssert command:\"%s\"\nExpected: %s\nActual: %s"
          (name command) expected actual))


(defn dispatch-command
  #_{:clj-kondo/ignore [:unused-binding]}
  [driver command & [opt]]
  (some-> command :command))


(defmulti run-command dispatch-command)


(defmethod run-command
  :default
  [_driver command & _]
  (log/warnf "The \"%s\" command is not implemented" (:command command)))


(defmethod run-command
  :assert
  [_driver {:keys [target value command]} & [{vars :vars}]]
  (let [stored-value (str (get @vars (str->var target)))]
    (assert (= stored-value value) (make-assert-msg command stored-value value))))


(defmethods run-command
  [:assertAlert :assertConfirmation :assertPrompt]
  [driver {:keys [target command]} & [{_vars :vars}]]
  (let [alert-msg (get-alert-text driver)]
    (dismiss-alert driver)
    (assert (= alert-msg target) (make-assert-msg command alert-msg target))))


(defmethod run-command
  :assertChecked
  [driver {:keys [target command]} & [{_vars :vars}]]
  (let [actual (selected? driver (make-query target))]
    (assert actual (make-assert-msg command actual true))))


(defmethod run-command
  :assertNotChecked
  [driver {:keys [target command]} & [{_vars :vars}]]
  (let [actual (selected? driver (make-query target))]
    (assert (not actual) (make-assert-msg command actual false))))


(defmethod run-command
  :assertEditable
  [driver {:keys [target command]} & [{_vars :vars}]]
  (let [q      (make-query target)
        actual (and (enabled? driver (make-query target))
                    (nil? (get-element-attr driver q :readonly)))]
    (assert actual (make-assert-msg command actual true))))


(defmethod run-command
  :assertNotEditable
  [driver {:keys [target command]} & [{_vars :vars}]]
  (let [q      (make-query target)
        actual (and (enabled? driver (make-query target))
                    (nil? (get-element-attr driver q :readonly)))]
    (assert (not actual) (make-assert-msg command actual false))))


(defmethod run-command
  :assertElementPresent
  [driver {:keys [target command]} & [{_vars :vars}]]
  (let [actual (exists? driver (make-query target))]
    (assert actual (make-assert-msg command actual true))))


(defmethod run-command
  :assertElementNotPresent
  [driver {:keys [target command]} & [{_vars :vars}]]
  (let [actual (absent? driver (make-query target))]
    (assert actual (make-assert-msg command actual true))))


(defmethods run-command
  [:assertValue :assertSelectedValue]
  [driver {:keys [target value command]} & [{_vars :vars}]]
  (let [actual-val (get-element-value driver (make-query target))]
    (assert (= actual-val value)
            (make-assert-msg command actual-val value))))


(defmethod run-command
  :assertNotSelectedValue
  [driver {:keys [target value command]} & [{_vars :vars}]]
  (let [actual-val (get-element-value driver (make-query target))]
    (assert (not= actual-val value)
            (make-assert-msg command actual-val value))))


(defmethod run-command
  :assertText
  [driver {:keys [target value command]} & [{_vars :vars}]]
  (let [actual-text (get-element-text driver (make-query target))]
    (assert (= actual-text value)
            (make-assert-msg command actual-text value))))


(defmethod run-command
  :assertNotText
  [driver {:keys [target value command]} & [{_vars :vars}]]
  (let [actual-text (get-element-text driver (make-query target))]
    (assert (not= actual-text value)
            (make-assert-msg command actual-text value))))


(defmethod run-command
  :assertSelectedLabel
  [driver {:keys [target value command]} & [{_vars :vars}]]
  (let [q            (make-query target)
        selected-val (get-element-value driver q)
        option-el    (query driver q {:value selected-val})
        option-text  (get-element-text-el driver option-el)]
    (assert (= option-text value)
            (make-assert-msg command option-text value))))


(defmethod run-command
  :assertTitle
  [driver {:keys [target command]} & [{_vars :vars}]]
  (let [title (get-title driver)]
    (assert (= title target) (make-assert-msg command title target))))


(defmethod run-command
  :check
  [driver {:keys [target]} & [{_base-url :base-url}]]
  (let [q (make-query target)]
    (when-not (selected? driver q)
      (click driver q))))


(defmethod run-command
  :click
  [driver {:keys [target]} & [_opt]]
  (click driver (make-query target)))


(defmethod run-command
  :close
  [driver _ & _]
  (close-window driver))


(defmethod run-command
  :doubleClick
  [driver {:keys [target]} & [_opt]]
  (double-click driver (make-query target)))


(defmethod run-command
  :dragAndDropToObject
  [driver {:keys [target value]} & [_opt]]
  (drag-and-drop driver
                 (make-query target)
                 (make-query value)))


(defmethod run-command
  :echo
  [_driver {:keys [target]} {vars :vars}]
  (println (fill-str-with-vars target @vars)))


(defmethod run-command
  :executeScript
  [driver {:keys [target value]} & [{vars :vars}]]
  (let [result (js-execute driver (gen-script-arguments target @vars))]
    (when-not (str/blank? value)
      (swap! vars assoc (str->var value) result))
    result))


(defmethod run-command
  :open
  [driver {:keys [target]} & [{base-url :base-url}]]
  (if (absolute-path? target)
    (go driver target)
    (go driver (make-absolute-url base-url target))))


(defmethod run-command
  :pause
  [_driver {:keys [target]} & [_opt]]
  (wait (/ (Integer/parseInt target) 1000)))


;; TODO refactor select fn, add select by-value
(defmethods run-command
  [:select :addSelection :removeSelection]
  [driver {:keys [target value]} & [_opt]]
  (let [[type val] (str/split value #"=" 2)
        q          (make-query target)]
    (case type
      "label" (select driver q val)

      "index" (let [index (inc (Integer/parseInt val))] ;; the initial index in selenium is 0, in xpath and css selectors it is 1
                (click-el driver (query driver q {:tag :option :index index})))

      (click-el driver (query driver q (make-query value))))))


(defmethod run-command
  :selectFrame
  [driver {:keys [target]} & [_opt]]
  (cond
    (= target "relative=top")
    (switch-frame-top driver)

    (= target "relative=parent")
    (switch-frame-parent driver)

    (str/starts-with? target "index=")
    (switch-frame* driver (-> target
                              (str/split #"index=")
                              second
                              (Integer/parseInt)))

    :else (switch-frame driver (make-query target))))


(defmethod run-command
  :selectWindow
  [driver {:keys [target] :as command} & [{vars :vars}]]
  (cond
    (or (str/starts-with? target "handle=")
        (str/starts-with? target "name="))
    (let [handle-name (-> target
                          (str/split #"=")
                          second
                          str->var)]
      (switch-window driver (get @vars handle-name)))

    (str/starts-with? target "win_ser_")
    (let [index  (second (str/split target #"win_ser_"))
          index  (if (= index "local")
                   0
                   (Integer/parseInt index))
          handle (get (get-window-handles driver) index)]
      (switch-window driver handle))

    :else (throw (ex-info "The `select window` can only be called using handles"
                          {:command command}))))


(defmethod run-command
  :sendKeys
  [driver {:keys [target value]} & [{vars :vars}]]
  (fill driver (make-query target) (-> (gen-send-key-input value)
                                       (fill-str-with-vars @vars))))


(defmethod run-command
  :setWindowSize
  [driver {:keys [target]} & [_opt]]
  (let [[width height] (map #(Integer/parseInt %) (str/split target #"x"))]
    (set-window-size driver width height)))


(defmethod run-command
  :store
  [_driver {:keys [target value]} & [{vars :vars}]]
  (swap! vars assoc (str->var value) target))


(defmethod run-command
  :storeAttribute
  [driver {:keys [target value]} & [{vars :vars}]]
  (let [[locator attr-name] (str/split target "@" 2)
        attr-val            (get-element-attr driver (make-query locator) attr-name)]
    (swap! vars assoc (str->var value) attr-val)))


(defmethod run-command
  :storeText
  [driver {:keys [target value]} & [{vars :vars}]]
  (let [text (get-element-text driver (make-query target))]
    (swap! vars assoc (str->var value) text)))


(defmethod run-command
  :storeTitle
  [driver {:keys [value]} & [{vars :vars}]]
  (let [title (get-title driver)]
    (swap! vars assoc (str->var value) title)))


(defmethod run-command
  :storeValue
  [driver {:keys [target value]} & [{vars :vars}]]
  (let [val (get-element-value driver (make-query target))]
    (swap! vars assoc (str->var value) val)))


(defmethod run-command
  :storeWindowHandle
  [driver {:keys [target]} & [{vars :vars}]]
  (let [handle (get-window-handle driver)]
    (swap! vars assoc (str->var target) handle)))


(defmethod run-command
  :storeXpathCount
  [driver {:keys [target value]} & [{vars :vars}]]
  (let [cnt (count (find-elements* driver locator-xpath target))]
    (swap! vars assoc (str->var value) cnt)))


(defmethod run-command
  :submit
  [driver {:keys [target]} & [{_vars :vars}]]
  (fill-el driver (query (make-query target) {:tag :input}) k/enter))


(defmethod run-command
  :type
  [driver {:keys [target value]} & [{vars :vars}]]
  (fill driver (make-query target) (-> (gen-send-key-input value)
                                       (fill-str-with-vars @vars))))


(defmethod run-command
  :unCheck
  [driver {:keys [target]} & [{_base-url :base-url}]]
  (let [q (make-query target)]
    (when (selected? driver q)
      (click driver q))))


(defmethod run-command
  :verify
  [{:keys [target value command]} & [{vars :vars}]]
  (let [stored-value (str (get @vars (str->var target)))]
    (is (= stored-value value) (make-assert-msg command stored-value value))))


(defmethod run-command
  :verifyChecked
  [driver {:keys [target command]} & [{_vars :vars}]]
  (let [actual (selected? driver (make-query target))]
    (is (true? actual) (make-assert-msg command actual true))))


(defmethod run-command
  :verifyNotChecked
  [driver {:keys [target command]} & [{_vars :vars}]]
  (let [actual (selected? driver (make-query target))]
    (is (false? actual) (make-assert-msg command actual false))))


(defmethod run-command
  :verifyEditable
  [driver {:keys [target command]} & [{_vars :vars}]]
  (let [q      (make-query target)
        actual (and (enabled? driver (make-query target))
                    (nil? (get-element-attr driver q :readonly)))]
    (is (true? actual) (make-assert-msg command actual true))))


(defmethod run-command
  :verifyNotEditable
  [driver {:keys [target command]} & [{_vars :vars}]]
  (let [q      (make-query target)
        actual (and (enabled? driver (make-query target))
                    (nil? (get-element-attr driver q :readonly)))]
    (is (false? actual) (make-assert-msg command actual false))))


(defmethod run-command
  :verifyElementPresent
  [driver {:keys [target command]} & [{_vars :vars}]]
  (let [actual (exists? driver (make-query target))]
    (is (true? actual) (make-assert-msg command actual true))))


(defmethod run-command
  :verifyElementNotPresent
  [driver {:keys [target command]} & [{_vars :vars}]]
  (let [actual (absent? driver (make-query target))]
    (is (true? actual) (make-assert-msg command actual true))))


(defmethod run-command
  [:verifyValue :verifySelectedValue]
  [driver {:keys [target value command]} & [{_vars :vars}]]
  (let [actual-val (get-element-value driver (make-query target))]
    (is (= actual-val value)
        (make-assert-msg command actual-val value))))


(defmethod run-command
  :verifyNotSelectedValue
  [driver {:keys [target value command]} & [{_vars :vars}]]
  (let [actual-val (get-element-value driver (make-query target))]
    (is (not= actual-val value)
        (make-assert-msg command actual-val value))))


(defmethod run-command
  :verifyText
  [driver {:keys [target value command]} & [{_vars :vars}]]
  (let [actual-text (get-element-text driver (make-query target))]
    (is (= actual-text value)
        (make-assert-msg command actual-text value))))


(defmethod run-command
  :verifyNotText
  [driver {:keys [target value command]} & [{_vars :vars}]]
  (let [actual-text (get-element-text driver (make-query target))]
    (is (not= actual-text value)
        (make-assert-msg command actual-text value))))


(defmethod run-command
  :verifySelectedLabel
  [driver {:keys [target value command]} & [{_vars :vars}]]
  (let [q            (make-query target)
        selected-val (get-element-value driver q)
        option-el    (query driver q {:value selected-val})
        option-text  (get-element-text-el driver option-el)]
    (is (= option-text value)
        (make-assert-msg command option-text value))))


(defmethod run-command
  :verifyTitle
  [driver {:keys [target command]} & [{_vars :vars}]]
  (let [title (get-title driver)]
    (is (= title target) (make-assert-msg command title target))))


(defmethod run-command
  :waitForElementEditable
  [driver {:keys [target value]} & [{_vars :vars}]]
  (wait-enabled driver (make-query target)
                {:timeout (/ (Integer/parseInt value) 1000)}))


(defmethod run-command
  :waitForElementNotEditable
  [driver {:keys [target value]} & [{_vars :vars}]]
  (wait-disabled driver (make-query target)
                 {:timeout (/ (Integer/parseInt value) 1000)}))


(defmethod run-command
  :waitForElementPresent
  [driver {:keys [target value]} & [{_vars :vars}]]
  (wait-exists driver (make-query target)
               {:timeout (/ (Integer/parseInt value) 1000)}))


(defmethod run-command
  :waitForElementNotPresent
  [driver {:keys [target value]} & [{_vars :vars}]]
  (wait-absent driver (make-query target)
               {:timeout (/ (Integer/parseInt value) 1000)}))


(defmethod run-command
  :waitForElementVisible
  [driver {:keys [target value]} & [{_vars :vars}]]
  (wait-visible driver (make-query target)
                {:timeout (/ (Integer/parseInt value) 1000)}))


(defmethod run-command
  :waitForElementNotVisible
  [driver {:keys [target value]} & [{_vars :vars}]]
  (wait-invisible driver (make-query target)
                  {:timeout (/ (Integer/parseInt value) 1000)}))


(defmethod run-command
  :waitForText
  [driver {:keys [target value]} & [{_vars :vars}]]
  (let [q (make-query target)]
    (wait-visible driver q)
    (wait-has-text driver q value)))


(defmethods run-command
  [:webdriverChooseCancelOnVisibleConfirmation
   :webdriverChooseCancelOnVisiblePrompt]
  [driver {:keys [_target _value]} & [{_vars :vars}]]
  (dismiss-alert driver))


(defmethods run-command
  [:webdriverChooseOkOnVisibleConfirmation]
  [driver {:keys [_target _value]} & [{_vars :vars}]]
  (accept-alert driver))


;;
;; Control flow
;;

(defmethods run-command
  [:if :elseIf :repeatIf :while]
  [driver {:keys [target]} & [{vars :vars}]]
  (js-execute driver (gen-expession-script target @vars)))


(defmethods run-command
  [:do :end]
  [_ _ & _])


(defmethod run-command
  :forEach
  [_driver {:keys [target value]} & [{vars :vars}]]
  [(str->var value) (get @vars (str->var target))])


(defmethod run-command
  :times
  [_driver {:keys [target]} & [{_vars :vars}]]
  (Integer/parseInt target))


(defn log-command-message
  [{:keys [command target value]}]
  (cond->> ""
    (not (str/blank? value))
    (str (format " with value: '%s'" value))

    (not (str/blank? target))
    (str (format " on '%s'" target))

    "always"
    (str (format "Command: %s" (name command)))))


(defn run-command-with-log
  "
  A middleware wrapper on top of `run-command`.
  Does the same but logs all the events & errors
  if any occur.
  "
  [driver command & [opt]]
  (let [[msg result] (try
                       ["OK" (run-command driver command opt)]
                       (catch Exception e
                         [(format "Failed: %s" (.getMessage e)) e])
                       (catch java.lang.AssertionError e
                         [(format "Failed: %s" (.getMessage e)) e]))
        message      (str (log-command-message command) " / " msg)]

    (log/info message)

    (when-not (= msg "OK")
      (throw result))

    result))
