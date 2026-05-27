# Support Bundle

`/rep support bundle` creates a diagnostic ZIP for support, review, and incident investigation.

## Output

- Directory: `plugins/ReputationBan/support/`
- File name: `reputationban-support-YYYYMMDD-HHMMSS.zip`
- Permission: `reputationban.admin.diagnostics`

## Included Files

- `meta.txt`: generation time, plugin version, server version, Java version, and plugin data folder.
- `doctor.txt`: `/rep doctor`-style health details without webhook URLs.
- `counts.txt`: table row counts only.
- `config-redacted.yml`: `config.yml` with sensitive keys redacted.
- `README-SHARING.txt`: sharing safety notes.
- `plugin.yml` and `changelog-excerpt.txt` when available.

## Excluded Files

The bundle must not include:

- `reputationban.db`, `reputationban.db-wal`, or `reputationban.db-shm`
- server logs such as `latest.log` or `debug.log`
- live `config.yml`
- Discord Webhook URLs, passwords, tokens, sessions, cookies, or other secrets

## Before Sharing

Open `config-redacted.yml` and confirm that webhook URLs and tokens are not present. The bundle is designed to be safe by default, but operators should still inspect it before posting it to tickets or review threads.
