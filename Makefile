
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

UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Linux)
	DISPLAY := $(DISPLAY)
endif
ifeq ($(UNAME_S),Darwin)
	DISPLAY := $(HOST):0
endif

.PHONY: docker-test-display
docker-test-display:
# before running test on Mac with x11 run this command: `export HOST=$HOST`
	@if [ ${HOST} =  -a $(UNAME_S) = Darwin ]; then\
		echo "Do export HOST=\$$HOST"; exit 1;\
	fi
	xhost +
	docker run --rm \
	-v ${CURDIR}:/etaoin \
	-v /tmp/.X11-unix:/tmp/.X11-unix -e DISPLAY=${DISPLAY} \
	-w /etaoin ${IMAGE}:latest \
	lein test || \
	xhost -

.PHONY: docker-test
docker-test:
	docker run --rm \
	-v ${CURDIR}:/etaoin \
	-w /etaoin ${IMAGE}:latest \
	lein test
