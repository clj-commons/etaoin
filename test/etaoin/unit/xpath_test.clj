(ns etaoin.unit.xpath-test
  (:require [clojure.test :refer [deftest testing is]]
            [etaoin.test-report]
            [etaoin.impl.xpath :as xpath]))

(def xpath-samples
  [[{:tag :a} ".//a"]

   [{:tag :form :method :GET :class :message :index 5}
    ".//form[@method=\"GET\"][@class=\"message\"][5]"]

   [{:tag :button :fn/text "Press Me"} ".//button[text()=\"Press Me\"]"]

   [{:fn/has-text "download"} ".//*[contains(text(), \"download\")]"]

   [{:fn/has-string "download"} ".//*[contains(string(), \"download\")]"]

   [{:tag :div :fn/has-class "overlay"} ".//div[contains(@class, \"overlay\")]"]

   [{:fn/has-classes [:active :sticky :marked]}
    ".//*[contains(@class, \"active\")][contains(@class, \"sticky\")][contains(@class, \"marked\")]"]

   [{:tag :input :fn/disabled true} ".//input[@disabled=true()]"]

   [{:tag :input :fn/enabled false} ".//input[@enabled=false()]"]

   [{:tag :a :fn/link "yandex"} ".//a[contains(@href, \"yandex\")]"]
   ])

(deftest test-xpath-expand
  (doseq [[q xpath] xpath-samples]
    (testing (format "XPath %s" q)
      (is (= (xpath/expand q) xpath)))))
