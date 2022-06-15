(require '[etaoin.api :as e]
         '[etaoin.keys :as k])

(def driver (e/chrome))

(e/go driver "https://en.wikipedia.org/")

(def query-search {:tag :input :name :search})

(e/wait-visible driver [{:id :simpleSearch} query-search])

;; search for something
(e/fill driver query-search "Clojure programming language")

(e/clear driver query-search)

(e/fill-human driver query-search "Clojure programming language")

(e/fill driver query-search k/enter)
(e/wait-visible driver {:class :mw-search-results})

(e/scroll-down driver 100)

;; I'm sure the first link is what I was looking for
(e/click driver [{:class :mw-search-results}
                 {:class :mw-search-result-heading}
                 {:tag :a}])

(e/wait-visible driver {:id :firstHeading})

(e/get-url driver)
(e/get-title driver)

(e/has-text? driver "Clojure")

;; navigate on history
(e/back driver)
(e/forward driver)
(e/refresh driver)

(e/screenshot driver "clojure.png")

;; stops Firefox and HTTP server
(e/quit driver)
