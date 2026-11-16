# Offline scoring regression fixtures (#6218)

Run `com.sap.sailing.domain.test.ProductionScoringReplayTest` and
`com.sap.sailing.domain.test.ScoringSchemeInteractionTest` as JUnit tests in
`com.sap.sailing.domain.test`, with `-ea` in the launch configuration's VM
arguments. Both suites check that Java assertions in the domain model are
enabled. When using Eclipse MCP, pass a saved launch configuration with
`-ea` as `launcherName`; generated launches may omit it. Tests require no
network or credentials.

## Scope and provenance

57 JSON files represent 35 top-level boards plus 22 child boards: 1,177
competitor rows and 16,960 cells. The 26 production-represented scoring types
are complemented by synthetic cases for `HIGH_POINT_WINNER_GETS_EIGHT` and
`HIGH_POINT_BY_WINS_TIES_LASTLY_BROKEN_BY_OTHER_LEADERBOARD`, covering all 28
current types. This is behavioral regression coverage, not proof of every
rule interaction or an endorsement of every historical configuration.

Each fixture retains result/configuration URLs, capture times and hashes,
and the originally observed scheme. Full raw captures are preserved under
`/home/uhl/data/SAP/sailing/research/bug6218/6218-production-audit/`.
To regenerate from that archive, run from the repository root:

```sh
python3 java/com.sap.sailing.domain.test/resources/scoring/prepare_fixtures.py \
  /home/uhl/data/SAP/sailing/research/bug6218/6218-production-audit
```

The extractor is offline. Expected values come from captured results;
`expected*` fields are assertions only. The loader imports unweighted cell
corrections and penalty labels, constructs real regatta or flexible leaderboards and race-log
registrations, and lets Java recompute totals, discards and rankings.
Actual child leaderboards feed overall columns; only explicitly corrected
overall cells are overridden. Shared competitor identities are retained.
Parent suppression is inferred from child IDs missing from the captured
parent field, as labelled in the fixture. Every child and parent is checked
with both forward and reverse competitor insertion order.
Absent expected net scores are explicitly asserted as null, covering 2,859
cells; they are not skipped or treated as zero.

Raw finish-rank conversion, penalties, race-log overrides, redress and
time-dependent scores are exercised separately by the interaction tests
and the existing scoring suites. `uncorrectedTotalPoints` is not imported.

## Complete flexible leaderboards

Three fresh production captures reconstruct actual `FlexibleLeaderboardImpl`
instances, preserving column order, Medal flags, factors and the default fleet:

| Fixture | Captured scheme | Competitors × columns | Scoring features |
|---|---|---:|---|
| `flexible-qingdao-2011` | `HIGH_POINT` | 9 × 29 | No discards; R29 Medal factor 2; corrected DNF |
| `flexible-kiel-2012-470w` | `LOW_POINT` | 12 × 7 | Discard threshold 5; M Medal factor 2; penalty labels |
| `flexible-505-worlds-2011` | `LOW_POINT` | 84 × 7 | Discard threshold 5; ordinary-race ties; DNF/DNS/DNC |

All 933 cells have numeric scores and expected net scores. Each replay checks
computed totals, discards, net cells and whole-field ranking in both competitor
insertion directions. Column configuration and row completeness are also asserted.
Empty exported fleet membership remains empty (two Kiel M cells and eleven 505
cells); numeric corrections still apply. The captured Kiel Medal values are
retained exactly, including fractional unweighted corrections implied by factor 2.

Results and `SailingService.getLeaderboard` replies were captured on 2026-11-12
under `flexible-fixtures-20261112/` in the audit archive, with request/time/SHA-256
provenance. Fresh GWT decoding confirmed the earlier metadata. The fourth candidate,
Kiel 2012 2.4mR, has 350 of 360 numeric cells: Cermak is entirely unscored. Its
capture is retained; the three hole-free boards above were selected for these tests.

## One-stays-one weighting and targeted interactions

`youth2025-windsurf-women` preserves the complete Youth Worlds 2025 Female
Windsurfer board (26 × 7). R3–R5 use factor 2 with `oneAlwaysStaysOne=true`:
unweighted 1, 2 and 4 become 1, 3 and 7. Leaderboard discard thresholds are
`[3,8,16]`; a fresh `getRegattaByName` capture independently confirms that no
series overrides them. The empty Slalom2 series retains its true flag too.
Raw replies and decoder provenance are in `one-stays-one-20261112/`.

The full replay checks all cells, discards, totals and rankings in both
insertion directions. A focused test independently anchors unweighted and
weighted values, winner Medea Falcioni's six-point total, and Martina Mantovani
Borrás/Alba Klein Meghnagi's 36-point tie with different discards. Correctly
weighted A8.1 puts Klein Meghnagi fifth and Mantovani Borrás sixth, matching the
official results. The captured ranks remain in the JSON; the replay explicitly
asserts the repaired ranks for this pair and the generic 49erFX change below.
Fixture inversion uses captured flags rather than the reconstructed model's
flags so that input and output mistakes cannot compensate for each other.

