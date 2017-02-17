
[url-webdriver]: https://www.w3.org/TR/webdriver/
[url-wiki]: https://en.wikipedia.org/wiki/Etaoin_shrdlu#Literature
[url-tests]: https://github.com/igrishaev/etaoin/blob/master/test/etaoin/api_test.clj
[url-chromedriver]: https://sites.google.com/a/chromium.org/chromedriver/
[url-chromedriver-dl]: https://sites.google.com/a/chromium.org/chromedriver/downloads
[url-geckodriver-dl]: https://github.com/mozilla/geckodriver/releases
[url-phantom-dl]: http://phantomjs.org/download.html

Pure Clojure implementation of [Webdriver][url-webdriver] protocol.

Use that library to automate a browser, test your frontend behaviour, simulate
human actions or whatever you want.

It's named after [Etaoin Shrdlu][url-wiki] a typing machine that became alive
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

## Installation

Add the following into `:dependencies` vector in your `project.clj` file:

```
[etaoin "0.1.0"]
```
## Usage

This section is in progress, see [unit tests][url-tests] for details.

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

- Now, check your installation launching any of these commands. For each
  command, an endless process with a local HTTP server should start.

  ```bash
  chromedriver
  geckodriver
  phantomjs --wd
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

## Roadmoap

- Add Safari checks into unit tests.
- Add MS Edge support.
- Add touch API tests.
- Add mobile support.
- Make unit tests more detailed.

## License

Copyright © 2017 Ivan Grishaev.

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
