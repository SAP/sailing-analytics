#!/bin/bash

# Description:
# This script finds all *.prefs and *.launch files tracked by Git
# and sets them to "assume-unchanged", so local changes won't show
# up in 'git status' or be accidentally committed.

# Find and apply the assume-unchanged flag
echo "Scanning for tracked *.prefs and *.launch files..."

files=$(git ls-files | grep -E '\.prefs$|\.launch$')

if [ -z "$files" ]; then
  echo "No .prefs or .launch files found in the tracked file list."
  exit 0
fi

echo "$files" | while read -r file; do
  git update-index --assume-unchanged "$file"
  echo "Marked as assume-unchanged: $file"
done

echo ""
echo "Git will now ignore local changes to these files (but they will still update from upstream if you clear this flag before pulling)."

