#!/usr/bin/env python3
"""Reorganize flat S3-access + CloudFront logs into a merged date-partitioned layout.

 Phase 1 (local): rename flat files into <PART>/YYYY/MM/DD/ (metadata-only; cheap on sc1).
 Phase 2 (S3):    for each old flat key -> server-side copy to partitioned key, then delete
                  the flat key.  No local reads; explicit per-key ops; threaded; batched deletes.

 Needs boto3:  sudo dnf install -y python3-boto3   (or: pip3 install boto3)
"""

import argparse, os, re, sys, time
from concurrent.futures import ThreadPoolExecutor, as_completed

# ================= CONFIG =================
BASE   = "/var/log/old/cache/aws/sapsailing-access-logs"
BUCKET = "sapsailing-access-logs"
REGION = "eu-west-1"
PART   = "media.sapsailing.com017363970217/eu-west-1/media.sapsailing.com"
MERGED_LOCAL = os.path.join(BASE, "logs", PART)
MERGED_S3    = f"logs/{PART}"
WORKERS = 32

JOBS = [
    {   # S3 server access logs
        "name": "s3access",
        "srcdir": os.path.join(BASE, "logs"),
        "file_re": re.compile(r"^media\.sapsailing\.com(20\d\d)-([01]\d)-([0-3]\d)-"),
        "strip": "media.sapsailing.com",
        # narrowed so we never list the partitioned subtree (…com017…):
        "s3_list_prefix": "logs/media.sapsailing.com20",
        "s3_key_re": re.compile(r"^logs/media\.sapsailing\.com((20\d\d)-([01]\d)-([0-3]\d)-.*)$"),
        "name_g": 1, "ymd_g": (2, 3, 4),
        # single-day listings: date sits right after the prefix, so scope server-side
        "date_prefix": lambda d: f"logs/media.sapsailing.com{d}",
    },
    {   # CloudFront legacy logs (merged into same tree, names kept)
        "name": "cloudfront",
        "srcdir": os.path.join(BASE, "media-sapsailing-com"),
        "file_re": re.compile(r"^[A-Za-z0-9]+\.(20\d\d)-([01]\d)-([0-3]\d)-\d\d\."),
        "strip": None,
        "s3_list_prefix": "media-sapsailing-com/",
        "s3_key_re": re.compile(r"^media-sapsailing-com/([A-Za-z0-9]+\.(20\d\d)-([01]\d)-([0-3]\d)-\d\d\..*)$"),
        "name_g": 1, "ymd_g": (2, 3, 4),
        # date follows the distribution id, so it can't be narrowed by a simple prefix:
        "date_prefix": lambda d: None,
    },
]
# ==========================================

START = time.monotonic()
AUTOYES = False
def log(m): print(f"[{time.strftime('%H:%M:%S')} +{time.monotonic()-START:6.0f}s] {m}", flush=True)
def confirm(prompt, require="y"):
    if AUTOYES: return True
    print(f"\n>>> {prompt} ", end="", flush=True)
    a = sys.stdin.readline().strip()
    return a == require if require != "y" else a.lower() in ("y", "yes")
def day_ok(y, mo, d, date): return (not date) or (f"{y}-{mo}-{d}" == date)
def dest_name(job, name):
    s = job.get("strip")
    return name[len(s):] if s and name.startswith(s) else name

# ---------------- PHASE 1: local renames ----------------
def phase1(date):
    log("=== PHASE 1: local restructure (rename, metadata-only) ===")
    plan = []
    for job in JOBS:
        src, fre = job["srcdir"], job["file_re"]
        if not os.path.isdir(src): log(f"[{job['name']}] {src} missing, skip"); continue
        lst = os.path.join("/var/tmp", f"logmig.{job['name']}.list")
        seen = matched = 0
        log(f"[{job['name']}] scanning {src} ...")
        with open(lst, "w") as out, os.scandir(src) as it:
            for e in it:
                seen += 1
                if seen % 1_000_000 == 0: log(f"[{job['name']}]   scanned {seen:,} matched {matched:,}")
                if not e.is_file(follow_symlinks=False): continue
                m = fre.match(e.name)
                if not m: continue
                y, mo, d = m.groups()
                if not day_ok(y, mo, d, date): continue
                out.write(f"{y}\t{mo}\t{d}\t{e.name}\n"); matched += 1
        log(f"[{job['name']}] scan done: {seen:,} entries, {matched:,} to migrate")
        plan.append((job, lst, matched))
    grand = sum(x[2] for x in plan)
    log(f"PHASE 1: {grand:,} local files would move")
    if grand == 0 or not confirm("Execute local moves now? [y/N]"):
        log("Phase 1 NOT executed."); return
    for job, lst, matched in plan:
        src, made, moved, t0 = job["srcdir"], set(), 0, time.monotonic()
        with open(lst) as f:
            for ln in f:
                y, mo, d, name = ln.rstrip("\n").split("\t", 3)
                dd = os.path.join(MERGED_LOCAL, y, mo, d)
                if dd not in made: os.makedirs(dd, exist_ok=True); made.add(dd)
                try: os.rename(os.path.join(src, name), os.path.join(dd, dest_name(job, name)))
                except (FileNotFoundError, FileExistsError): pass
                moved += 1
                if moved % 200_000 == 0:
                    log(f"[{job['name']}]   moved {moved:,}/{matched:,}  {moved/max(time.monotonic()-t0,1e-9):,.0f}/s")
        log(f"[{job['name']}] MOVED {moved:,}")

