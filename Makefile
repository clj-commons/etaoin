
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

.PHONY: fast-test
fast-test:
	lein test :only etaoin.fast-api-test #only chrome and single session

orig:
	find . -name '*.orig' -delete

.PHONY: tags
tags:
	ctags -e -R ./src

deploy:
	lein deploy clojars

.PHONY: release
release:
	lein release

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


.PHONY: autodoc
autodoc:
	lein codox
	cd autodoc && git checkout gh-pages
	cd autodoc && git add -A
	cd autodoc && git commit -m "Documentation updated"
	cd autodoc && git push

IMAGE := etaoin

.PHONY: docker-build
docker-build:
	docker build -t ${IMAGE}:latest .

BASE_DOCKER = docker run --rm \
	-v $(CURDIR)/:/etaoin \
	-w /etaoin ${IMAGE}:latest

.PHONY: docker-test
docker-test:
	${BASE_DOCKER} make test

.PHONY: docker-fast-test
docker-fast-test:
	${BASE_DOCKER} make fast-test
