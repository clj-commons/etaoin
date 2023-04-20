FROM clojure:temurin-17-tools-deps-bullseye-slim

RUN apt-get -yqq update && \
    apt-get -yqq upgrade && \
    apt-get -yqq install sudo && \
    apt-get -yqq install libnss3 && \
    apt-get -yqq install bzip2 && \
    apt-get -yqq install imagemagick && \
    apt-get -yqq install gnupg2 && \
    apt-get -yqq install git xvfb curl unzip && \
    apt-get -yqq install fonts-ipafont-gothic xfonts-100dpi xfonts-75dpi xfonts-scalable xfonts-cyrillic && \
    apt-get install -y ca-certificates jq libfontconfig libgconf-2-4 && \
    apt-get install -y fluxbox && \
    rm -rf /var/lib/apt/lists/*

# This dockerfile is not amenable to layer updates because although commands herein
# don't change, the impact of them does. Basically we are saying: install latest of
# things. Build this image with --no-cache to ensure you get latest as intented.

# Install babashka, we use it for (and after) docker image building
RUN curl -sLO https://raw.githubusercontent.com/babashka/babashka/master/install && \
    chmod +x install && \
    ./install && \
    rm ./install

# Image will default non-root user: etaoin-user
RUN groupadd etaoin-user && \
    useradd --create-home --shell /bin/bash --gid etaoin-user etaoin-user && \
    usermod -a -G sudo etaoin-user && \
    echo 'ALL ALL = (ALL) NOPASSWD: ALL' >> /etc/sudoers && \
    echo 'etaoin-user:secret' | chpasswd
ENV HOME=/home/etaoin-user

# Copy over etaoin sources for docker image build support,
# we'll delete after use here but copy over fresh etaoin sources
# at image when run
COPY ./ /etaoin
RUN cd /etaoin && bb -docker-install

# download deps to avoid repeating this work during image run
RUN chown -R etaoin-user:etaoin-user /etaoin
USER etaoin-user
RUN cd /etaoin && bb download-deps

# Create a spot to copy over fresh sources
RUN mkdir /home/etaoin-user/etaoin

USER root
RUN rm -rf /etaoin

USER etaoin-user

WORKDIR /home/etaoin-user/etaoin

COPY script/docker_entry.clj /bin

ENTRYPOINT ["/usr/local/bin/bb", "/bin/docker_entry.clj"]
