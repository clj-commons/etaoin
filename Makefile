;; TODO: lread move to bb tasks

.PHONY: kill
kill:
	pkill chromedriver || true
	pkill geckodriver || true
	pkill safaridriver || true
	pkill phantomjs || true

IMAGE := etaoin

;; TODO: lread: have never tried, test, fix if necessary
.PHONY: docker-build
docker-build:
	docker build --no-cache -t ${IMAGE}:latest .

.PHONY: check-host
check-host:
	ifndef HOST
		$(error The HOST variable is not set, please do `export HOST=$$HOST` first)
	endif

;; TODO: lread: have never tried, test, fix if necessary
# works only on mac + quartz
.PHONY: docker-test-display
docker-test-display: check-host
	xhost +
	docker run --rm \
	-v ${CURDIR}:/etaoin \
	-v /tmp/.X11-unix:/tmp/.X11-unix -e DISPLAY=$(HOST):0 \
	-w /etaoin ${IMAGE}:latest \
	bb test all || \
	xhost -

;; TODO: lread: have never tried, test, fix if necessary
.PHONY: docker-test
docker-test:
	docker run --rm \
	-v ${CURDIR}:/etaoin \
	-w /etaoin ${IMAGE}:latest \
	bb test all
