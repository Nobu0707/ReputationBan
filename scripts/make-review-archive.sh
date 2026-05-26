#!/usr/bin/env bash
set -euo pipefail

# ReputationBan review archive generator.
# Usage:
#   bash scripts/make-review-archive.sh
#   bash scripts/make-review-archive.sh "Phase 2"
#
# The optional argument is an expected substring of HEAD's commit subject.
# If it does not match, the script exits before producing an archive, which
# prevents uploading a stale review tarball.

EXPECTED_SUBJECT="${1:-}"

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

HEAD_SHA="$(git rev-parse --short=12 HEAD)"
HEAD_SUBJECT="$(git show -s --format=%s HEAD)"
BRANCH="$(git rev-parse --abbrev-ref HEAD)"
STAMP="$(date +%Y%m%d-%H%M%S)"

if [[ -n "$EXPECTED_SUBJECT" && "$HEAD_SUBJECT" != *"$EXPECTED_SUBJECT"* ]]; then
  echo "ERROR: HEAD subject does not match expected phase/commit." >&2
  echo "  expected substring: $EXPECTED_SUBJECT" >&2
  echo "  actual HEAD:        $HEAD_SHA $HEAD_SUBJECT" >&2
  exit 2
fi

OUTDIR="$ROOT/build/reputationban-review-$HEAD_SHA-$STAMP"
ARCHIVE="$ROOT/reputationban-review-$HEAD_SHA-$STAMP.tar.gz"
LATEST="$ROOT/reputationban-review-latest.tar.gz"

rm -rf "$OUTDIR"
mkdir -p "$OUTDIR"/{meta,diff,file-diffs,files,checks}

{
  echo "generatedAt=$STAMP"
  echo "repo=$ROOT"
  echo "branch=$BRANCH"
  echo "head=$HEAD_SHA"
  echo "subject=$HEAD_SUBJECT"
  echo "expectedSubject=$EXPECTED_SUBJECT"
} > "$OUTDIR/meta/review-info.txt"

git log --oneline -10 > "$OUTDIR/meta/git-log-oneline.txt"
git status --short > "$OUTDIR/meta/git-status-short.txt"
git show --stat --oneline --no-color HEAD > "$OUTDIR/meta/head-stat.txt"
git show --name-status --no-color HEAD > "$OUTDIR/meta/head-name-status.txt"
git diff-tree --root --no-commit-id --name-only -r HEAD > "$OUTDIR/meta/changed-files.txt"
git remote -v > "$OUTDIR/meta/git-remotes.txt"
git tag --list --sort=creatordate > "$OUTDIR/meta/tags.txt"
git show --no-color HEAD > "$OUTDIR/diff/head-full-diff.txt"

# Per-file diffs and HEAD file contents for every changed file.
while IFS= read -r file; do
  [[ -z "$file" ]] && continue

  safe="${file//\//__}"
  safe="${safe// /_}"

  git show --no-color HEAD -- "$file" > "$OUTDIR/file-diffs/${safe}.diff.txt" || true

  # Store the committed file content when it exists at HEAD.
  # Deleted files will fail here and are skipped.
  if git cat-file -e "HEAD:$file" 2>/dev/null; then
    mkdir -p "$OUTDIR/files/$(dirname "$file")"
    git show "HEAD:$file" > "$OUTDIR/files/$file" || true
  fi
done < "$OUTDIR/meta/changed-files.txt"

# Lightweight command outputs useful for review. These should never print secrets.
{
  echo "## rg ban and report paths"
  rg -n "punishIfNeeded|OfflinePlayer|\\.ban\\(|isBanned|ban_count|score_history|setAutoCommit|commit\\(|rollback\\(" src/main/java/dev/modplugin/reputationban || true
  echo
  echo "## rg review workflow"
  rg -n "ReputationBan|diff-tree --root|Zone.Identifier|gitattributes|review_code" .gitattributes .gitignore scripts || true
} > "$OUTDIR/checks/rg-review-signals.txt"

# These checks do not mutate the repo. Full builds are still run separately by Codex.
git diff --check > "$OUTDIR/checks/git-diff-check.txt" 2>&1 || true

if [[ -f "$ROOT/build/review_code_output.txt" ]]; then
  cp "$ROOT/build/review_code_output.txt" "$OUTDIR/checks/review-code-output.txt"
fi

if compgen -G "$ROOT/build/test-results/test/*.xml" >/dev/null; then
  {
    echo "JUnit XML files:"
    find "$ROOT/build/test-results/test" -type f -name '*.xml' -print | sort
    echo
    echo "Suite summary:"
    grep -h "<testsuite " "$ROOT"/build/test-results/test/*.xml \
      | sed -E 's/.*name="([^"]+)".*tests="([^"]+)".*skipped="([^"]+)".*failures="([^"]+)".*errors="([^"]+)".*/\1 tests=\2 skipped=\3 failures=\4 errors=\5/' || true
  } > "$OUTDIR/checks/gradle-test-results-summary.txt"
fi

if [[ -d "$ROOT/build/libs" ]]; then
  find "$ROOT/build/libs" -maxdepth 1 -type f -print | sort > "$OUTDIR/checks/built-jars.txt"
  if find "$ROOT/build/libs" -maxdepth 1 -type f -name '*.jar' | grep -q .; then
    (cd "$ROOT" && sha256sum build/libs/*.jar) > "$OUTDIR/checks/built-jars-sha256.txt"
  fi
fi

tar -czf "$ARCHIVE" -C "$(dirname "$OUTDIR")" "$(basename "$OUTDIR")"
cp "$ARCHIVE" "$LATEST"

echo "Created:"
echo "  $ARCHIVE"
echo "  $LATEST"
echo
echo "HEAD:"
echo "  $HEAD_SHA $HEAD_SUBJECT"
echo
echo "Changed files:"
cat "$OUTDIR/meta/changed-files.txt"
