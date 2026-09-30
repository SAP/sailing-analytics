#!/usr/bin/env bash
set -euo pipefail

FROM_ADDR="SAP Sailing Analytics <support@sapsailing.com>"
SUBJECT="SAP Sailing Analytics — Open Source Transition Update"
SMTP_URL="smtps://smtp.mail.eu-west-1.awsapps.com:465"

usage() {
    cat <<'EOF'
Usage: sendEmails.sh [--dry-run] <recipients.csv> [template.html]

  recipients.csv   CSV file with columns: email,name
  template.html    HTML template with {{NAME}} placeholder
                   (default: /tmp/emailTemplate.html)
  --dry-run        Print personalized emails to stdout without sending

Environment variables:
  SMTP_USER        SMTP username (required unless --dry-run)
  SMTP_PASS        SMTP password (required unless --dry-run)

CSV format example:
  email,name
  alice@example.com,Alice
  bob@example.com,Bob
EOF
    exit 1
}

DRY_RUN=false
if [[ "${1:-}" == "--dry-run" ]]; then
    DRY_RUN=true
    shift
fi

CSV="${1:-}"
TEMPLATE="${2:-/tmp/emailTemplate.html}"

[[ -z "$CSV" ]] && usage
[[ ! -f "$CSV" ]] && { echo "Error: CSV file not found: $CSV"; exit 1; }
[[ ! -f "$TEMPLATE" ]] && { echo "Error: Template file not found: $TEMPLATE"; exit 1; }

if [[ "$DRY_RUN" == false ]]; then
    [[ -z "${SMTP_USER:-}" ]] && { echo "Error: SMTP_USER not set"; exit 1; }
    [[ -z "${SMTP_PASS:-}" ]] && { echo "Error: SMTP_PASS not set"; exit 1; }
fi

TEMPLATE_BODY=$(cat "$TEMPLATE")

SENT=0
FAILED=0
SKIPPED_HEADER=false

while IFS= read -r line || [[ -n "$line" ]]; do
    # Skip empty lines
    [[ -z "${line// }" ]] && continue

    # Skip header row
    if [[ "$SKIPPED_HEADER" == false ]]; then
        SKIPPED_HEADER=true
        if echo "$line" | grep -qi "^email"; then
            continue
        fi
    fi

    EMAIL=$(echo "$line" | cut -d',' -f1 | xargs)
    NAME=$(echo "$line" | cut -d',' -f2- | xargs)

    [[ -z "$EMAIL" ]] && continue

    BODY="${TEMPLATE_BODY//\{\{NAME\}\}/$NAME}"

    if [[ "$DRY_RUN" == true ]]; then
        echo "========================================"
        echo "To:      $EMAIL"
        echo "From:    $FROM_ADDR"
        echo "Subject: $SUBJECT"
        echo "----------------------------------------"
        echo "$BODY"
        echo ""
        SENT=$((SENT + 1))
        continue
    fi

    MIME_MSG=$(cat <<MIME
From: $FROM_ADDR
To: $EMAIL
Subject: =?UTF-8?B?$(echo -n "$SUBJECT" | base64 -w0)?=
MIME-Version: 1.0
Content-Type: text/html; charset=UTF-8
Content-Transfer-Encoding: base64

$(echo -n "$BODY" | base64 -w76)
MIME
    )

    RETRIES=0
    MAX_RETRIES=3
    SEND_OK=false
    while [[ $RETRIES -le $MAX_RETRIES ]]; do
        if echo "$MIME_MSG" | curl --silent --show-error \
            --url "$SMTP_URL" \
            --ssl-reqd \
            --mail-from "support@sapsailing.com" \
            --mail-rcpt "$EMAIL" \
            --user "${SMTP_USER}:${SMTP_PASS}" \
            --upload-file - 2>/tmp/curl_err.txt; then
            echo "Sent to $EMAIL ($NAME)"
            SENT=$((SENT + 1))
            SEND_OK=true
            break
        else
            if grep -q "421" /tmp/curl_err.txt 2>/dev/null; then
                RETRIES=$((RETRIES + 1))
                WAIT=$((RETRIES * 5))
                echo "  Rate limited, retrying in ${WAIT}s... (attempt $((RETRIES))/$MAX_RETRIES)" >&2
                sleep "$WAIT"
            else
                break
            fi
        fi
    done
    if [[ "$SEND_OK" == false ]]; then
        echo "FAILED: $EMAIL ($NAME)" >&2
        FAILED=$((FAILED + 1))
    fi

    sleep 3
done < "$CSV"

echo ""
if [[ "$DRY_RUN" == true ]]; then
    echo "Dry run complete. $SENT emails previewed."
else
    echo "Done. Sent: $SENT, Failed: $FAILED"
fi
