#!/bin/bash

# Append mail-related environment variables to configuration/mail.properties to override defaults
echo "mail.enabled = true" >>configuration/mail.properties
if [ -n "$MAIL_FROM" ]; then
  echo "mail.from = $MAIL_FROM" >>configuration/mail.properties
fi
if [ -n "$MAIL_SMTP_HOST" ]; then
  echo "mail.smtp.host = $MAIL_SMTP_HOST" >>configuration/mail.properties
fi
if [ -n "$MAIL_SMTP_PORT" ]; then
  echo "mail.smtp.port = $MAIL_SMTP_PORT" >>configuration/mail.properties
fi
if [ -n "$MAIL_SMTP_AUTH" ]; then
  echo "mail.smtp.auth = $MAIL_SMTP_AUTH" >>configuration/mail.properties
fi
if [ -n "$MAIL_SMTP_USER" ]; then
  echo "mail.smtp.user = $MAIL_SMTP_USER" >>configuration/mail.properties
fi
if [ -n "$MAIL_SMTP_PASSWORD" ]; then
  echo "mail.smtp.password = $MAIL_SMTP_PASSWORD" >>configuration/mail.properties
fi
