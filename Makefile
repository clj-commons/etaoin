
repl:
	lein repl

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

.PHONY: check-host
check-host:
	ifndef HOST
		$(error The HOST variable is not set, please do `export HOST=$$HOST` first)
	endif

# works only on mac + quartz
.PHONY: docker-test-display
docker-test-display: check-host
	xhost +
	docker run --rm \
	-v ${CURDIR}:/etaoin \
	-v /tmp/.X11-unix:/tmp/.X11-unix -e DISPLAY=$(HOST):0 \
	-w /etaoin ${IMAGE}:latest \
	lein test || \
	xhost -

.PHONY: docker-test
docker-test:
	docker run --rm \
	-v ${CURDIR}:/etaoin \
	-w /etaoin ${IMAGE}:latest \
	lein test