# ---------------- PHASE 2: server-side S3 repartition ----------------
def phase2(date, only, workers, dry):
    log("=== PHASE 2: server-side S3 copy+delete ===")
    try:
        import boto3
    except ImportError:
        sys.exit("boto3 not installed:  sudo dnf install -y python3-boto3   (or pip3 install boto3)")
    s3 = boto3.client("s3", region_name=REGION)
    for job in JOBS:
        if only and job["name"] != only: continue
        kre, ng, yg = job["s3_key_re"], job["name_g"], job["ymd_g"]
        lp = job["s3_list_prefix"]
        if date:
            dp = job.get("date_prefix", lambda d: None)(date)
            if dp:
                lp = dp
            else:
                log(f"[{job['name']}] cannot narrow listing by date; scanning full prefix (slow)")
        copied = deleted = skipped = 0; next_rep = 50_000; t0 = time.monotonic(); shown = 0
        log(f"[{job['name']}] paginating s3://{BUCKET}/{lp} ...")
        pag = s3.get_paginator("list_objects_v2")
        ex = ThreadPoolExecutor(max_workers=workers)
        try:
            for page in pag.paginate(Bucket=BUCKET, Prefix=lp):
                pairs = []
                for obj in page.get("Contents", []):
                    key = obj["Key"]; m = kre.match(key)
                    if not m: continue
                    y, mo, d = (m.group(g) for g in yg)
                    if not day_ok(y, mo, d, date): continue
                    sc = obj.get("StorageClass", "STANDARD")
                    if sc in ("GLACIER", "DEEP_ARCHIVE"):
                        log(f"[{job['name']}] SKIP archived {key} ({sc})"); skipped += 1; continue
                    newkey = f"{MERGED_S3}/{y}/{mo}/{d}/{m.group(ng)}"
                    pairs.append((key, newkey, sc))
                if not pairs: continue
                if dry:
                    while shown < 5 and shown < len(pairs):
                        log(f"[{job['name']}] WOULD copy {pairs[shown][0]} -> {pairs[shown][1]}"); shown += 1
                    copied += len(pairs); continue
                futs = {ex.submit(s3.copy_object, Bucket=BUCKET, Key=nk,
                                  CopySource={"Bucket": BUCKET, "Key": ok}, StorageClass=sc): ok
                        for (ok, nk, sc) in pairs}
                done = []
                for fut in as_completed(futs):
                    src_key = futs[fut]
                    try: fut.result(); done.append(src_key)
                    except Exception as e: log(f"[{job['name']}] COPY FAIL {src_key}: {e}")
                copied += len(done)
                for i in range(0, len(done), 1000):
                    chunk = done[i:i+1000]
                    s3.delete_objects(Bucket=BUCKET,
                                      Delete={"Objects": [{"Key": k} for k in chunk], "Quiet": True})
                    deleted += len(chunk)
                if copied >= next_rep:
                    log(f"[{job['name']}]   copied {copied:,} deleted {deleted:,} "
                        f"{copied/max(time.monotonic()-t0,1e-9):,.0f}/s")
                    next_rep += 50_000
        finally:
            ex.shutdown(wait=True)
        log(f"[{job['name']}] {'WOULD migrate' if dry else 'DONE'}: "
            f"copied {copied:,} deleted {deleted:,} skipped {skipped:,}")

def main():
    global AUTOYES
    ap = argparse.ArgumentParser(description="Server-side log repartition (local rename + S3 copy/delete).")
    ap.add_argument("--date", help="restrict to one day YYYY-MM-DD")
    ap.add_argument("--phase", action="append", choices=["1", "2"], help="default = both")
    ap.add_argument("--only", help="S3 phase: only this job name")
    ap.add_argument("--workers", type=int, default=WORKERS)
    ap.add_argument("--dry-run", action="store_true", help="Phase 2 preview (no copy/delete)")
    ap.add_argument("--yes", action="store_true", help="skip confirmations")
    args = ap.parse_args()
    if args.date and not re.fullmatch(r"20\d\d-[01]\d-[0-3]\d", args.date):
        sys.exit("--date must be YYYY-MM-DD")
    AUTOYES = args.yes
    phases = args.phase or ["1", "2"]
    log(f"Scope: {'day '+args.date if args.date else 'ALL'} | phases {','.join(phases)} "
        f"| workers {args.workers} | {'DRY-RUN' if args.dry_run else 'LIVE'}")
    if "1" in phases:
        phase1(args.date)
    if "2" in phases and confirm("Proceed to PHASE 2 (server-side S3 copy+delete)? [y/N]"):
        if not args.dry_run and not confirm("This COPIES then DELETES in S3. Type GO to run live:", require="GO"):
            log("Phase 2 not executed (dry-run it with --dry-run)."); return
        phase2(args.date, args.only, args.workers, args.dry_run)
    log("ALL REQUESTED PHASES COMPLETE")

if __name__ == "__main__":
    main()
