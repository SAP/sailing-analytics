#!/usr/bin/env bash
#
# merge-upstream-to-downstream.sh
#
# Merge an upstream repo into a downstream repo by way of a PR opened from a
# secondary account's fork, so the account you review with stays free to approve
# the PR (branch protection commonly forbids self-approval / requires last-push
# approval by someone other than the pusher).
#
# The fork remote's URL is expected to embed a PAT (https://<token>@host/owner/repo);
# that PAT is the single source of truth for credentials and is read at runtime,
# never printed. Keep this script free of any local repo/remote/branch details:
# everything is passed as named options so the script itself is safe to commit.
#
# Flow (names below are the resolved values, see options):
#   1. Check out <local-fork-branch> (should track <fork-remote>/<fork-branch>).
#   2. Fetch <downstream-remote>/<downstream-branch> -> ff <local-downstream-branch>.
#   3. Fetch <upstream-remote>/<upstream-branch>     -> ff <local-upstream-branch>.
#   4. Merge <local-downstream-branch> into <local-fork-branch>.
#   5. Merge <local-upstream-branch>   into <local-fork-branch>.
#   6. Push <local-fork-branch> -> <fork-remote>/<fork-branch>.
#   7. Open PR <fork-owner>:<fork-branch> -> <downstream-repo>:<downstream-branch>,
#      body = incoming commit titles, maintainer-edit enabled (gh default),
#      authored via the fork PAT.
#
# Options (all named; only --fork-remote is required unless MERGE_U2D_FORK_REMOTE
# is set). Precedence: command-line option > MERGE_U2D_* env var > built-in default.
#   --fork-remote NAME           (required) remote for the fork; its URL holds the PAT
#                                env: MERGE_U2D_FORK_REMOTE
#   --upstream-remote NAME       default: eclipse    env: MERGE_U2D_UPSTREAM_REMOTE
#   --downstream-remote NAME     default: sap        env: MERGE_U2D_DOWNSTREAM_REMOTE
#   --upstream-branch NAME       default: main       env: MERGE_U2D_UPSTREAM_BRANCH
#   --downstream-branch NAME     default: main       env: MERGE_U2D_DOWNSTREAM_BRANCH
#   --fork-branch NAME           default: main       env: MERGE_U2D_FORK_BRANCH
#   --local-upstream-branch NAME   default: <upstream-remote>-<upstream-branch>
#                                env: MERGE_U2D_LOCAL_UPSTREAM_BRANCH
#   --local-downstream-branch NAME default: <downstream-remote>-<downstream-branch>
#                                env: MERGE_U2D_LOCAL_DOWNSTREAM_BRANCH
#   --local-fork-branch NAME       default: <fork-remote>-<downstream-remote>-<downstream-branch>
#                                env: MERGE_U2D_LOCAL_FORK_BRANCH
#   -h | --help                  show this help and exit
#
# So, e.g., `export MERGE_U2D_FORK_REMOTE=aksajhfduwafe` in your .bashrc lets you
# run the script with no arguments at all.
#
# Example (author's layout, downstream remote actually named "github"):
#   ./merge-upstream-to-downstream.sh --fork-remote aksajhfduwafe --downstream-remote github \
#       --local-downstream-branch sap-main --local-fork-branch aksajhfduwafe-sap-main
#
#   (Defaults assume the downstream remote is named "sap"; the author's is "github",
#   so the local-branch names are given explicitly to match sap-main / aksajhfduwafe-sap-main.
#   Alternatively rename the remote to "sap" and the local-branch defaults line up.)

set -euo pipefail

# --- defaults -----------------------------------------------------------------
# Precedence for every setting: command-line option > environment variable >
# built-in default. Each var is seeded here from its MERGE_U2D_* environment
# variable (empty if unset) or the built-in default; the option parser below
# then overwrites a var ONLY when its flag is actually given. So you can export
# e.g. MERGE_U2D_FORK_REMOTE=aksajhfduwafe in your .bashrc and run with no args,
# while still overriding any single value on the command line.
FORK_REMOTE="${MERGE_U2D_FORK_REMOTE:-}"
UPSTREAM_REMOTE="${MERGE_U2D_UPSTREAM_REMOTE:-eclipse}"
DOWNSTREAM_REMOTE="${MERGE_U2D_DOWNSTREAM_REMOTE:-sap}"
UPSTREAM_BRANCH="${MERGE_U2D_UPSTREAM_BRANCH:-main}"
DOWNSTREAM_BRANCH="${MERGE_U2D_DOWNSTREAM_BRANCH:-main}"
FORK_BRANCH="${MERGE_U2D_FORK_BRANCH:-main}"
LOCAL_UPSTREAM_BRANCH="${MERGE_U2D_LOCAL_UPSTREAM_BRANCH:-}"     # resolved after parsing if left empty
LOCAL_DOWNSTREAM_BRANCH="${MERGE_U2D_LOCAL_DOWNSTREAM_BRANCH:-}"
LOCAL_FORK_BRANCH="${MERGE_U2D_LOCAL_FORK_BRANCH:-}"