Interaction tests additionally cover:

- A8.1 applies the column factor exactly once to corrected unweighted scores,
  for both ordinary and Medal callers. Half/double factors distinguish weighted
  comparison from original-score comparison and accidental second weighting.
- One-stays-one weighting with existing discards and non-discardable Medal scores;
  factor-zero scores are excluded from A8.1 before sorting, including missing and
  explicit null entries under both null-ordering policies.
- Two sailed wins, or a carry win plus one sailed win, overriding a worse
  points sum (10 versus 7), with both carry-flag settings. Ordinary LowPoint
  controls use identical factor-1 Medal columns and select the lower total.
- A=(Q2,F1=3,F2=3) versus B=(Q3,F1=1,F2=4), with no two-win champion:
  flagged Q selects A by carry score; unflagged Q selects B through A8.
- Null Final scores retaining Qualification totals, a valid zero triggering
  the reset, and removal of that correction restoring the previous total.
- Every ordered competitor pair agreeing with each asserted small-field
  display order, including self-comparisons and reverse comparisons. Sporting
  equality remains tested separately from display order.

## Important interpretations

- `49erfx-2013-petit` and `tonsberg-2024-elite` use fresh results and regatta
  configurations after the user's 2026-11-13 corrections. The former matches
  all 36 score cells, nine totals and nine stage ranks in the official PDF.
  The latter retains all 15 competitors and 180 cells, with Q carry-flagged;
  Oslofjorden ranks fourth ahead of Arendal on eight points under June SI 20.9.
  Raw responses, PDF, browser evidence and comparison are preserved under
  `followup-20261113/corrected-by-user/` beside the audit directory. The extractor
  uses fresh regatta flags and separately retained GWT factors/discard thresholds;
  it preserves the earlier captures. Complete replays pass in both insertion orders.
- NRV F9 in `scl2022-travemuende` retains the operations-entered 1.625 RDG award.
  The user closed this historical case on 2026-11-13; it is not an implementation
  bug and is not a target for score correction or further award investigation.
- SCL Porto Cervo 2021 and Vilamoura 2022 use the NoR-compliant first-to-two
  scheme confirmed by the user's database corrections. Their captured old
  schemes remain in provenance. Q's flag is retained; direct Qualification
  ties and A8 including Q remain distinct, unresolved rule interpretations.
- Singapore 2015's Aberdeen Challenge Qualification has discard threshold
  `[1]`, independently captured through `SailingService.getRegattaByName`.
  The REST configuration omits this setting. Q1 is therefore discarded.
- KW2014's three official 66th places remain a sporting tie. Full replay
  permits their existing sequential display ranks 66–68; a separate test
  verifies A8.1/A8.2 equality and retains the official result provenance.
- 49erFX Europeans 2017 SI 20.1.3/20.2.3 supply local discard thresholds;
  SI 20.4.1, page 6, specifies Fleet-Racing-only A8:
  <https://www.manage2sail.com/en-US/Home/DownloadDocument/91895f73-ec37-4e9d-8fc5-6c3b63d35248>.
  ARG19's `1,4,7,10` beats ESP178jun's `1,4,8,14`. Corrected generic all-series
  A8.1 instead favors ESP: Q5's unweighted 6 is now compared as weighted 3,
  rather than reciprocal 12. The complete replay explicitly expects ESP25/ARG26
  under generic LowPoint, while retaining the captured official ARG25/ESP26.
  This is not SI compliance. **Implementing SI20.4 is an accepted #6218 requirement:**
  Qualification A8; Fleet-only A8.1/A8.2 with existing discards and no Qualification
  fallback; Medal-only A8.2 without preceding Medal-sum/A8.1 preference. The new
  variant follows completion of the scoring refactoring, including synthetic Medal ties because all ten
  Medal totals in this fixture differ.
- ESS2017 penalties use the same race-log-selected twelve/eight-point basis
  as finish scores. With six finishers and an eight-point maximum, the last
  finisher gets 3, DNS/DNF/RET get 2, and DSQ/OCS get 1. Interaction tests
  cover all five reasons, both bases, column weighting and fixed-award precedence.
- `HIGH_POINT_MATCH_RACING` retains its Appendix C11.1 TODO. The graph-based
  tied-cohort scheme is separately named `HIGH_POINT_FIRST_GETS_ONE`.

Full scheme mapping, remaining interaction/rules-review gaps and executed
suite results are maintained in the durable research `SCORING_COVERAGE.md`
and `CURRENT_STATE.md`.
