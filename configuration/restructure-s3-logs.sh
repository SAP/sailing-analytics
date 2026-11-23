#!/usr/bin/env python3
"""Migrate flat S3-access + CloudFront logs into a merged date-partitioned tree,
then reconcile S3 (upload new keys, delete old flat keys). Interactive, phased."""

import argparse, json, os, re, subprocess, sys, tempfile, time

# ================= CONFIG =================
BASE   = "/var/log/old/cache/aws/sapsailing-access-logs"
BUCKET = "sapsailing-access-logs"
LISTDIR = "/var/tmp"
PART   = "media.sapsailing.com017363970217/eu-west-1/media.sapsailing.com"
MERGED_LOCAL = os.path.join(BASE, "logs", PART)   # unchanged
MERGED_S3    = f"logs/{PART}"                      # unchanged

JOBS = [
    {   # S3 server access logs:  media.sapsailing.comYYYY-MM-DD-HH-MM-SS-ID
        "name": "s3access",
        "srcdir": os.path.join(BASE, "logs"),
        "file_re":    re.compile(r"^media\.sapsailing\.com(20\d\d)-([01]\d)-([0-3]\d)-"),
        "old_prefix": "logs/media.sapsailing.com",
        "old_key_re": re.compile(r"^logs/media\.sapsailing\.com(20\d\d)-([01]\d)-([0-3]\d)-"),
    },
    {   # CloudFront legacy logs:  DISTID.YYYY-MM-DD-HH.hash.gz  (merged into same tree)
        "name": "cloudfront",
        "srcdir": os.path.join(BASE, "media-sapsailing-com"),
        "file_re":    re.compile(r"^[A-Za-z0-9]+\.(20\d\d)-([01]\d)-([0-3]\d)-\d\d\."),
        "old_prefix": "media-sapsailing-com/",
        "old_key_re": re.compile(r"^media-sapsailing-com/[A-Za-z0-9]+\.(20\d\d)-([01]\d)-([0-3]\d)-\d\d\."),
    },
]
# ====================================================================

START = time.monotonic()
AUTOYES = False
def log(m): print(f"[{time.strftime('%H:%M:%S')} +{time.monotonic()-START:6.0f}s] {m}", flush=True)

def confirm(prompt, require="y"):
    if AUTOYES: return True
    print(f"\n>>> {prompt} ", end="", flush=True)
    ans = sys.stdin.readline().strip()
    return ans == require if require != "y" else ans.lower() in ("y", "yes")

def day_matches(y, mo, d, date): return (not date) or (f"{y}-{mo}-{d}" == date)

# ---------------- PHASE 1: local restructure ----------------
def phase1(date):
    log("=== PHASE 1: local restructure (flat -> YYYY/MM/DD) ===")
    plan = []
    for job in JOBS:
        src, fre = job["srcdir"], job["file_re"]
        if not os.path.isdir(src): log(f"[{job['name']}] {src} missing, skip"); continue
        listpath = os.path.join(LISTDIR, f"logmig.{job['name']}.list")
        seen = matched = 0
        log(f"[{job['name']}] scanning {src} ...")
        with open(listpath, "w") as out, os.scandir(src) as it:
            for e in it:
                seen += 1
                if seen % 1_000_000 == 0: log(f"[{job['name']}]   scanned {seen:,} matched {matched:,}")
                if not e.is_file(follow_symlinks=False): continue
                m = fre.match(e.name)
                if not m: continue
                y, mo, d = m.groups()
                if not day_matches(y, mo, d, date): continue
                out.write(f"{y}\t{mo}\t{d}\t{e.name}\n"); matched += 1
        log(f"[{job['name']}] scan done: {seen:,} entries, {matched:,} to migrate")
        with open(listpath) as f:
            for _, ln in zip(range(4), f):
                y, mo, d, name = ln.rstrip("\n").split("\t", 3)
                log(f"[{job['name']}]   sample: {name} -> {os.path.relpath(os.path.join(MERGED_LOCAL,y,mo,d), BASE)}/")
        plan.append((job, listpath, matched))
    grand = sum(x[2] for x in plan)
    log(f"PHASE 1: {grand:,} files would move into {os.path.relpath(MERGED_LOCAL, BASE)}/YYYY/MM/DD/")
    if grand == 0 or not confirm("Execute these moves now? [y/N]"):
        log("Phase 1 moves NOT executed."); return
    for job, listpath, matched in plan:
        src, made, moved, t0 = job["srcdir"], set(), 0, time.monotonic()
        with open(listpath) as f:
            for ln in f:
                y, mo, d, name = ln.rstrip("\n").split("\t", 3)
                dd = os.path.join(MERGED_LOCAL, y, mo, d)
                if dd not in made: os.makedirs(dd, exist_ok=True); made.add(dd)
                try: os.rename(os.path.join(src, name), os.path.join(dd, name))
                except (FileNotFoundError, FileExistsError): pass
                moved += 1
                if moved % 200_000 == 0:
                    r = moved / max(time.monotonic()-t0, 1e-9)
                    log(f"[{job['name']}]   moved {moved:,}/{matched:,}  {r:,.0f}/s")
        log(f"[{job['name']}] MOVED {moved:,}")

