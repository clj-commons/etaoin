
Pure Clojure implementation of Webdriver protocol.

Use that library to automate browser actions

## Benefits
- Selenium-free: no long dependencies in your project, no tonns of jars, etc.
- Lightweight, fast. Simple, easy to understand.
- Compact: just one main module with a couple of helpers.
- Extremely declarative: the code is just a list of actions.

## Capabilities
- Currenlty supports Chrome, Firefox and Phantom.js.
- May either connect to a remote driver or run it on your local machine. You
  have to install the drivers by your own first with `brew`, `apt-get`, etc.
- Run your tests directly from Emacs pressing `C-t t` in Cider mode as usual.
- Can imitate human-like behaviour (delays, typos, etc).

## Usage
- todo: api description here.

## Run tests
- Install Chrome and Firefox browsers as usual, e.g. downloading installation
  backages from the official sites.
- Install Chrome driver:
  `brew install chromedriver`
- Install Geckodriver, a driver for Firefox:
  `brew install geckodriver`
- Install Phantom.js browser:
  `brew install phantomjs`
- Check your installation with commands by launching the commands:
  ```bash
  chromedriver
  geckodriver
  phantomjs --wd
  ```
- If they work, you are ready to lunch the tests:
  `lein test`
- You'll see browser windows open and close in series. The tests use a local
  HTML file with a special layout to validate the most of the cases.

## Todo
- Add more browsers: Safari, MS Edge, iOS.
- Implement missing methods.
- Add more detailed unit tests.

## License

Copyright © 2017 Ivan Grishaev.

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
