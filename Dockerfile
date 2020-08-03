FROM clojure:lein-2.9.3

RUN apt-get -yqq update && \
    apt-get -yqq upgrade && \
    apt-get -yqq install gnupg2 && \
    apt-get -yqq install curl unzip && \
    apt-get -yqq install fonts-ipafont-gothic xfonts-100dpi xfonts-75dpi xfonts-scalable xfonts-cyrillic && \
    apt-get install -y jq libgconf-2-4 && \
    rm -rf /var/lib/apt/lists/*

# Install Chrome WebDriver
RUN CHROMEDRIVER_VERSION=`curl -sS chromedriver.storage.googleapis.com/LATEST_RELEASE` && \
    mkdir -p /opt/chromedriver-$CHROMEDRIVER_VERSION && \
    curl -sS -o /tmp/chromedriver_linux64.zip http://chromedriver.storage.googleapis.com/$CHROMEDRIVER_VERSION/chromedriver_linux64.zip && \
    unzip -qq /tmp/chromedriver_linux64.zip -d /opt/chromedriver-$CHROMEDRIVER_VERSION && \
    rm /tmp/chromedriver_linux64.zip && \
    chmod +x /opt/chromedriver-$CHROMEDRIVER_VERSION/chromedriver && \
    ln -fs /opt/chromedriver-$CHROMEDRIVER_VERSION/chromedriver /usr/local/bin/chromedriver

# Install Google Chrome
RUN curl -sS -o - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - && \
    echo "deb http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list && \
    apt-get -yqq update && \
    apt-get -yqq install google-chrome-stable && \
    rm -rf /var/lib/apt/lists/*

# Install Firefox
# RUN set -o nounset && set -o errexit && set -o xtrace
RUN FIREFOX_URI="https://download.mozilla.org/?product=firefox-latest&os=linux64&lang=en-US" && \
    wget --quiet --content-disposition "$FIREFOX_URI" && \
    tar xf firefox-*.tar.bz2 -C /usr/local && \
    ln -s /usr/local/firefox/firefox /usr/local/bin/firefox

# Install Geckodriver
ENV GECKODRIVER_VERSION="v0.27.0"
ENV GECKODRIVER_URL="https://github.com/mozilla/geckodriver/releases/download"
RUN curl -sL "$GECKODRIVER_URL/$GECKODRIVER_VERSION/geckodriver-$GECKODRIVER_VERSION-linux64.tar.gz" | \
    tar -xz -C /usr/local/bin

WORKDIR /etaoin
COPY ./ ./
RUN lein deps

WORKDIR /
RUN rm -rf /etaoin

ENV ETAOIN_TEST_DRIVERS="[:firefox :chrome]"