# ---------------- PHASE 2: upload merged tree ----------------
def phase2(date):
    log("=== PHASE 2: upload merged tree to S3 ===")
    src, dst = MERGED_LOCAL + "/", f"s3://{BUCKET}/{MERGED_S3}/"
    if date:
        y, mo, d = date.split("-")
        src = os.path.join(MERGED_LOCAL, y, mo, d) + "/"
        dst = f"s3://{BUCKET}/{MERGED_S3}/{y}/{mo}/{d}/"
    preview = os.path.join(LISTDIR, "upload-preview.txt")
    log(f"Dry-run preview: aws s3 sync {src} {dst}")
    with open(preview, "w") as pf:
        subprocess.run(["aws", "s3", "sync", src, dst, "--dryrun"], stdout=pf, check=False)
    n = sum(1 for _ in open(preview))
    log(f"Upload would transfer ~{n:,} objects (full list: {preview})")
    with open(preview) as f:
        for _, ln in zip(range(4), f): log("  " + ln.rstrip())
    if n == 0 or not confirm("Run the upload for real? [y/N]"):
        log("Phase 2 upload NOT executed."); return
    subprocess.run(["aws", "s3", "sync", src, dst], check=True)
    log("Upload complete.")

# ---------------- PHASE 3: delete old flat keys ----------------
def phase3(date):
    log("=== PHASE 3: delete OLD flat keys from S3 ===")
    delfile, total = os.path.join(LISTDIR, "old-keys-to-delete.txt"), 0
    with open(delfile, "w") as out:
        for job in JOBS:
            kre = job["old_key_re"]; cnt = 0
            log(f"[{job['name']}] listing s3://{BUCKET}/{job['old_prefix']} ...")
            p = subprocess.Popen(["aws", "s3", "ls", f"s3://{BUCKET}/{job['old_prefix']}",
                                  "--recursive"], stdout=subprocess.PIPE, text=True)
            for line in p.stdout:
                parts = line.split(None, 3)
                if len(parts) < 4: continue
                key = parts[3].rstrip("\n")
                m = kre.match(key)
                if not m: continue
                if not day_matches(m.group(1), m.group(2), m.group(3), date): continue
                out.write(key + "\n"); cnt += 1; total += 1
            p.wait()
            log(f"[{job['name']}] old flat keys matched: {cnt:,}")
    log(f"PHASE 3: {total:,} OLD keys would be DELETED (full list: {delfile})")
    with open(delfile) as f:
        for _, ln in zip(range(6), f): log("  will delete: " + ln.rstrip())
    if total == 0: log("Nothing to delete."); return
    log("WARNING: permanent. Ensure PHASE 2 upload already succeeded for these dates.")
    if not confirm("Type DELETE (all caps) to proceed:", require="DELETE"):
        log("Phase 3 delete NOT executed."); return
    done, batch = 0, []
    def flush(keys):
        payload = {"Objects": [{"Key": k} for k in keys], "Quiet": True}
        tf = tempfile.NamedTemporaryFile("w", suffix=".json", delete=False, dir=LISTDIR)
        json.dump(payload, tf); tf.close()
        subprocess.run(["aws", "s3api", "delete-objects", "--bucket", BUCKET,
                        "--delete", f"file://{tf.name}"], check=True)
        os.unlink(tf.name)
    with open(delfile) as f:
        for ln in f:
            batch.append(ln.rstrip("\n"))
            if len(batch) == 1000:
                flush(batch); done += len(batch); batch = []
                if done % 100_000 == 0: log(f"  deleted {done:,}/{total:,}")
        if batch: flush(batch); done += len(batch)
    log(f"DELETED {done:,} old keys")

def main():
    global AUTOYES
    ap = argparse.ArgumentParser(description="Phased, interactive log migration + S3 reconcile.")
    ap.add_argument("--date", help="restrict ALL phases to one day YYYY-MM-DD (test scope)")
    ap.add_argument("--phase", action="append", choices=["1", "2", "3"],
                    help="run only these phase(s); default = all, in order")
    ap.add_argument("--yes", action="store_true", help="skip ALL confirmations (dangerous)")
    args = ap.parse_args()
    if args.date and not re.fullmatch(r"20\d\d-[01]\d-[0-3]\d", args.date):
        sys.exit("--date must be YYYY-MM-DD")
    AUTOYES = args.yes
    phases = args.phase or ["1", "2", "3"]
    log(f"Scope: {'day ' + args.date if args.date else 'ALL dates'} | phases {','.join(phases)}"
        f" | mode {'AUTO-YES' if AUTOYES else 'interactive'}")
    if "1" in phases: phase1(args.date)
    if "2" in phases and confirm("Proceed to PHASE 2 (upload to S3)? [y/N]"): phase2(args.date)
    if "3" in phases and confirm("Proceed to PHASE 3 (DELETE old S3 keys)? [y/N]"): phase3(args.date)
    log("ALL REQUESTED PHASES COMPLETE")

if __name__ == "__main__":
    main()
