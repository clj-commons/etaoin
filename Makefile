IMAGE := etaoin

.PHONY: docker-build
docker-build:
	docker build --no-cache -t ${IMAGE}:latest .

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
	bb test all || \
	xhost -

.PHONY: docker-test
docker-test:
	docker run --rm \
	-v ${CURDIR}:/etaoin \
	-w /etaoin ${IMAGE}:latest \
	bb test all
