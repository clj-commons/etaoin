FROM clojure:lein-2.9.3

RUN apt-get -yqq update && \
    apt-get -yqq upgrade && \
    apt-get -yqq install gnupg2 && \
    apt-get -yqq install sudo xvfb curl unzip && \
    apt-get -yqq install fonts-ipafont-gothic xfonts-100dpi xfonts-75dpi xfonts-scalable xfonts-cyrillic && \
    apt-get install -y ca-certificates jq libfontconfig libgconf-2-4 && \
    rm -rf /var/lib/apt/lists/*
# add google-chrome to source list
RUN curl -sS -o - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - && \
    echo "deb http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list

# Make sure PATH includes ~/.local/bin
# https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=839155
# This only works for root. The automation user is done near the end of this Dockerfile
RUN echo 'PATH="$HOME/.local/bin:$PATH"' >> /etc/profile.d/user-local-path.sh


RUN groupadd --gid 1000 automation \
  && useradd --uid 1000 --gid automation --shell /bin/bash --create-home automation \
  && echo 'automation ALL=NOPASSWD: ALL' >> /etc/sudoers.d/50-automation

USER automation

#install java 11
RUN sudo apt-get update && sudo apt-get install -y openjdk-11-jre

# Install Chrome WebDriver
RUN CHROMEDRIVER_VERSION=`curl -sS chromedriver.storage.googleapis.com/LATEST_RELEASE` && \
    sudo mkdir -p /opt/chromedriver-$CHROMEDRIVER_VERSION && \
    sudo curl -sS -o /tmp/chromedriver_linux64.zip http://chromedriver.storage.googleapis.com/$CHROMEDRIVER_VERSION/chromedriver_linux64.zip && \
    sudo unzip -qq /tmp/chromedriver_linux64.zip -d /opt/chromedriver-$CHROMEDRIVER_VERSION && \
    sudo rm /tmp/chromedriver_linux64.zip && \
    sudo chmod +x /opt/chromedriver-$CHROMEDRIVER_VERSION/chromedriver && \
    sudo ln -fs /opt/chromedriver-$CHROMEDRIVER_VERSION/chromedriver /usr/local/bin/chromedriver

# Install Google Chrome
RUN sudo apt-get -yqq update && \
    sudo apt-get -yqq install google-chrome-stable && \
    sudo rm -rf /var/lib/apt/lists/*

# Install Firefox
# RUN set -o nounset && set -o errexit && set -o xtrace
RUN FIREFOX_URI="https://download.mozilla.org/?product=firefox-latest&os=linux64&lang=en-US" && \
    sudo wget --quiet --content-disposition "$FIREFOX_URI" && \
    sudo tar xf firefox-*.tar.bz2 -C /usr/local && \
    sudo ln -s /usr/local/firefox/firefox /usr/local/bin/firefox

# Install Geckodriver
RUN GECKODRIVER_META="https://api.github.com/repos/mozilla/geckodriver/releases/latest" && \
    GECKODRIVER_LATEST_RELEASE_URL=$(curl $GECKODRIVER_META | jq -r ".assets[] | select(.content_type==\"application/gzip\") | select(.name | test(\"linux64\")) | .browser_download_url") && \
    sudo curl --silent --show-error --location --fail --retry 3 --output /tmp/geckodriver_linux64.tar.gz "$GECKODRIVER_LATEST_RELEASE_URL"  && \
    cd /tmp  && \
    sudo tar xf geckodriver_linux64.tar.gz  && \
    sudo chmod +x geckodriver && \
    sudo mv geckodriver /usr/local/bin/


## install phantomjs
#
ENV OPENSSL_CONF /
RUN PHANTOMJS_VERSION=phantomjs-2.1.1-linux-x86_64 && \
  wget --quiet --content-disposition https://bitbucket.org/ariya/phantomjs/downloads/$PHANTOMJS_VERSION.tar.bz2 && \
  sudo tar xf $PHANTOMJS_VERSION.tar.bz2 -C /usr/local && \
  sudo ln -s /usr/local/$PHANTOMJS_VERSION/bin/phantomjs /usr/local/bin/phantomjs && \
  rm  $PHANTOMJS_VERSION.tar.bz2

ENV DISPLAY :99
RUN printf '#!/bin/sh\nsudo Xvfb :99 -screen 0 1280x1024x24 &\nexec "$@"\n' > /tmp/entrypoint \
  && chmod +x /tmp/entrypoint \
    && sudo mv /tmp/entrypoint /entrypoint.sh

ENV PATH /home/automation/.local/bin:/home/automation/bin:${PATH}

ADD ./ /etaoin
RUN cd /etaoin && lein deps && sudo rm -rf /etaoin

RUN mkdir /home/automation/etaoin
VOLUME ["/home/automation/etaoin"]

ENV ETAOIN_TEST_DRIVERS="[:firefox :chrome :phantom]"

ENTRYPOINT ["/entrypoint.sh"]
