# sendEmails.sh — Personalized Email Sender

Bash script for sending personalized HTML emails via SMTP (AWS WorkMail).

## Prerequisites

- `curl` with SMTP/TLS support
- `base64` (coreutils)
- SMTP credentials for the WorkMail account (`support@sapsailing.com`)

## Files

| File | Purpose |
|---|---|
| `sendEmails.sh` | The sending script |
| `emailTemplate.html` | HTML email template with `{{NAME}}` placeholder |
| `recipients.csv` | CSV file with recipient list |

## CSV Format

```csv
email,name
alice@example.com,Alice
bob@example.com,Bob
sailor42@example.com,sailor42
```

- **email** — recipient address
- **name** — inserted into the salutation ("Dear {{NAME}},"). Use the user's first name if known, otherwise their username/nickname.
- The header row is auto-detected and skipped if the first field starts with `email` (case-insensitive).

## Usage

```bash
./sendEmails.sh [--dry-run] <recipients.csv> [template.html]
```

| Argument | Required | Default | Description |
|---|---|---|---|
| `--dry-run` | no | — | Preview personalized emails without sending |
| `recipients.csv` | yes | — | Path to the CSV file |
| `template.html` | no | `/tmp/emailTemplate.html` | Path to the HTML template |

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `SMTP_USER` | yes (unless `--dry-run`) | SMTP username for WorkMail |
| `SMTP_PASS` | yes (unless `--dry-run`) | SMTP password for WorkMail |

## Examples

Preview all emails without sending:

```bash
./sendEmails.sh --dry-run recipients.csv
```

Send using a custom template:

```bash
export SMTP_USER="your-smtp-username"
export SMTP_PASS="your-smtp-password"
./sendEmails.sh recipients.csv myTemplate.html
```

## Configuration

These values are set at the top of the script and can be adjusted:

| Variable | Default | Description |
|---|---|---|
| `FROM_ADDR` | `SAP Sailing Analytics <support@sapsailing.com>` | Sender display name and address |
| `SUBJECT` | `Your SAP Sailing Analytics Account — Open Source Transition Update` | Email subject line |
| `SMTP_URL` | `smtps://smtp.mail.eu-west-1.awsapps.com:465` | WorkMail SMTP endpoint |

## How It Works

1. Reads the CSV line by line, skipping the header row.
2. For each recipient, replaces `{{NAME}}` in the HTML template with the name from the CSV.
3. Constructs a MIME message with UTF-8 base64-encoded subject and body.
4. Sends via `curl` over SMTPS to the WorkMail endpoint.
5. Waits 1 second between sends to respect SES rate limits.

## Recommended Workflow

1. **Preview** — Run with `--dry-run` and review the output.
2. **Test** — Send to your own address first (`echo "email,name" > test.csv && echo "you@example.com,YourName" >> test.csv`).
3. **Send** — Run against the full recipient list.
