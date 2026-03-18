# Changelog

All notable changes to this project will be documented in this file.

Releases are managed automatically by [release-please](https://github.com/googleapis/release-please).
Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/) — `feat:` bumps minor, `fix:` bumps patch, `feat!:` or `BREAKING CHANGE:` bumps major.

## [1.0.0] - 2026-03-18

### Features

- Strangler Fig example: Spring Boot gateway routing legacy → new paths with feature flag
- Branch-by-Abstraction example: service extracted behind port interface with runtime toggle
- Anti-Corruption Layer example: domain translation layer between legacy and new model
- Bilingual MkDocs site (German default, English alternate) with GitHub Pages deployment
- CI: verify all three examples on push and PR via `./mvnw verify`
