#!/usr/bin/env bash
set -euo pipefail

fail() { echo "[FAIL] $*" >&2; exit 1; }
pass() { echo "[PASS] $*"; }

require_file() {
  [[ -f "$1" ]] || fail "Missing required file: $1"
}

require_japanese_text() {
  local file="$1"
  require_file "$file"
  grep -Pq '[\p{Hiragana}\p{Katakana}\p{Han}]' "$file" || fail "$file does not appear to contain Japanese text"
  pass "$file contains Japanese text"
}

require_literal() {
  local file="$1"
  local literal="$2"
  require_file "$file"
  grep -Fq "$literal" "$file" || fail "$file is missing required text: $literal"
  pass "$file keeps $literal"
}

require_any_literal() {
  local file="$1"
  shift
  require_file "$file"
  local literal
  for literal in "$@"; do
    if grep -Fq "$literal" "$file"; then
      pass "$file keeps one of: $*"
      return 0
    fi
  done
  fail "$file is missing all required alternatives: $*"
}

require_file README.md
if ! compgen -G "docs/*.md" >/dev/null; then
  fail "No docs/*.md files found"
fi
pass "README.md and docs/*.md are present"

require_japanese_text README.md
require_japanese_text docs/INSTALLATION.md
require_japanese_text docs/CONFIGURATION.md
require_japanese_text docs/RELEASE_READINESS.md
require_japanese_text docs/SUPPORT_BUNDLE.md
require_japanese_text docs/SECURITY_REDACTION.md
require_japanese_text docs/PAPER_RUNTIME_SMOKE_REPORT_TEMPLATE.md
require_japanese_text docs/phase-25.md
require_japanese_text docs/phase-26.md
require_japanese_text docs/phase-28.md
require_japanese_text docs/V1_RELEASE_PLAN.md
require_japanese_text docs/PLAYER_REPORT_RUNTIME_SMOKE_CHECKLIST.md

require_literal README.md "/rep version"
require_literal README.md "/reportbad"
require_literal README.md "reputationban.admin"
require_literal README.md "0.28.0"
require_literal docs/V1_RELEASE_PLAN.md "v1.0.0"
require_literal docs/CONFIGURATION.md "initial-score"
require_any_literal docs/CONFIGURATION.md "notify.discord-webhook" "discord-webhook"
require_literal docs/CONFIGURATION.md "min-unique-reports-before-deduction"
require_literal docs/CONFIGURATION.md "retention"
require_any_literal docs/SECURITY_REDACTION.md "<redacted>" "<webhook-url>"

if grep -RE "https://(canary\\.|ptb\\.)?discord(app)?\\.com/api/webhooks/[0-9]+/[A-Za-z0-9_-]{20,}" docs >/dev/null; then
  fail "Concrete Discord webhook URL-like value found in docs"
fi
pass "No concrete Discord webhook URL-like values found in docs"

pass "Documentation localization checks completed"
