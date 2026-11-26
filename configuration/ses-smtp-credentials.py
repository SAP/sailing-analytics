#!/usr/bin/env python3
"""Derive region-specific SES SMTP credentials from a permanent IAM access key.

Reference: https://docs.aws.amazon.com/ses/latest/dg/smtp-credentials.html
Requires only the Python 3 standard library.
"""

import base64
import getpass
import hashlib
import hmac
import sys


def smtp_password(secret_access_key, region):
    """Apply AWS's version-4 SES SMTP password derivation."""
    key = ("AWS4" + secret_access_key).encode("utf-8")
    for message in ("11111111", region, "ses", "aws4_request", "SendRawEmail"):
        key = hmac.new(key, message.encode("utf-8"), hashlib.sha256).digest()
    return base64.b64encode(b"\x04" + key).decode("ascii")


def main():
    status = 0
    try:
        access_key_id = input("AWS access key ID: ").strip()
        secret_access_key = getpass.getpass("AWS secret access key (not an SMTP password): ").strip()
        region = input("AWS region of your SES SMTP endpoint (e.g. eu-central-1): ").strip()
        if not access_key_id or not secret_access_key or not region:
            raise ValueError("Access key ID, secret access key, and region are required.")
        print(f"Region: {region}")
        print(f"SMTP username: {access_key_id}")
        print(f"SMTP password: {smtp_password(secret_access_key, region)}")
    except (EOFError, KeyboardInterrupt):
        print("\nCancelled.", file=sys.stderr)
        status = 1
    except ValueError as error:
        print(f"Error: {error}", file=sys.stderr)
        status = 1
    return status


if __name__ == "__main__":
    sys.exit(main())
