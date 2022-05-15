#_{:clj-kondo/ignore [:use]}
(use 'etaoin.api)
(require '[etaoin.keys :as k])

(def driver (chrome))

(go driver "https://en.wikipedia.org/")

(def query-search {:tag :input :name :search})

(wait-visible driver [{:id :simpleSearch} query-search])

;; search for something
(fill driver query-search "Clojure programming language")

(clear driver query-search)

(fill-human driver query-search "Clojure programming language")

(fill driver query-search k/enter)
(wait-visible driver {:class :mw-search-results})

(scroll-down driver 100)

;; I'm sure the first link is what I was looking for
(click driver [{:class :mw-search-results}
               {:class :mw-search-result-heading}
               {:tag :a}])

(wait-visible driver {:id :firstHeading})

(get-url driver)
(get-title driver)

(has-text? driver "Clojure")

;; navigate on history
(back driver)
(forward driver)
(refresh driver)

(screenshot driver "clojure.png")

;; stops Firefox and HTTP server
(quit driver)
