
install-toc:
	npm install --save markdown-toc

create-toc:
	node_modules/.bin/markdown-toc -i README.md