usage() {
  cat <<EOF
Usage: $(basename "$0") --fork-remote NAME [options]

Merge an upstream repo into a downstream repo via a PR opened from a secondary
account's fork, so the reviewing account stays free to approve. The fork remote's
URL must embed a PAT (https://<token>@host/owner/repo); it is read at runtime and
never printed.

Precedence for every setting: command-line option > MERGE_U2D_* env var > default.
Defaults shown below already reflect any MERGE_U2D_* env var currently exported,
so e.g. \`export MERGE_U2D_FORK_REMOTE=aksajhfduwafe\` lets you run with no args.

Required (unless MERGE_U2D_FORK_REMOTE is set):
  --fork-remote NAME             remote for the fork; its URL holds the PAT
                                 [env MERGE_U2D_FORK_REMOTE] (current: ${FORK_REMOTE:-<unset>})

Remotes:
  --upstream-remote NAME         upstream remote            (default: ${UPSTREAM_REMOTE})
                                 [env MERGE_U2D_UPSTREAM_REMOTE]
  --downstream-remote NAME       downstream/base remote     (default: ${DOWNSTREAM_REMOTE})
                                 [env MERGE_U2D_DOWNSTREAM_REMOTE]

Remote branches:
  --upstream-branch NAME         branch on upstream remote  (default: ${UPSTREAM_BRANCH})
                                 [env MERGE_U2D_UPSTREAM_BRANCH]
  --downstream-branch NAME       branch on downstream remote(default: ${DOWNSTREAM_BRANCH})
                                 [env MERGE_U2D_DOWNSTREAM_BRANCH]
  --fork-branch NAME             branch on fork remote      (default: ${FORK_BRANCH})
                                 [env MERGE_U2D_FORK_BRANCH]

Local branches (defaults derive from the names above):
  --local-upstream-branch NAME   default: <upstream-remote>-<upstream-branch>
                                 [env MERGE_U2D_LOCAL_UPSTREAM_BRANCH]
  --local-downstream-branch NAME default: <downstream-remote>-<downstream-branch>
                                 [env MERGE_U2D_LOCAL_DOWNSTREAM_BRANCH]
  --local-fork-branch NAME       default: <fork-remote>-<downstream-remote>-<downstream-branch>
                                 [env MERGE_U2D_LOCAL_FORK_BRANCH]

Other:
  -h, --help                     show this help and exit

Example (downstream remote named "github" rather than the default "sap"):
  $(basename "$0") --fork-remote aksajhfduwafe --downstream-remote github \\
      --local-downstream-branch sap-main --local-fork-branch aksajhfduwafe-sap-main
EOF
}

# --- named-option parsing -----------------------------------------------------
while [ $# -gt 0 ]; do
  case "$1" in
    --fork-remote)            FORK_REMOTE="$2"; shift 2 ;;
    --upstream-remote)        UPSTREAM_REMOTE="$2"; shift 2 ;;
    --downstream-remote)      DOWNSTREAM_REMOTE="$2"; shift 2 ;;
    --upstream-branch)        UPSTREAM_BRANCH="$2"; shift 2 ;;
    --downstream-branch)      DOWNSTREAM_BRANCH="$2"; shift 2 ;;
    --fork-branch)            FORK_BRANCH="$2"; shift 2 ;;
    --local-upstream-branch)   LOCAL_UPSTREAM_BRANCH="$2"; shift 2 ;;
    --local-downstream-branch) LOCAL_DOWNSTREAM_BRANCH="$2"; shift 2 ;;
    --local-fork-branch)       LOCAL_FORK_BRANCH="$2"; shift 2 ;;
    --*=*)  # support --opt=value form
      set -- "${1%%=*}" "${1#*=}" "${@:2}" ;;
    -h|--help) usage; exit 0 ;;
    *) printf 'Unknown option: %s\n\n' "$1" >&2; usage >&2; exit 2 ;;
  esac
done

# --- resolve derived defaults -------------------------------------------------
[ -n "$FORK_REMOTE" ] || { printf 'ERROR: --fork-remote is required (or set MERGE_U2D_FORK_REMOTE).\n\n' >&2; usage >&2; exit 2; }
: "${LOCAL_UPSTREAM_BRANCH:=${UPSTREAM_REMOTE}-${UPSTREAM_BRANCH}}"
: "${LOCAL_DOWNSTREAM_BRANCH:=${DOWNSTREAM_REMOTE}-${DOWNSTREAM_BRANCH}}"
: "${LOCAL_FORK_BRANCH:=${FORK_REMOTE}-${DOWNSTREAM_REMOTE}-${DOWNSTREAM_BRANCH}}"

PR_TITLE="Merged latest ${UPSTREAM_REMOTE}/${UPSTREAM_BRANCH} updates to downstream"
# ------------------------------------------------------------------------------

log() { printf '\n\033[1;34m==>\033[0m %s\n' "$*"; }
die() { printf '\033[1;31mERROR:\033[0m %s\n' "$*" >&2; exit 1; }

# Restore the branch the user started on, even on failure; clean temp file.
ORIGINAL_REF="$(git rev-parse --abbrev-ref HEAD)"
PR_BODY_FILE=""
cleanup() {
  [ -n "$PR_BODY_FILE" ] && rm -f "$PR_BODY_FILE"
  git checkout --quiet "$ORIGINAL_REF" 2>/dev/null || true
}
trap cleanup EXIT

# --- preflight ----------------------------------------------------------------
command -v gh >/dev/null 2>&1 || die "gh CLI not found on PATH."
for r in "$FORK_REMOTE" "$UPSTREAM_REMOTE" "$DOWNSTREAM_REMOTE"; do
  git remote get-url "$r" >/dev/null 2>&1 || die "remote '$r' not defined."
done
git show-ref --verify --quiet "refs/heads/$LOCAL_FORK_BRANCH" \
  || die "local branch '$LOCAL_FORK_BRANCH' does not exist (create it tracking $FORK_REMOTE/$FORK_BRANCH)."
git show-ref --verify --quiet "refs/heads/$LOCAL_DOWNSTREAM_BRANCH" \
  || die "local branch '$LOCAL_DOWNSTREAM_BRANCH' does not exist (create it tracking $DOWNSTREAM_REMOTE/$DOWNSTREAM_BRANCH)."
git show-ref --verify --quiet "refs/heads/$LOCAL_UPSTREAM_BRANCH" \
  || die "local branch '$LOCAL_UPSTREAM_BRANCH' does not exist (create it tracking $UPSTREAM_REMOTE/$UPSTREAM_BRANCH)."
# Block only on modified/staged TRACKED files — those are what branch switching
# and merging would clobber. Untracked files (build artifacts, .bak, etc.) are
# left alone by checkout/merge, so they must not block the run.
[ -z "$(git status --porcelain --untracked-files=no)" ] || die "working tree has uncommitted tracked changes; commit/stash first."

# Extract the bare PAT embedded in the fork remote URL (https://<token>@host/...).
# Never echoed; only exported into gh's environment for the PR call.
FORK_URL="$(git remote get-url "$FORK_REMOTE")"
FORK_PAT="$(printf '%s' "$FORK_URL" | sed -nE 's#^https://([^@/]+)@.*#\1#p')"
[ -n "$FORK_PAT" ] || die "could not extract a PAT from the '$FORK_REMOTE' remote URL (expected https://<token>@host/owner/repo)."
FORK_OWNER="$(printf '%s' "$FORK_URL" | sed -nE 's#^https://[^@/]+@[^/]+/([^/]+)/.*#\1#p')"
FORK_HOST="$(printf '%s' "$FORK_URL" | sed -nE 's#^https://[^@/]+@([^/]+)/.*#\1#p')"
[ -n "$FORK_OWNER" ] || die "could not parse the fork owner from the '$FORK_REMOTE' URL."
: "${FORK_HOST:=github.com}"

# Derive the downstream (base) repo "owner/name" from the downstream remote URL,
# supporting both git@host:owner/repo(.git) and https://host/owner/repo(.git).
# Strip any trailing ".git" explicitly — a non-greedy name group won't drop it.
DOWNSTREAM_URL="$(git remote get-url "$DOWNSTREAM_REMOTE")"
BASE_REPO="$(printf '%s' "$DOWNSTREAM_URL" \
  | sed -nE 's#^(git@[^:]+:|https?://[^/]+/)([^/]+/[^/]+)$#\2#p')"
BASE_REPO="${BASE_REPO%.git}"
[ -n "$BASE_REPO" ] || die "could not derive owner/name from '$DOWNSTREAM_REMOTE' URL: $DOWNSTREAM_URL"

# --- 1. check out the fork branch ---------------------------------------------
log "Checking out $LOCAL_FORK_BRANCH"
git checkout "$LOCAL_FORK_BRANCH"

# --- 2. fetch downstream into its local branch --------------------------------
log "Fetching $DOWNSTREAM_REMOTE/$DOWNSTREAM_BRANCH into $LOCAL_DOWNSTREAM_BRANCH"
git fetch "$DOWNSTREAM_REMOTE" "$DOWNSTREAM_BRANCH"
git checkout "$LOCAL_DOWNSTREAM_BRANCH"
git merge --ff-only "$DOWNSTREAM_REMOTE/$DOWNSTREAM_BRANCH" \
  || die "$LOCAL_DOWNSTREAM_BRANCH cannot fast-forward to $DOWNSTREAM_REMOTE/$DOWNSTREAM_BRANCH (local diverged?)."

# --- 3. fetch upstream into its local branch ----------------------------------
log "Fetching $UPSTREAM_REMOTE/$UPSTREAM_BRANCH into $LOCAL_UPSTREAM_BRANCH"
git fetch "$UPSTREAM_REMOTE" "$UPSTREAM_BRANCH"
git checkout "$LOCAL_UPSTREAM_BRANCH"
git merge --ff-only "$UPSTREAM_REMOTE/$UPSTREAM_BRANCH" \
  || die "$LOCAL_UPSTREAM_BRANCH cannot fast-forward to $UPSTREAM_REMOTE/$UPSTREAM_BRANCH (local diverged?)."

# --- upstream-contribution guard ----------------------------------------------
# The whole point is to carry UPSTREAM changes downstream. If upstream has no
# commit that isn't already on downstream main, there is nothing to contribute:
# stop before merging/pushing/PRing so we never open a ridiculous empty (or
# downstream-into-itself) PR. (A downstream-only advance with no upstream content
# is excluded here too, precisely because it contributes nothing from upstream.)
UPSTREAM_CONTRIB_COUNT="$(git rev-list --count "$DOWNSTREAM_REMOTE/$DOWNSTREAM_BRANCH..$LOCAL_UPSTREAM_BRANCH")"
if [ "$UPSTREAM_CONTRIB_COUNT" -eq 0 ]; then
  log "$UPSTREAM_REMOTE/$UPSTREAM_BRANCH has no commits beyond $DOWNSTREAM_REMOTE/$DOWNSTREAM_BRANCH — nothing to contribute. No merge, push, or PR. Done."
  exit 0
fi
log "$UPSTREAM_CONTRIB_COUNT upstream commit(s) not yet on downstream; proceeding."

# --- 4 & 5. merge downstream then upstream into the fork branch ---------------
git checkout "$LOCAL_FORK_BRANCH"
log "Merging $LOCAL_DOWNSTREAM_BRANCH into $LOCAL_FORK_BRANCH"
git merge --no-edit "$LOCAL_DOWNSTREAM_BRANCH" \
  || die "merge of $LOCAL_DOWNSTREAM_BRANCH hit conflicts; resolve, commit, then re-run (fetches are idempotent)."
log "Merging $LOCAL_UPSTREAM_BRANCH into $LOCAL_FORK_BRANCH"
git merge --no-edit "$LOCAL_UPSTREAM_BRANCH" \
  || die "merge of $LOCAL_UPSTREAM_BRANCH hit conflicts; resolve, commit, then re-run."

# --- compute the commits this PR would bring into the downstream base ---------
# Broader than the upstream-contribution guard above: this is everything the PR
# delivers to downstream (upstream commits + any downstream re-sync), used for
# the PR body. It stays a defensive belt-and-braces check — if it somehow ends
# up empty (e.g. everything was already merged concurrently), don't push/PR.
INCOMING_RANGE="$DOWNSTREAM_REMOTE/$DOWNSTREAM_BRANCH..$LOCAL_FORK_BRANCH"
INCOMING_COUNT="$(git rev-list --count "$INCOMING_RANGE")"
if [ "$INCOMING_COUNT" -eq 0 ]; then
  log "No new commits over $DOWNSTREAM_REMOTE/$DOWNSTREAM_BRANCH — nothing to push or PR. Done."
  exit 0
fi
log "$INCOMING_COUNT commit(s) will be proposed to $BASE_REPO."

# --- already-open-PR short-circuit (idempotency) ------------------------------
# If an open PR already exists from this fork head into the downstream base AND
# its head commit already equals what we're about to push, there is nothing to
# do: don't push, don't open a duplicate PR — just report the existing one. If a
# PR exists but its head is OLDER (new upstream commits arrived since), we fall
# through and push, which updates that same PR; step 7 then reports it.
HEAD_SPEC="${FORK_OWNER}:${FORK_BRANCH}"
LOCAL_FORK_TIP="$(git rev-parse "$LOCAL_FORK_BRANCH")"
# Match on head branch AND head-fork owner (—head filters by branch name only,
# which could collide with a same-named branch from a different fork). Emit
# "<sha> <url>" for the first match, or nothing.
EXISTING_PR="$(GH_TOKEN="$FORK_PAT" GH_HOST="$FORK_HOST" \
  gh pr list --repo "$BASE_REPO" --head "$FORK_BRANCH" --base "$DOWNSTREAM_BRANCH" --state open \
    --json url,headRefOid,headRepositoryOwner \
    --jq "[.[] | select(.headRepositoryOwner.login == \"$FORK_OWNER\")][0] | select(.) | \"\(.headRefOid) \(.url)\"" 2>/dev/null || true)"
if [ -n "$EXISTING_PR" ]; then
  EXISTING_PR_SHA="${EXISTING_PR%% *}"
  EXISTING_PR_URL="${EXISTING_PR#* }"
  if [ "$EXISTING_PR_SHA" = "$LOCAL_FORK_TIP" ]; then
    log "An open PR already targets $BASE_REPO:$DOWNSTREAM_BRANCH from $HEAD_SPEC at this exact commit — nothing to do: $EXISTING_PR_URL"
    exit 0
  fi
  log "An open PR exists ($EXISTING_PR_URL) but at an older head; pushing will update it."
fi

# --- 6. push the fork branch --------------------------------------------------
log "Pushing $LOCAL_FORK_BRANCH -> $FORK_REMOTE/$FORK_BRANCH"
git push "$FORK_REMOTE" "$LOCAL_FORK_BRANCH:$FORK_BRANCH"

# --- 7. create (or report) the PR ---------------------------------------------
PR_BODY_FILE="$(mktemp)"
{
  echo "Automated merge of upstream \`$UPSTREAM_REMOTE/$UPSTREAM_BRANCH\` (and re-sync of downstream) into \`$BASE_REPO:$DOWNSTREAM_BRANCH\`."
  echo
  echo "### Incoming commits ($INCOMING_COUNT)"
  echo
  git log --no-merges --pretty=format:'- %s (%h)' "$INCOMING_RANGE"
  echo
} > "$PR_BODY_FILE"

log "Opening PR $HEAD_SPEC -> $BASE_REPO:$DOWNSTREAM_BRANCH (authored by the fork account)"
# GH_TOKEN makes gh act as the fork account so the maintainer-edit grant sticks
# (gh enables it by default; we deliberately omit --no-maintainer-edit) and the
# review account stays free to approve. GH_HOST pins gh to the fork's host.
# NOTE: `gh pr create --head` takes owner:branch (cross-fork form), but
# `gh pr list --head` (the fallback below) takes the BRANCH NAME ONLY — hence we
# filter the list by head-fork owner separately, matching the early check above.
if ! GH_TOKEN="$FORK_PAT" GH_HOST="$FORK_HOST" \
     gh pr create \
       --repo "$BASE_REPO" \
       --base "$DOWNSTREAM_BRANCH" \
       --head "$HEAD_SPEC" \
       --title "$PR_TITLE" \
       --body-file "$PR_BODY_FILE" ; then
  EXISTING="$(GH_TOKEN="$FORK_PAT" GH_HOST="$FORK_HOST" \
    gh pr list --repo "$BASE_REPO" --head "$FORK_BRANCH" --base "$DOWNSTREAM_BRANCH" --state open \
      --json url,headRepositoryOwner \
      --jq "[.[] | select(.headRepositoryOwner.login == \"$FORK_OWNER\")][0] | select(.) | .url" 2>/dev/null || true)"
  [ -n "$EXISTING" ] && log "A PR already exists for $HEAD_SPEC (updated by the push): $EXISTING" \
                     || die "gh pr create failed and no existing open PR was found."
fi

log "Done."
