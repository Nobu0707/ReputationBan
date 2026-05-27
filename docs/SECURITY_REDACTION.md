# Security And Redaction

ReputationBan treats Discord webhook URLs as secrets. They must not appear in logs, command output, audit metadata, CSV exports, support bundles, release artifacts, or review archives.

## Support Bundles

`/rep support bundle` is designed for sharing with reviewers or support channels. It includes diagnostics and redacted config only. It does not include SQLite database files, WAL/SHM files, live server logs, or live `config.yml`.

`meta.txt` and `doctor.txt` avoid shared-unnecessary absolute paths by using `<plugin-data-folder>` for the plugin data directory.

## config-redacted.yml

`config-redacted.yml` keeps normal operational settings visible while redacting sensitive key names. Keys containing `url`, `webhook`, `password`, `token`, `secret`, `session`, or `cookie` are replaced with `<redacted>`.

## Free Text

`Redactor.redactSecretLikeValue` also redacts URL-like values and simple free-text secret patterns such as `token abc123`, `password is hunter2`, `sessionId abc`, `webhook VALUE`, and `url VALUE`.

## Review Archives

`scripts/make-review-archive.sh` writes `checks/secret-scan.txt` and redacts concrete Discord webhook URL patterns from that output. Review archives also collect release verification output and the latest Paper runtime smoke summary when one exists.

## Before Sharing

Before sharing a support bundle, release artifact, or review archive:

- Check that no Discord webhook URL appears.
- Check that no database or log files are included.
- Check that `config-redacted.yml` contains `<redacted>` for secret-like settings.
- Check that local absolute paths are minimized or replaced with placeholders.
