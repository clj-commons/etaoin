(ns etaoin.ide
  (:require [cheshire.core :refer [parse-stream]]
            [clojure.java.io :as io]
            [etaoin.api :refer :all]
            [etaoin.keys :as k]
            [etaoin.util :refer [defmethods]]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

(defn absolute-path?
  [path]
  (-> path
      str/lower-case
      (str/starts-with? "http")))

(defn str->var
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
   "DECIMAL"      k/num-.
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
   "NUM_PERIOD"   k/num-.
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
  (let [base-url (if (str/ends-with? base-url "/")
                   (subs base-url 0 (-> base-url count dec))
                   base-url)]
    (str base-url target)))

(defn make-assert-msg
  [command actual expected]
  (format "\nAssert command:\"%s\"\nExpected: %s\nActual%s"
          command expected actual))

(defn dispatch-command
  [driver command & [opt]]
  (some-> command :command keyword))

(defmulti run-command dispatch-command)

(defmethod run-command
  :default
  [driver command & _]
  (log/warnf "The \"%s\" command is not implemented" (:command command)))

(defmethod run-command
  :assert
  [driver {:keys [target value command]} & [{vars :vars}]]
  (let [stored-value (str (get @vars (str->var target)))]
    (assert (= stored-value value) (make-assert-msg command stored-value value))))

(defmethods run-command
  [:assertAlert :assertConfirmation :assertPrompt]
  [driver {:keys [target command]} & [{vars :vars}]]
  (let [alert-msg (get-alert-text driver)]
    (assert (= alert-msg target) (make-assert-msg command alert-msg target))))

(defmethod run-command
  :assertChecked
  [driver {:keys [target command]} & [{vars :vars}]]
  (let [actual (selected? driver (make-query target))]
    (assert actual (make-assert-msg command actual true))))

(defmethod run-command
  :assertNotChecked
  [driver {:keys [target command]} & [{vars :vars}]]
  (let [actual (selected? driver (make-query target))]
    (assert (not actual) (make-assert-msg command actual false))))

(defmethod run-command
  :assertEditable
  [driver {:keys [target command]} & [{vars :vars}]]
  (let [q (make-query target)
        actual (and (enabled? driver (make-query target))
                    (nil? (get-element-attr driver q :readonly)))]
    (assert actual (make-assert-msg command actual true))))

(defmethod run-command
  :assertNotEditable
  [driver {:keys [target command]} & [{vars :vars}]]
  (let [q (make-query target)
        actual (and (enabled? driver (make-query target))
                    (nil? (get-element-attr driver q :readonly)))]
    (assert (not actual) (make-assert-msg command actual false))))

(defmethod run-command
  :assertElementPresent
  [driver {:keys [target command]} & [{vars :vars}]]
  (let [actual (exists? driver (make-query target))]
    (assert actual (make-assert-msg command actual true))))

(defmethod run-command
  :assertElementPresent
  [driver {:keys [target command]} & [{vars :vars}]]
  (let [actual (exists? driver (make-query target))]
    (assert (not actual) (make-assert-msg command actual false))))

(defmethod run-command
  [:assertValue :assertSelectedValue]
  [driver {:keys [target value command]} & [{vars :vars}]]
  (let [actual-val (get-element-attr driver (make-query target) :value)]
    (assert (= actual-val value)
            (make-assert-msg command actual-val value))))

(defmethod run-command
  :assertNotSelectedValue
  [driver {:keys [target value command]} & [{vars :vars}]]
    (let [actual-val (get-element-attr driver (make-query target) :value)]
    (assert (not= actual-val value)
            (make-assert-msg command actual-val value))))

(defmethod run-command
  :assertText
  [driver {:keys [target value command]} & [{vars :vars}]]
  (let [actual-text (get-element-text driver (make-query target))]
    (assert (= actual-text value)
            (make-assert-msg command actual-text value))))

(defmethod run-command
  :assertNotText
  [driver {:keys [target value command]} & [{vars :vars}]]
  (let [actual-text (get-element-text driver (make-query target))]
    (assert (not= actual-text value)
            (make-assert-msg command actual-text value))))

(defmethod run-command
  :assertSelectedLabel
  [driver {:keys [target value command]} & [{vars :vars}]]
  (let [q (make-query target)
        selected-val (get-element-attr driver q :value)
        option-el  (query driver q {:value selected-val})
        option-text (get-element-text-el driver option-el)]
    (assert (= option-text value)
            (make-assert-msg command option-text value))))

(defmethod run-command
  :assertTitle
  [driver {:keys [target command]} & [{vars :vars}]]
  (let [title (get-title driver)]
    (assert (= title target) (make-assert-msg command title target))))

(defmethod run-command
  :open
  [driver {:keys [target]} & [{base-url :base-url}]]
  (if (absolute-path? target)
    (go driver target)
    (go driver (make-absolute-url base-url target))))

(defmethod run-command
  :pause
  [driver {:keys [target]} & [{base-url :base-url}]]
  (wait (/ target 1000)))

(defmethod run-command
  :setWindowSize
  [driver {:keys [target]} & [opt]]
  (let [[width height] (map #(Integer/parseInt %) (str/split target #"x"))]
    (set-window-size driver width height)))

(defmethod run-command
  :type
  [driver {:keys [target value]} & [opt]]
  (fill driver (make-query target) (gen-send-key-input value)))

(defmethod run-command
  :click
  [driver {:keys [target]} & [opt]]
  (click driver (make-query target)))

(defmethod run-command
  :close
  [driver _ & _]
  (close-window driver))

(defmethod run-command
  :doubleClick
  [driver {:keys [target]} & [opt]]
  (double-click driver (make-query target)))

(defmethod run-command
  :dragAndDropToObject
  [driver {:keys [target value]} & [opt]]
  (drag-and-drop driver
                 (make-query target)
                 (make-query value)))

(defmethod run-command
  :executeScript
  [driver {:keys [target value]} & [{vars :vars}]]
  (let [result (js-execute driver target)]
    (when-not (str/blank? value)
      (swap! vars assoc (str->var value) result))
    result))

;; TODO refactor select fn, add select by-value
(defmethods run-command
  [:select :addSelection]
  [driver {:keys [target value]} & [opt]]
  (let [[type val] (str/split value #"=" 2)
        q          (make-query target)]
    (case type
      "label" (select driver q val)

      "index" (let [index (inc (Integer/parseInt val))] ;; the initial index in selenium is 0, in xpath and css selectors it is 1
                (click-el driver (query driver q {:tag :option :index index})))

      (click-el driver (query driver q (make-query value))))))

(defmethod run-command
  :selectFrame
  [driver {:keys [target]} & [opt]]
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
  :store
  [driver {:keys [target value]} & [{vars :vars}]]
  (swap! vars assoc (str->var value) target))

(defmethod run-command
  :storeWindowHandle
  [driver {:keys [target]} & [{vars :vars}]]
  (let [handle (get-window-handle driver)]
    (swap! vars assoc (str->var target) handle)))

(defmethod run-command
  :sendKeys
  [driver {:keys [target value]} & [opt]]
  (fill driver (make-query target) (gen-send-key-input value)))

(defmethod run-command
  :submit
  [driver {:keys [target]} & [{vars :vars}]]
  (fill-el driver (query (make-query target) {:tag :input}) k/enter))

(defmethod run-command
  :waitForElementEditable
  [driver {:keys [target value]} & [{vars :vars}]]
  (wait-enabled driver (make-query target) {:timeout (/ value 1000)}))

(defmethod run-command
  :waitForElementNotEditable
  [driver {:keys [target value]} & [{vars :vars}]]
  (wait-disabled driver (make-query target) {:timeout (/ value 1000)}))

(defmethod run-command
  :waitForElementPresent
  [driver {:keys [target value]} & [{vars :vars}]]
  (wait-exists driver (make-query target) {:timeout (/ value 1000)}))

(defmethod run-command
  :waitForElementNotPresent
  [driver {:keys [target value]} & [{vars :vars}]]
  (wait-absent driver (make-query target) {:timeout (/ value 1000)}))

(defmethod run-command
  :waitForElementVisible
  [driver {:keys [target value]} & [{vars :vars}]]
  (wait-visible driver (make-query target) {:timeout (/ value 1000)}))

(defmethod run-command
  :waitForElementNotVisible
  [driver {:keys [target value]} & [{vars :vars}]]
  (wait-invisible driver (make-query target) {:timeout (/ value 1000)}))

(defmethods run-command
  [:webdriverChooseCancelOnVisibleConfirmation
   :webdriverChooseCancelOnVisiblePrompt]
  [driver {:keys [target value]} & [{vars :vars}]]
  (dismiss-alert driver))

(defmethods run-command
  [:webdriverChooseOkOnVisibleConfirmation]
  [driver {:keys [target value]} & [{vars :vars}]]
  (accept-alert driver))



(defn run-ide-test
  [driver {:keys [commands]} & [{vars :vars :as opt}]]
  (doseq [{:keys [opensWindow
                  windowHandleName
                  windowTimeout]
           :as   command} commands]
    (if opensWindow
      (let [init-handles  (set (get-window-handles driver))
            _             (run-command driver command opt)
            _             (wait (/ windowTimeout 1000))
            final-handles (set (get-window-handles driver))
            handle        (first (clojure.set/difference final-handles init-handles))]
        (swap! vars assoc (str->var windowHandleName) handle))
      (run-command driver command opt))))

(defn get-tests-by-suite-id
  [suite-id id {:keys [suites tests]}]
  (let [test-ids    (-> (filter #(= suite-id (id %)) suites)
                        first
                        :tests
                        set)
        suite-tests (filter #(test-ids (:id %)) tests)]
    suite-tests))

(defn find-tests
  [{:keys [test-id test-ids suite-id suite-ids test-name suite-name]}
   {:keys [suites tests] :as parsed-file}]
  (let [test-ids    (cond-> #{}
                      test-id    (conj (first (filter #(= test-id (:id %)) tests)))
                      test-name  (conj (first (filter #(= test-name (:name %)) tests)))
                      suite-id   (into (get-tests-by-suite-id suite-id :id parsed-file))
                      suite-name (into (get-tests-by-suite-id suite-name :name parsed-file))
                      test-ids   (into (filter #((set test-ids) (:id %)) tests))
                      suite-ids  (into (mapcat #(get-tests-by-suite-id % :id parsed-file) suite-ids)))
        tests-found (filter test-ids tests)]
    (if (empty? tests-found)
      tests
      tests-found)))

(defn run-ide-script
  [driver path & [opt]]
  (let [parsed-file (with-open [rdr (io/reader path)]
                      (parse-stream rdr true))
        opt-search  (select-keys opt [:test-name :test-id :test-ids
                                      :suite-name :suite-id :suite-ids])
        tests-found (find-tests opt-search parsed-file)
        opt         (merge {:base-url (:url parsed-file)
                            :vars     (atom {})}
                           opt)]
    (doseq [test tests-found]
      (run-ide-test driver test opt))))
