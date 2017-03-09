
[url-webdriver]: https://www.w3.org/TR/webdriver/
[url-wiki]: https://en.wikipedia.org/wiki/Etaoin_shrdlu#Literature
[url-tests]: https://github.com/igrishaev/etaoin/blob/master/test/etaoin/api_test.clj
[url-chromedriver]: https://sites.google.com/a/chromium.org/chromedriver/
[url-chromedriver-dl]: https://sites.google.com/a/chromium.org/chromedriver/downloads
[url-geckodriver-dl]: https://github.com/mozilla/geckodriver/releases
[url-phantom-dl]: http://phantomjs.org/download.html
[url-webkit]: https://webkit.org/blog/6900/webdriver-support-in-safari-10/
[url-doc]: http://grishaev.me/etaoin/

Pure Clojure implementation of [Webdriver][url-webdriver] protocol.

Use that library to automate a browser, test your frontend behaviour, simulate
human actions or whatever you want.

It's named after [Etaoin Shrdlu][url-wiki] -- a typing machine that became alive
after a mysteries note was produced on it.

## Benefits

- Selenium-free: no long dependencies, no tons of downloaded jars, etc.
- Lightweight, fast. Simple, easy to understand.
- Compact: just one main module with a couple of helpers.
- Declarative: the code is just a list of actions.

## Capabilities

- Currently supports Chrome, Firefox, Phantom.js and Safari (partially).
- May either connect to a remote driver or run it on your local machine.
- Run your unit tests directly from Emacs pressing `C-t t` as usual.
- Can imitate human-like behaviour (delays, typos, etc).

## Documentation

Please visit [Etaoin site][url-doc] for documentation. These are
autodoc-generated pages so you may also check the source code.

To see how to compose separated functions into the whole scenarios, check our
[unit tests][url-tests].

## Installation

Add the following into `:dependencies` vector in your `project.clj` file:

```
[etaoin "0.1.2"]
```
## Usage

### From REPL

The good news you may automate your browser directly from the REPL:

```clojure
(use 'etaoin.api)
(require '[etaoin.keys :as k])
(def driver (firefox)) ;; here, a Firefox window should appear

;; let's perform a quick Wiki session
(go driver "https://en.wikipedia.org/")
(wait-visible driver [{:id :simpleSearch} {:tag :input :name :search}])

;; search for something
(fill driver {:tag :input :name :search} "Clojure programming language")
(fill driver {:tag :input :name :search} k/enter)
(wait-visible driver {:class :mw-search-results})

;; I'm sure the first link is what I'm looking for
(click driver [{:class :mw-search-results} {:class :mw-search-result-heading} {:tag :a}])
(wait-visible driver {:id :firstHeading})

;; let's ensure
(get-url driver)
"https://en.wikipedia.org/wiki/Clojure"

(get-title driver)
"Clojure - Wikipedia"

(has-text? driver "Clojure")
true

;; navigate on history
(back driver)
(forward driver)
(refresh driver)
(get-title driver)
"Clojure - Wikipedia"

;; stops Firefox and HTTP server
(quit driver)
```

You see, any function requires a driver instance as the first argument. So you
may simplify it using `doto` macros:

```clojure
(def driver (firefox))
(doto driver
  (go "https://en.wikipedia.org/")
  (wait-visible [{:id :simpleSearch} {:tag :input :name :search}])
  ...
  (fill {:tag :input :name :search} k/enter)
  (wait-visible {:class :mw-search-results})
  ...
  (wait-visible {:id :firstHeading})
  (get-url)
  "https://en.wikipedia.org/wiki/Clojure"
  ...

  (quit))
```

In that case, your code looks like a DSL designed just for such purposes.

If any exception occurs during a browser session, the external process might
hang forever until you kill it manually. To prevent it, use `with-<browser>`
macros as follows:

```clojure
(with-firefox {} ff ;; additional options, bind name
  (doto ff
    (go "https://google.com")
    ...))
```

Whatever happens during a session, the process will be stopped anyway.

### Be patient

The main difference between a program and a human is that the first one
operates very fast. It means so fast, that sometimes a browser cannot render new
HTML in time. So after each action you need to put `wait-<something>` function
that just polls a browser checking for a predicate. O just `(wait <seconds>)` if
you don't care about optimization.

## Run tests

- Install Chrome and Firefox browsers downloading them from the official sites.
- Install Google [Chrome driver][url-chromedriver]:

  - `brew install chromedriver` for Mac users
  - or download compiled binaries from the [official site][url-chromedriver-dl].

- Install Geckodriver, a driver for Firefox:

  - `brew install geckodriver` for Mac users
  - or download it from the [Mozilla site][url-geckodriver-dl].

- Install Phantom.js browser:

  - `brew install phantomjs` For Mac users
  - or download it from the [official site][url-phantom-dl].

- Install Safari Driver (for Mac users):

  - update your Mac OS to El Captain using App Store;
  - set up Safari options as the [Webkit page][url-webkit] says (scroll down to
    "Running the Example in Safari" section).

- Now, check your installation launching any of these commands. For each
  command, an endless process with a local HTTP server should start.

  ```bash
  chromedriver
  geckodriver
  phantomjs --wd
  safaridriver -p 0
  ```

- If they work, you are ready to lunch the tests:

  ```bash
  lein test
  ```

- You'll see browser windows open and close in series. The tests use a local
  HTML file with a special layout to validate the most of the cases.

## Contributing

The project is open for your improvements and ideas. If any of unit tests fall
on your machine please submit an issue giving your OS version, browser and
console output.

## Roadmap

- Add MS Edge support.
- Add touch API tests.
- Add mobile support.
- Make unit tests to be more detailed.

## License

Copyright © 2017 Ivan Grishaev.

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
