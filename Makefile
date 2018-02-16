
repl:
	lein repl

repl-1.7:
	lein with-profile +1.7 repl

repl-1.8:
	lein with-profile +1.8 repl

repl-1.9:
	lein with-profile +1.9 repl

.PHONY: test
test:
	lein test

orig:
	find . -name '*.orig' -delete

.PHONY: tags
tags:
	ctags -e -R ./src

deploy:
	lein deploy clojars

toc-install:
	npm install --save markdown-toc

toc-build:
	node_modules/.bin/markdown-toc -i README.md

.PHONY: kill
kill:
	pkill chromedriver || true
	pkill geckodriver || true
	pkill safaridriver || true
	pkill phantomjs || true
