# Setting up a fork account for the upstream→downstream merge script

> **Optional / advanced.** You only need this if you are going to *run* the
> `configuration/merge-upstream-to-downstream.sh` helper (see
> [onboarding → Syncing upstream into downstream](/wiki/howto/onboarding#syncing-upstream-into-downstream-via-a-pull-request)).
> Most developers never do this — merging upstream into downstream is a
> maintenance task handled by a few people. If that isn't you, skip this page.

The merge helper opens the downstream sync as a pull request authored from a
**secondary GitHub account's fork**, so your *primary* account stays free to
review and approve it (the downstream branch-protection rules forbid approving
your own last push). This page is the one-time setup for that secondary account
and its fork remote.

Most of these steps are deliberately **not automatable** — GitHub blocks
scripting exactly the parts that matter for account and credential creation
(CAPTCHA on signup, no API to mint a personal access token, mandatory 2FA for
contributing accounts). So the bulk of this is a manual checklist; only the last
three steps are command-line work.

Legend: 🌐 needs a web browser · 🔑 needs your MFA device · ⌨️ command line.

## One-time setup

1. 🌐 **Create the secondary GitHub account.** A plain second personal account is
   enough; it does *not* need to be a member of the SAP org. Signup requires a
   CAPTCHA and e-mail verification, so it can only be done in a browser.
2. 🌐🔑 **Enable two-factor authentication on it.** This is effectively mandatory:
   as soon as the account forks a repo or opens pull requests it falls under
   GitHub's forced-2FA rule for contributing accounts, and access is cut off if
   2FA is not enabled within the enrollment window. Set up a TOTP app (or a
   security key) now to avoid a lockout later.
3. 🌐🔑 **Log `gh` in as the secondary account.** In a shell that is *not* already
   authenticated as your primary account (use a separate `GH_CONFIG_DIR`, or log
   out first), run `gh auth login` and complete the browser device flow, entering
   the 2FA code when prompted. One-time and interactive.
4. 🌐 **Create the personal access token (PAT).** There is **no API or CLI to mint
   a PAT** — it must be created in the browser under *Settings → Developer
   settings → Personal access tokens*. A token scoped to allow pushing to the
   fork and opening pull requests on it is sufficient (for a fine-grained token,
   grant it on the fork repository with Contents + Pull requests read/write).
   Copy the token once — GitHub shows it only at creation time.
5. ⌨️ **Fork the downstream repository** (as the secondary account):
   ```
   gh repo fork SAP/sailing-analytics --clone=false --remote=false
   ```
6. ⌨️ **Add the fork as a remote with the PAT embedded in its URL.** The merge
   script reads the token from this remote URL at runtime and never prints it, so
   this remote is the **single source of truth** for the credential — after
   rotating the token you update *only* this URL. Never pass the token as a
   command-line argument (it would land in your shell history and in `ps`
   output); read it into a variable and interpolate it:
   ```
   read -rs -p 'Paste the fork PAT: ' FORK_PAT; echo
   git remote add myfork "https://${FORK_PAT}@github.com/<secondary-account>/sailing-analytics"
   unset FORK_PAT
   ```
7. ⌨️ **Fetch and create the tracking branch** the script expects (default name
   `<fork-remote>-sap-main`):
   ```
   git fetch myfork
   git checkout -b myfork-sap-main myfork/main
   ```

That's it — from now on you can run the merge script (optionally exporting
`MERGE_U2D_FORK_REMOTE=myfork` in your `~/.bashrc` so it needs no arguments).

## What cannot be scripted, and why

| Step | Automatable? | Blocker |
|------|:---:|---|
| Create the account | No | CAPTCHA + e-mail verification; no signup API |
| Enable 2FA | No | Requires your MFA device; mandatory for contributing accounts |
| `gh auth login` as the account | One-time browser | OAuth device flow + 2FA prompt |
| Fork the repo | Yes | `gh repo fork` |
| Create the PAT | No | GitHub has no endpoint to mint a PAT — browser only |
| Add the remote with the PAT | Yes | `git remote add` |
| First fetch + tracking branch | Yes | `git fetch` / `git checkout -b` |

## Security notes

- Treat the PAT like a password. Keep it *only* in the fork remote's URL; do not
  echo it, commit it, or paste it where it could be logged.
- If it ever leaks, rotate it (create a new token, delete the old one) and update
  the remote URL — rotation is the only real fix.
- Scope the token as narrowly as the fork workflow allows; it never needs access
  to the SAP org or the upstream repo.
