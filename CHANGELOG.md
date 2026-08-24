# Changelog

All notable changes to this project will be documented in this file.

Releases are managed automatically by [release-please](https://github.com/googleapis/release-please).
Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/) — `feat:` bumps minor, `fix:` bumps patch, `feat!:` or `BREAKING CHANGE:` bumps major.

## 1.0.0 (2026-08-24)


### Features

* add German/English bilingual site (German default, English at /en/) ([b3c6875](https://github.com/marvinrichter/jvm-modernisation-playbook/commit/b3c6875d380a1d109db6aca8e15c46eebc2d8e0d))
* initial JVM Modernisation Playbook ([1f181f5](https://github.com/marvinrichter/jvm-modernisation-playbook/commit/1f181f50cbdc23f3c60676095a9cfb6f541b25e4))


### Bug Fixes

* rename stranglerFig to stranglerfig for case-sensitive Linux filesystem ([7b04c62](https://github.com/marvinrichter/jvm-modernisation-playbook/commit/7b04c620e0834daa98f94aa3037f6947791fd415))
* use mvn instead of ./mvnw (no wrapper in repo) ([13b1e38](https://github.com/marvinrichter/jvm-modernisation-playbook/commit/13b1e38c92c1f6fdf9d2fe799ec4698a9eb5b2bc))

## [1.0.0] - 2026-03-18

### Features

- Strangler Fig example: Spring Boot gateway routing legacy → new paths with feature flag
- Branch-by-Abstraction example: service extracted behind port interface with runtime toggle
- Anti-Corruption Layer example: domain translation layer between legacy and new model
- Bilingual MkDocs site (German default, English alternate) with GitHub Pages deployment
- CI: verify all three examples on push and PR via `./mvnw verify`
