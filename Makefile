
.PHONY: autodoc
autodoc:
	lein autodoc
	cd autodoc
	git add .
	git commit -m "Documentation updated"
	git push
	cd ..

deploy:
	lein deploy clojars
