(ns etaoin.unit.wait-test
  "Unit tests for `etaoin.api/wait-predicate` timing.

  Hermetic: the predicates here are plain functions, so no driver and no browser
  are required."
  (:require
   [clj-commons.slingshot :refer [throw+ try+]]
   [clojure.test :refer [deftest is testing]]
   [etaoin.api :as e]
   [etaoin.test-report]))

(defn- timed
  "Run `f`, return `[result elapsed-ms]`.

  Result is `{:ok <value>}` or, on timeout, `{:timeout <ex-data>}`. Anything else
  `f` throws is left to escape."
  [f]
  (let [start (System/nanoTime)
        res   (try+ {:ok (f)} (catch [:type :etaoin/timeout] data {:timeout data}))]
    [res (quot (- (System/nanoTime) start) 1000000)]))

(defn- slow-false
  "A predicate that always fails and takes `ms` to do it, like a WebDriver round trip."
  [ms]
  (fn [] (Thread/sleep (long ms)) false))

;; ---------------------------------------------------------------------------

(deftest timeout-is-real-elapsed-time-not-a-count-of-intervals
  (testing "a predicate slower than the interval does not extend the timeout"
    ;; Before this was deadline-based, `:time-rest` only ever decreased by
    ;; `:interval`, so time spent inside `pred` was free. With a 200ms predicate
    ;; and a 50ms interval, a nominal 1s timeout ran for roughly 5s.
    (let [[res ms] (timed #(e/wait-predicate (slow-false 200)
                                             {:timeout 1 :interval 0.05}))]
      (is (:timeout res) "should have timed out")
      (is (< ms 1600)
          (str "nominal timeout was 1000ms, actually waited " ms "ms"))))

  (testing "a fast predicate still honours the timeout"
    (let [[res ms] (timed #(e/wait-predicate (constantly false)
                                             {:timeout 1 :interval 0.05}))]
      (is (:timeout res))
      (is (<= 900 ms 1600)
          (str "expected roughly 1000ms, waited " ms "ms")))))

(deftest timeout-data-reports-what-actually-happened
  (let [pred    (slow-false 100)
        [res _] (timed #(e/wait-predicate pred {:timeout  1
                                                :interval 0.05
                                                :message  "no luck"}))
        data    (:timeout res)]
    (is (= :etaoin/timeout (:type data)))
    (is (= "no luck" (:message data)))
    (is (= 1 (:timeout data)))
    (is (= 0.05 (:interval data)))
    (is (identical? pred (:predicate data))
        "the failing predicate travels with the exception")
    (testing "elapsed-ms reflects the real wait"
      (is (<= 900 (:elapsed-ms data) 1600)))
    (testing "times counts calls actually made to pred"
      ;; ~1000ms budget over ~150ms per cycle, so several calls but nowhere near
      ;; the 20 an interval-counting implementation would have allowed.
      (is (pos? (:times data)))
      (is (< (:times data) 20)))))

(deftest a-succeeding-predicate-returns-immediately
  (testing "success is not delayed by the timeout"
    (let [[res ms] (timed #(e/wait-predicate (constantly :found) {:timeout 30}))]
      (is (= {:ok :found} res))
      (is (< ms 100) (str "returned in " ms "ms")))))

(deftest a-predicate-that-succeeds-on-a-later-call-still-returns-its-value
  (let [calls (atom 0)
        pred  #(when (< 2 (swap! calls inc)) :eventually)
        [res _] (timed #(e/wait-predicate pred {:timeout 5 :interval 0.01}))]
    (is (= {:ok :eventually} res))
    (is (= 3 @calls))))

(deftest an-http-error-from-pred-counts-as-a-failed-attempt
  (testing "pred throwing an http error is treated as falsey, not propagated"
    (let [calls    (atom 0)
          pred     (fn []
                     (swap! calls inc)
                     (throw+ {:type :etaoin/http-error}))
          [res ms] (timed #(e/wait-predicate pred {:timeout 0.2 :interval 0.05}))]
      (is (:timeout res) "should time out rather than let the http error escape")
      (is (pos? @calls))
      (is (= @calls (-> res :timeout :times)))
      (is (< ms 1000) (str "waited " ms "ms")))))

(deftest zero-timeout-calls-pred-once-and-does-not-sleep
  (testing "a zero timeout still gives pred one chance"
    ;; `:interval` is deliberately huge: an implementation that sleeps before
    ;; noticing it is out of time would take 5s to get here.
    (let [calls    (atom 0)
          [res ms] (timed #(e/wait-predicate (fn [] (swap! calls inc) false)
                                             {:timeout 0 :interval 5}))]
      (is (:timeout res))
      (is (= 1 @calls))
      (is (= 1 (-> res :timeout :times)))
      (is (< ms 1000) (str "returned in " ms "ms"))))

  (testing "a negative timeout also gives pred one chance"
    (let [calls (atom 0)]
      (try+
        (e/wait-predicate (fn [] (swap! calls inc) false) {:timeout -1 :interval 5})
        (catch [:type :etaoin/timeout] _))
      (is (= 1 @calls)))))
