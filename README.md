
[url-webdriver]: https://www.w3.org/TR/webdriver/
[url-wiki]: https://en.wikipedia.org/wiki/Etaoin_shrdlu#Literature
[url-tests]: https://github.com/igrishaev/etaoin/blob/master/test/etaoin/api_test.clj
[url-doc]: http://grishaev.me/etaoin/
[url-pages]:https://github.com/igrishaev/etaoin/wiki

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

- [Wiki][url-pages]
- [API docs][url-doc]
- [Unit tests][url-tests]

## Contributing

The project is open for your improvements and ideas. If any of unit tests fall
on your machine please submit an issue giving your OS version, browser and
console output.

## License

Copyright © 2017 Ivan Grishaev.

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
