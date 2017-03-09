#!/usr/bin/env bash

lein autodoc
cd autodoc
git add .
git commit -m "Documentation updated"
git push
cd ..
