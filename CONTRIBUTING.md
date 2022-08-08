# Contributing Guidelines

**Do**
- remember that a gift, while appreciated, is also a burden. We value your input but start with an issue to propose your change before investing your valuable time in a PR.
- read the [rewrite-clj Developer Guide](doc/02-developer-guide.adoc).
- follow [the seven rules of a great Git commit message][1].
- follow [the Clojure Style Guide][2].
- include/update tests for your change.
- ensure that the Continuous Integration checks pass.
- feel free to pester the project maintainers about your PR if it hasn't been responded to. Sometimes notifications can be missed.

**Don't**
- include more than one feature or fix in a single PR.
- include changes unrelated to the purpose of the PR. This includes changing the project version number, adding lines to the
`.gitignore` file, or changing the indentation or formatting.
- open a new PR if changes are requested. Just push to the same branch and the PR will be updated.
- overuse vertical whitespace; avoid multiple sequential blank lines.

[1]: https://chris.beams.io/posts/git-commit/#seven-rules
[2]: https://guide.clojure.style
