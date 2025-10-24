#!/bin/bash
if systemctl status sshd.service 2>/dev/null >/dev/null; then
    echo "Status: 200"
    echo "Content-type: text/html"
    echo ""
    echo "SSH service is up and running"
else
    EXIT_CODE=$?
    echo "Status: 500"
    echo "Content-type: text/html"
    echo ""
    echo "SSH service is not running. Code: ${EXIT_CODE}"
fi
