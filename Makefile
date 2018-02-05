
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

deploy:
	lein deploy clojars

toc-install:
	npm install --save markdown-toc

toc-build:
	node_modules/.bin/markdown-toc -i README.md
