#! /bin/bash

set -o nounset
set -o errexit
set -o xtrace

# Remove the firefox that comes with circleci docker image, too old.
sudo dpkg --purge firefox-mozilla-build
echo

# The firefox available with APT is still too old (version 52.9), so we need to
# install manually. We will install as /usr/local/bin/firefox
FIREFOX_URI="https://download.mozilla.org/?product=firefox-latest&os=linux64&lang=en-US"
wget --quiet --content-disposition "$FIREFOX_URI"
# Archive file name is something like firefox-61.0.1.tar.bz2
sudo tar xf firefox-*.tar.bz2 -C /usr/local
sudo ln -s /usr/local/firefox/firefox /usr/local/bin/firefox
which firefox
firefox --version
echo

# Adapted from the commented-out dockerfile
# https://github.com/CircleCI-Public/circleci-dockerfiles/blob/master/clojure/images/lein-2.8.1/browsers/Dockerfile

GECKODRIVER_META="https://api.github.com/repos/mozilla/geckodriver/releases/latest"
GECKODRIVER_LATEST_RELEASE_URL=$(curl $GECKODRIVER_META | jq -r ".assets[] | select(.name | test(\"linux64\")) | .browser_download_url")
curl --silent --show-error --location --fail --retry 3 --output /tmp/geckodriver_linux64.tar.gz "$GECKODRIVER_LATEST_RELEASE_URL"
cd /tmp
tar xf geckodriver_linux64.tar.gz
chmod +x geckodriver 
sudo mv geckodriver /usr/local/bin/
which geckodriver
geckodriver --version
