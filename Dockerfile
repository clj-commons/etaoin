FROM clojure:temurin-17-tools-deps

RUN apt-get -yqq update && \
    apt-get -yqq upgrade && \
    apt-get -yqq install libnss3 && \
    apt-get -yqq install bzip2 && \
    apt-get -yqq install imagemagick && \
    apt-get -yqq install gnupg2 && \
    apt-get -yqq install git xvfb curl unzip && \
    apt-get -yqq install fonts-ipafont-gothic xfonts-100dpi xfonts-75dpi xfonts-scalable xfonts-cyrillic && \
    apt-get install -y ca-certificates jq libfontconfig libgconf-2-4 && \
    apt-get install -y fluxbox && \
    rm -rf /var/lib/apt/lists/*

# This dockerfile is not amenable to layer updates because nothing changes
# build with --no-cache

# Install babashka, we use it for (and after) docker image building
RUN curl -sLO https://raw.githubusercontent.com/babashka/babashka/master/install && \
    chmod +x install && \
    ./install && \
    rm ./install

# Copy over etaoin only for its build support, delete after use
COPY ./ /etaoin

RUN cd /etaoin && bb -docker-install && bb download-deps && rm -rf /etaoin
