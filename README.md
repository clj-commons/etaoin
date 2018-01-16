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
  * [Using headless drivers](#using-headless-drivers)
  * [Postmortem: auto-save artifacts in case of exception](#postmortem-auto-save-artifacts-in-case-of-exception)
  * [Reading browser's logs](#reading-browsers-logs)
  * [Additional parameters](#additional-parameters)
  * [Scrolling](#scrolling)
  * [Working with frames and iframes](#working-with-frames-and-iframes)
  * [Wait functions](#wait-functions)
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
- [Barrick Gold](http://www.barrick.com/)
- [Doctor Evidence](http://drevidence.com/)

You are welcome to submit your company into that list.

## Documentation

- [API docs][url-doc]
- [Slack channel][url-slack]
- [Unit tests][url-tests]

## Installation

Add the following into `:dependencies` vector in your `project.clj` file:

```
[etaoin "0.2.1"]
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

Clicking on a file input button opens an OS-specific dialog that you are not
allowed to interact with using WebDriver protocol. Use the `upload-file`
function to attach a local file to a file input widget. The function takes a
selector that points to a file input and either a full path as a string or a
native `java.io.File` instance. The file should exist or you'll get an exception
otherwise. Usage example:

```clojure
(def driver (chrome))

;; open a web page that serves uploaded files
(go driver "http://nervgh.github.io/pages/angular-file-upload/examples/simple/")

;; bound selector to variable; you may also specify an id, class, etc
(def input {:tag :input :type :file})

;; upload an image with the first one file input
(def my-file "/Users/ivan/Downloads/sample.png")
(upload-file driver input my-file)

;; or pass a native Java object:
(require '[clojure.java.io :as io])
(def my-file (io/file "/Users/ivan/Downloads/sample.png"))
(upload-file driver input my-file)
```

### Using headless drivers

Recently, Google Chrome and later Firefox started support a feature named
headless mode. When being headless, none of UI windows occur on the screen, only
the stdout output goes into console. This feature allows you to run integration
tests on servers that do not have graphical output device.

Ensure your browser supports headless mode by checking if it accepts `--headles`
command line argument when running it from the terminal. Phantom.js driver is
headless by its nature (it has never been developed for rendering UI).

When starting a driver, pass `:headless` boolean flag to switch into headless
mode. Note, only latest version of Chrome and Firefox are supported. For other
drivers, the flag will be ignored.

```clojure
(def driver (chrome {:headless true})) ;; runs headless Chrome
```

or

```clojure
(def driver (firefox {:headless true})) ;; runs headless Firefox
```

To check of any driver has been run in headless mode, use `headless?` predicate:

```clojure
(headless? driver) ;; true
```

Note, it will always return true for Phantom.js instances.

There are several shortcuts to run Chrome or Firefox in headless mode by
default:

```clojure
(def driver (chrome-headless))

;; or

(def driver (firefox-headless {...})) ;; with extra settings

;; or

(with-chrome-headless nil driver
  (go driver "http://example.com"))

(with-firefox-headless {...} driver ;; extra settings
  (go driver "http://example.com"))
```

There are also `when-headless` and `when-not-headless` macroses that allow to
perform a bunch of commands only if a browser is in headless mode or not
respectively:

```clojure
(with-chrome nil driver
  ...
  (when-not-headless driver
    ... some actions that might be not available in headless mode)
  ... common actions for both versions)
```

### Postmortem: auto-save artifacts in case of exception

Sometimes, it might be difficult to discover what went wrong during the last UI
tests session. A special macro `with-postmortem` saves some useful data on disk
before the exception was triggered. Those data are a screenshot, HTML code and
JS console logs. Note: not all browsers support getting JS logs.

Example:

```clojure
(def driver (chrome))
(with-postmortem driver {:dir "/Users/ivan/artifacts"}
  (click driver :non-existing-element))
```

An exception will rise, but in `/Users/ivan/artifacts` there will be three files
named by a template `<browser>-<host>-<port>-<datetime>.<ext>`:

- `firefox-127.0.0.1-4444-2017-03-26-02-45-07.png`: an actual screenshot of the
  browser's page;
- `firefox-127.0.0.1-4444-2017-03-26-02-45-07.html`: the current browser's HTML
  content;
- `firefox-127.0.0.1-4444-2017-03-26-02-45-07.json`: a JSON file with console
  logs; those are a vector of objects.

The handler takes a map of options with the following keys. All of them might be
absent.

```clojure
{;; default directory where to store artifacts; pwd is used when not passed
 :dir "/home/ivan/UI-tests"

 ;; a directory where to store screenshots; :dir is used when not passed
 :dir-img "/home/ivan/UI-tests/screenshots"

 ;; the same but for HTML sources
 :dir-src "/home/ivan/UI-tests/HTML"

 ;; the same but for console logs
 :dir-log "/home/ivan/UI-tests/console"

 ;; a string template to format a date; See SimpleDateFormat Java class
 :date-format "yyyy-MM-dd-hh-mm-ss"}
```

### Reading browser's logs

Function `(get-logs driver)` returns the browser's logs as a vector of
maps. Each map has the following structure:

```clojure
{:level :warning,
 :message "1,2,3,4  anonymous (:1)",
 :timestamp 1511449388366,
 :source nil,
 :datetime #inst "2017-11-23T15:03:08.366-00:00"}
```

Currently, logs are available in Chrome and Phantom.js only. Please note, the
message text and the source type highly depend on the browser. Chrome wipes the
logs once they have been read. Phantom.js keeps them but only until you change
the page.

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
 :path-driver "/Users/ivan/Downloads/geckodriver"

 ;; Path to the driver's binary file. When not passed, the driver discovers it
 ;; by its own.
 :path-browser "/Users/ivan/Downloads/firefox/firefox"

 ;; Extra command line arguments sent to the browser's process. See your browser's
 ;; supported flags.
 :args ["--incognito" "--app" "http://example.com"]

 ;; Extra command line arguments sent to the webdriver's process.
 :args-driver ["-b" "/path/to/firefox/binary"]

 ;; Sets browser's minimal logging level. Only messages with level above
 ;; that one will be collected. Useful for fetching Javascript logs. Possible
 ;; values are: nil (aliases :off, :none), :debug, :info, :warn (alias :warning),
 ;; :err (aliases :error, :severe, :crit, :critical), :all. When not passed,
 ;; :all is set.
 :log-level :err ;; to show only errors but not debug

 ;; Env variables sent to the driver's process. Not processed yet.
 :env {:MOZ_CRASHREPORTER_URL "http://test.com"}

 ;; Initial window size.
 :size [1024 680]

 ;; Default URL to open. Works only in FF for now.
 :url "http://example.com"

 ;; Driver-specific options. Make sure you have read the docs before setting them.
 :capabilities {:chromeOptions {:args ["--headless"]}}}
```

### Scrolling

The library ships a set of functions to scroll the page.

The most important one, `scroll-query` jumps the the first element found with
the query term:

```clojure
(def driver (chrome))

;; the form button placed somewhere below
(scroll-query driver :button-submit)

;; the main article
(scroll-query driver {:tag :h1})
```

To jump to the absolute position, just use `scroll` as follows:

```clojure
(scroll driver 100 600)

;; or pass a map with x and y keys
(scroll driver {:x 100 :y 600})
```

To scroll relatively, use `scroll-by` with offset values:

```clojure
;; keeps the same horizontal position, goes up for 100 pixels
(scroll-by driver 0 -100) ;; map parameter is also supported
```

There are two shortcuts to jump top or bottom of the page:

```clojure
(scroll-bottom driver) ;; you'll see the footer...
(scroll-top driver)    ;; ...and the header again
```

The following functions scroll the page in all directions:

```clojure
(scroll-down driver 200)  ;; scrolls down by 200 pixels
(scroll-down driver)      ;; scrolls down by the default (100) number of pixels

(scroll-up driver 200)    ;; the same, but scrolls up...
(scroll-up driver)

(scroll-left driver 200)  ;; ...left
(scroll-left driver)

(scroll-right driver 200) ;; ... and right
(scroll-right driver)
```

One note, in all cases the scroll actions are served with Javascript. Ensure
your browser has it enabled.

### Working with frames and iframes

While working with the page, you cannot interact with those items that are put
into a frame or an iframe. The functions below switch the current context on
specific frame:

```clojure
(switch-frame driver :frameId) ;; now you are inside an iframe with id="frameId"
(click driver :someButton)     ;; click on a button inside that iframe
(switch-frame-top driver)      ;; switches on the top of the page again
```

Frames could be nested one into another. The functions take that into
account. Say you have an HTML layout like this:

```html
<iframe src="...">
  <iframe src="...">
    <button id="the-goal">
  </frame>
</frame>
```

So you can reach the button with the following code:

```clojure
(switch-frame-first driver)  ;; switches to the first top-level iframe
(switch-frame-first driver)  ;; the same for an iframe inside the previous one
(click driver :the-goal)
(switch-frame-parent driver) ;; you are in the first iframe now
(switch-frame-parent driver) ;; you are at the top
```

To reduce number of code lines, there is a special `with-frame` macro. It
temporary switches frames while executing the body returning its last expression
and switching to the previous frame afterwards.

```clojure
(with-frame driver {:id :first-frame}
  (with-frame driver {:id :nested-frame}
    (click driver {:id :nested-button})
    42))
```

The code above returns `42` staying at the same frame that has been before
before evaluating the macros.

### Wait functions

The main difference between a program and a human being is that the first one
operates very fast. It means so fast, that sometimes a browser cannot render new
HTML in time. So after each action you'd better to put `wait-<something>`
function that just polls a browser until the predicate evaluates into true. Or
just `(wait <seconds>)` if you don't care about optimization.

The `with-wait` macro might be helpful when you need to prepend each action with
`(wait n)`. For example, the following form

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

There is another macro `(doto-wait n driver & body)` that acts like the standard
`doto` but prepend each form with `(wait n)`. For example:

```clojure
(with-chrome {} driver
  (doto-wait 1 driver
    (go "http://site.com")
    (click :this-link)
    (click :that-button)
    ...etc))
```

The final form would be something like this:

```clojure
(with-chrome {} driver
  (doto driver
    (wait 1)
    (go "http://site.com")
    (wait 1)
    (click :this-link)
    (wait 1)
    (click :that-button)
    ...etc))
```

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

To save some artifacts in case of exception, wrap the body of your test into
`with-postmortem` handler as follows:

```clojure
(deftest test-user-login
  (with-postmortem *driver* {:dir "/path/to/folder"}
    (doto *driver*
      (go "http://127.0.0.1:8080")
      (click-visible :login)
      ;; any other actions...
      )))
```

Now that, if any exception occurs in that test, artifacts will be saved.

To not copy and paste the options map, declare it on the top of the module. If
you use Circle CI, it would be great to save the data into a special artifacts
directory that might be downloaded as a zip file once the build has been
finished:

```clojure
(def pm-dir
  (or (System/getenv "CIRCLE_ARTIFACTS") ;; you are on CI
      "/some/local/path"))               ;; local machine

(def pm-opt
  {:dir pm-dir})
```

Now pass that map everywhere into PM handler:

```clojure
  ;; test declaration
  (with-postmortem *driver* pm-dir
    ;; test body goes here
    )
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
- [JW Koelewijn](https://github.com/jwkoelewijn)

The project is open for your improvements and ideas. If any of unit tests fall
on your machine please submit an issue giving your OS version, browser and
console output.

## License

Copyright © 2017 Ivan Grishaev.

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
