[url-webdriver]: https://www.w3.org/TR/webdriver/
[url-wiki]: https://en.wikipedia.org/wiki/Etaoin_shrdlu#Literature
[url-tests]: https://github.com/igrishaev/etaoin/blob/master/test/etaoin/api_test.clj
[url-doc]: http://grishaev.me/etaoin/
[url-slack]: https://clojurians.slack.com/messages/C7KDM0EKW/

# Etaion

Pure Clojure implementation of [Webdriver][url-webdriver] protocol. Use that
library to automate a browser, test your frontend behaviour, simulate human
actions or whatever you want.

It's named after [Etaoin Shrdlu][url-wiki] -- a typing machine that became alive
after a mysteries note was produced on it.

# Table of Contents

<!-- toc -->

- [Benefits](#benefits)
- [Capabilities](#capabilities)
- [Who uses it?](#who-uses-it)
- [Documentation](#documentation)
- [Installation](#installation)
- [Basic Usage](#basic-usage)
  * [From REPL](#from-repl)
- [Advanced Usage](#advanced-usage)
  * [Working with multiple elements](#working-with-multiple-elements)
  * [File uploading](#file-uploading)
  * [Using headless driver](#using-headless-driver)
  * [Auto-save screenshots in case of exception](#auto-save-screenshots-in-case-of-exception)
  * [Additional parameters](#additional-parameters)
  * [Be patient (wait, with-wait etc)](#be-patient-wait-with-wait-etc)
- [Writing Integration Tests For Your Application](#writing-integration-tests-for-your-application)
  * [Basic fixture](#basic-fixture)
  * [Multi-Driver Fixtures](#multi-driver-fixtures)
  * [Postmortem Handler To Collect Artifacts](#postmortem-handler-to-collect-artifacts)
  * [Running Tests By Tag](#running-tests-by-tag)
- [Installing Drivers](#installing-drivers)
- [Troubleshooting](#troubleshooting)
  * [Calling maximize function throws an error](#calling-maximize-function-throws-an-error)
  * [Clicking On Non-Visible Element](#clicking-on-non-visible-element)
  * [Unpredictable errors in Chrome when window is not active](#unpredictable-errors-in-chrome-when-window-is-not-active)
- [Contributors](#contributors)
- [License](#license)

<!-- tocstop -->

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

## Who uses it?

- [Flyerbee](https://www.flyerbee.com/)
- [Room Key](https://www.roomkey.com/)

You are welcome to submit your company into that list.

## Documentation

- [API docs][url-doc]
- [Slack channel][url-slack]
- [Unit tests][url-tests]

## Installation

Add the following into `:dependencies` vector in your `project.clj` file:

```
[etaoin "0.1.9-SNAPSHOT"]
```

## Basic Usage

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

;; I'm sure the first link is what I was looking for
(click driver [{:class :mw-search-results} {:class :mw-search-result-heading} {:tag :a}])
(wait-visible driver {:id :firstHeading})

;; let's ensure
(get-url driver) ;; "https://en.wikipedia.org/wiki/Clojure"

(get-title driver) ;; "Clojure - Wikipedia"

(has-text? driver "Clojure") ;; true

;; navigate on history
(back driver)
(forward driver)
(refresh driver)
(get-title driver)
;; "Clojure - Wikipedia"

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
(with-firefox {} ff ;; additional options first, then bind name
  (doto ff
    (go "https://google.com")
    ...))
```

Whatever happens during a session, the process will be stopped anyway.

## Advanced Usage

### Working with multiple elements

Most of the functions work with a term that return first single element. For
example, `(click driver {:tag :div})` will click on the first `div` tag found on
the page. Therefore it's better to operate on element's IDs rather then classes
to prevent strange behaviour.

In case your really need to get multiple elements and process them in batch, use
`query-all` function with other ones that names end with `-el`. These functions
are to work with machine wise elements represented by long driver-specific
string values.

Here is a example of how to get all the links from the page:

```clojure
(def driver (firefox))
(go driver "http://wikipedia.org")
(let [els (query-all driver {:tag :a})
      ;; els is vector of strings smth like "280abeaf-27ec-5544-8634-b2cfe86a58a6"
      get-link #(get-element-attr-el driver % :href)]
  (mapv get-link els))
;; returns ["//ru.wikipedia.org/" "//en.wikipedia.org/" etc ... ]
```

### File uploading

When selecting files to upload, you are not allowed to navigate through the
system file dialog. Instead, use the `upload-file` function to attach a local
file to a file input widget. The function takes either a full path as a string
or a native `java.io.File` instance. The file should exist or you'll get an
exception otherwise. Usage example:

```clojure
(def driver (chrome))

;; open a web page that serves uploaded files
(go driver "http://nervgh.github.io/pages/angular-file-upload/examples/simple/")

;; search for input widgets, there are a couple of them
(query-all driver {:tag :input :type :file})

;; upload an image with the first one
(upload-file driver {:tag :input :type :file} "/Users/ivan/Downloads/sample.png")
```

### Using headless driver

Since version 59, Google Chrome officially supports headless mode. It's when it
works without opening a UI window so it is possible to run such a driver on
servers without a display device.

Headless mode uses the standard `chromedriver`, the difference is only in
additional parameters passed to Chrome.

To use headless driver in your code, use either `(headless)` function or
`(with-headless)` macros as well. Perhaps you will need to take more screenshots
to see that's going on under the hood:

```clojure
(doto driver
  ;; ... clicks, etc
  (when-headless
    (screenshot driver "/in/the/middle/of/test.png"))
  ;; continue
  )
```

### Auto-save screenshots in case of exception

Sometimes, it might be difficult to discover what went wrong during the last UI
tests session. To keep some postmortem evident on your disk, wrap the code
block with `with-postmortem` macros:

```clojure
(def driver (firefox))
(with-postmortem driver {:dir "/Users/ivan/artifacts"}
  (click driver :non-existing-element))
```

An exception will rise, but in `/Users/ivan/artifacts` there will be two files:

- `firefox-127.0.0.1-4444-2017-03-26-02-45-07.png`: an actual screenshot of the
  browser's page;

- `firefox-127.0.0.1-4444-2017-03-26-02-45-07.html`: an actual browser's HTML
  content.

The filename template is `<browser>-<host>-<port>-<datetime>.ext`.

### Additional parameters

When running a driver instance, a map of additional parameters might be passed
to tweak the browser's behaviour:

```clojure
(def driver (chrome {:path "/path/to/driver/binary"}))
```

Below, here is a map of parameters the library support. All of them might be
skipped or have nil values. Some of them, if not passed, are taken from the
`defaults` map.

```clojure
{;; Host and port for webdriver's process. Both are taken from defaults
 ;; when are not passed. If you pass a port that has been already taken,
 ;; the library will try to take a random one instead.
 :host "127.0.0.1"
 :port 9999

 ;; Path to webdriver's binary file. Taken from defaults when not passed.
 :path "/Users/ivan/Downloads/geckodriver"

 ;; Extra command line arguments sent to the browser's process. See your browser's
 ;; supported flags.
 :args ["--incognito" "--app" "http://example.com"]

 ;; Extra command line arguments sent to the webdriver's process.
 :args-driver ["-b" "/path/to/firefox/binary"]

 ;; Env variables sent to the driver's process. Not processed yet.
 :env {:MOZ_CRASHREPORTER_URL "http://test.com"}

 ;; Initial window size.
 :size [1024 680]

 ;; Default URL to open. Works only in FF for now.
 :url "http://example.com"

 ;; Driver-specific options. Make sure you have read the docs before setting them.
 :capabilities {:chromeOptions {:args ["--headless"]}}}
```

### Be patient (wait, with-wait etc)

The main difference between a program and a human is that the first one
operates very fast. It means so fast, that sometimes a browser cannot render new
HTML in time. So after each action you need to put `wait-<something>` function
that just polls a browser checking for a predicate. O just `(wait <seconds>)` if
you don't care about optimization.

The `with-wait` macro might be helpful when you need to prepend each action
with `(wait n)`. For example, the following form

```clojure
(with-chrome {} driver
  (with-wait 3
    (go driver "http://site.com")
    (click driver {:id "search_button"})))
```

turns into something like this:

```clojure
(with-chrome {} driver
  (wait 3)
  (go driver "http://site.com")
  (wait 3)
  (click driver {:id "search_button"}))
```

and thus returns the result of the last form of the original body.

## Writing Integration Tests For Your Application

### Basic fixture

To make your test not depend on each other, you need to wrap them into a fixture
that will create a new instance of a driver and shut it down properly at the end
if each test.

Good solution might be to have a global variable (unbound by default) that will
point to the target driver during the tests.

```clojure
(ns project.test.integration
  "A module for integration tests"
  (:require [clojure.test :refer :all]
            [etaoin.api :refer :all]))

(def ^:dynamic
  "Current driver"
  *driver*)

(defn fixture-driver
  "Executes a test running a driver. Bounds a driver
   with the global *driver* variable."
  [f]
  (with-chrome {} driver
    (binding [*driver* driver]
      (f))))

(use-fixtures
  :each ;; start and stop driver for each test
  fixture-driver)

;; now declare your tests

(deftest ^:integration
  test-some-case
  (doto *driver*
    (go url-project)
    (click :some-button)
    (refresh)
    ...
    ))
```

### Multi-Driver Fixtures

In the example above, we examined a case when you run tests against a single
type of driver. However, you may want to test your site on multiple drivers,
say, Chrome and Firefox. In that case, your fixture may become a bit more
complex:

```clojure

(def driver-type [:firefox :chrome])

(defn fixture-drivers [f]
  (doseq [type driver-types]
    (with-driver type {} driver
      (binding [*driver* driver]
        (testing (format "Testing in %s browser" (name type))
          (f))))))
```

Now, each test will be run twice in both Firefox and Chrome browsers. Please
note the test call is prepended with `testing` macro that puts driver name into
the report. Once you've got an error, you'll easy find what driver failed the
tests exactly.

### Postmortem Handler To Collect Artifacts

To save a screenshot and HTML dump of your page in case of exception, wrap your
fixture into `with-postmortem` handler as follows:

```clojure
(defn fixture-drivers [f]
  (doseq [type driver-types]
    (with-driver type {} driver
      (with-postmortem driver {:dir "/path/to/folder"}
        (binding [*driver* driver]
          (testing (format "Testing in %s browser" (name type))
            (f)))))))
```

If you use Circle CI, it would be great to save data into artifacts directory:

```clojure
(def pm-dir
  (or (System/getenv "CIRCLE_ARTIFACTS")
      "/some/local/path"))
```

Now pass `pm-dir` into `with-postmortem` macro:

```clojure
...
  (with-postmortem driver {:dir pm-dir}
    (binding [*driver* driver]
      ...
```

Once an error occurs, you will find a PNG image that represents your browser
page at the moment of exception and HTML dump.

### Running Tests By Tag

Since UI tests may take lots of time to pass, it's definitely a good practice to
pass both server and UI tests independently from each other.

First, add `^:integration` tag to all the tests that are run inder the browser
like follows:

```clojure
(deftest ^:integration
  test-password-reset-pipeline
  (doto *driver*
    (go url-password-reset)
    (click :reset-btn)
    ...
```

Then, open your `project.clj` file and add test selectors:

```clojure
:test-selectors {:default (complement :integration)
                 :integration :integration}
```

Now, once you launch `lein test` you will run all the tests except browser
ones. To run integration tests, launch `lein test :integration`.

The main difference between a program and a human is that the first one
operates very fast. It means so fast, that sometimes a browser cannot render new
HTML in time. So after each action you need to put `wait-<something>` function
that just polls a browser checking for a predicate. O just `(wait <seconds>)` if
you don't care about optimization.

## Installing Drivers

[url-webdriver]: https://www.w3.org/TR/webdriver/
[url-tests]: https://github.com/igrishaev/etaoin/blob/master/test/etaoin/api_test.clj
[url-chromedriver]: https://sites.google.com/a/chromium.org/chromedriver/
[url-chromedriver-dl]: https://sites.google.com/a/chromium.org/chromedriver/downloads
[url-geckodriver-dl]: https://github.com/mozilla/geckodriver/releases
[url-phantom-dl]: http://phantomjs.org/download.html
[url-webkit]: https://webkit.org/blog/6900/webdriver-support-in-safari-10/

This page provides instructions on how to install drivers you need to automate
your browser.

Install Chrome and Firefox browsers downloading them from the official
sites. There won't be a problem on all the platforms.

Install specific drivers you need:

- Google [Chrome driver][url-chromedriver]:

  - `brew install chromedriver` for Mac users
  - or download compiled binaries from the [official site][url-chromedriver-dl].
  - ensure you have at least `2.28` version installed. `2.27` and below has a
    bug related to maximizing a window (see [[Troubleshooting]]).

- Geckodriver, a driver for Firefox:

  - `brew install geckodriver` for Mac users
  - or download it from the official [Mozilla site][url-geckodriver-dl].

- Phantom.js browser:

  - `brew install phantomjs` For Mac users
  - or download it from the [official site][url-phantom-dl].

- Safari Driver (for Mac only):

  - update your Mac OS to El Captain using App Store;
  - set up Safari options as the [Webkit page][url-webkit] says (scroll down to
    "Running the Example in Safari" section).

Now, check your installation launching any of these commands. For each command,
an endless process with a local HTTP server should start.

```bash
chromedriver
geckodriver
phantomjs --wd
safaridriver -p 0
```

You may run tests for this library launching:

```bash
lein test
```

You'll see browser windows open and close in series. The tests use a local HTML
file with a special layout to validate the most of the cases.

This page holds common troubles you might face during webdriver automation.

## Troubleshooting

### Calling maximize function throws an error

Example:

```clojure
etaoin.api> (def driver (chrome))
#'etaoin.api/driver
etaoin.api> (maximize driver)
ExceptionInfo throw+: {:response {
:sessionId "2672b934de785aabb730fd19330cf40c",
:status 13,
:value {:message "unknown error: cannot get automation extension\nfrom unknown error: page could not be found: chrome-extension://aapnijgdinlhnhlmodcfapnahmbfebeb/_generated_background_page.html\n
(Session info: chrome=57.0.2987.133)\n  (Driver info: chromedriver=2.27.440174
(e97a722caafc2d3a8b807ee115bfb307f7d2cfd9),platform=Mac OS X 10.11.6 x86_64)"}},
...
```

**Solution:** just update your `chromedriver` to the last version. Tested with
2.29, works fine. People say it woks as well since 2.28.

Remember, `brew` package manager has the outdated version 2.27. You will
probably have to download binaries from the [official site][chromedriver-dl].

See the [related issue][maximize-issue] in Selenium project.

[maximize-issue]:https://github.com/SeleniumHQ/selenium/issues/3508
[chromedriver-dl]:https://sites.google.com/a/chromium.org/chromedriver/downloads

### Clicking On Non-Visible Element

Example:

```clojure
etaoin.api> (click driver :some-id)
ExceptionInfo throw+: {:response {
:sessionId "d112ce8ddb49accdae78a769d5809eae",
:status 11,
:value {:message "element not visible\n  (Session info: chrome=57.0.2987.133)\n
(Driver info: chromedriver=2.29.461585
(0be2cd95f834e9ee7c46bcc7cf405b483f5ae83b),platform=Mac OS X 10.11.6 x86_64)"}},
...
```

**Solution:** you are trying to click an element that is not visible or its
dimentions are as little as it's impossible for a human to click on it. You
should pass another selector.

### Unpredictable errors in Chrome when window is not active

**Problem:** when you focus on other window, webdriver session that is run under
Google Chrome fails.

**Solution:** Google Chrome may suspend a tab when it has been inactive for some
time. When the page is suspended, no operation could be done on it. No clicks,
Js execution, etc. So try to keep Chrome window active during test session.

## Contributors

- [Ivan Grishaev](https://github.com/igrishaev)
- [Adam Frey](https://github.com/AdamFrey)

The project is open for your improvements and ideas. If any of unit tests fall
on your machine please submit an issue giving your OS version, browser and
console output.

## License

Copyright © 2017 Ivan Grishaev.

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
