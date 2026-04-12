# Contributing

Thanks for contributing to `kewei-ai-agent`.

This repository is public-facing. Please optimize for clarity, reviewability, and reproducibility.

## Default Workflow

1. Open an issue first for non-trivial work.
2. Create a focused branch from `main`.
3. Make one topic-focused change per branch.
4. Run tests locally before pushing.
5. Open a pull request instead of pushing directly to `main`.
6. Merge only after review and green checks.

## Branch Naming

Use one of these prefixes:

- `feat/<topic>`
- `fix/<topic>`
- `docs/<topic>`
- `refactor/<topic>`
- `chore/<topic>`

Examples:

- `feat/long-term-memory-tools`
- `fix/readme-links`
- `docs/setup-guide`

## Do Not Commit

Please do not commit:

- secrets, API keys, passwords, tokens, certificates
- local-only config such as `application-local.yml`, `.env`, or personal test files
- machine-specific absolute paths
- temporary files, generated output, `tmp/`, `target/`, `.DS_Store`
- internal AI planning notes unless they are intentionally user-facing docs

## Pull Request Checklist

Before requesting review, make sure:

- tests passed locally
- docs were updated if behavior changed
- no secret values are in the diff
- no local absolute paths remain in docs
- unrelated changes are not bundled together

Typical commands:

```bash
./mvnw test
./mvnw -q -DskipTests compile
```

## Main Branch Policy

Treat `main` as release-quality:

- no direct pushes for normal feature work
- use branch + PR + review by default
- keep history understandable and scoped
