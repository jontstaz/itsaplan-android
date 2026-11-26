#!/usr/bin/env bash
# Verified screenshot capture: only writes the PNG when the on-screen context
# matches the expected marker for that screen. Re-runs are idempotent per mode.
set -uo pipefail
S="${1:?session id required}"
MODE="${2:?mode label required}"
OUT="$(dirname "$0")"
AG="android-emu-agent"
PKG="io.github.jontstaz.itsaplan"

ensure_app() {
  adb -s emulator-5554 shell "dumpsys window | grep mCurrentFocus" | grep -q "$PKG" || {
    $AG app launch "$S" "$PKG" >/dev/null 2>&1; sleep 1; }
  $AG wait idle "$S" --timeout-ms 5000 >/dev/null 2>&1
}
snap() { $AG ui snapshot "$S" --format text 2>/dev/null; }
find_ref() { snap | grep -m1 "$1" | sed -E 's/.*(\^g[0-9]+a[0-9]+).*/\1/'; }
tapref() { local r; r=$(find_ref "$1"); [ -n "$r" ] && $AG action tap "$S" "$r" >/dev/null 2>&1; sleep 0.5; }
back() { $AG action back "$S" >/dev/null 2>&1; sleep 0.6; $AG wait idle "$S" --timeout-ms 4000 >/dev/null 2>&1; }

# capture <file> <context-marker-regex>
capture() {
  local name="$1" marker="$2" tries=0
  while [ $tries -lt 3 ]; do
    ensure_app
    if snap | grep -qE "$marker"; then
      $AG ui screenshot "$S" --pull --output "$OUT/$name.png" >/dev/null 2>&1 \
        && echo "  captured $name.png" || echo "  FAILED $name"
      return
    fi
    tries=$((tries+1)); sleep 1
  done
  echo "  SKIP $name (context '$marker' not found)"
}

ensure_app
tapref "clickable 'Board'"; capture "board-${MODE}" "scrollable 'backlog'|clickable 'TEST'"

tapref "clickable 'TEST-2' bounds=\[5"
capture "issue-detail-${MODE}" "'TEST-2'|Edit title and description"

if snap | grep -q "Write a comment"; then
  tapref "textfield 'Write a comment"
  capture "issue-comment-keyboard-${MODE}" "Write a comment"
  back
fi
back; capture "board-back-${MODE}" "scrollable 'backlog'" >/dev/null

tapref "Search all projects"; capture "global-search-${MODE}" "Search all projects|Type at least 2"
back

tapref "clickable 'TEST'"; capture "project-switcher-${MODE}" "Switch Project"
back

tapref "Filter issues"; capture "filter-sheet-${MODE}" "Filter Board"
back

tapref "Create new issue"; capture "create-issue-${MODE}" "New Issue in|Create Issue"
back

capture "tab-my-${MODE}" "No issue|My" && true
tapref "clickable 'My'"; capture "tab-my-${MODE}" "My Issues|Assigned|Delegated|assigned to you"
tapref "clickable 'Inbox'"; capture "tab-inbox-${MODE}" "Inbox|Unread Only"
tapref "clickable 'Cycles'"; capture "tab-cycles-${MODE}" "Cycles|No cycles"
tapref "clickable 'Docs'"; capture "tab-docs-${MODE}" "Docs|No documents"
tapref "clickable 'Settings'"; capture "tab-settings-${MODE}" "Settings|Theme|Sign out"
tapref "clickable 'Board'"
echo "done: ${MODE}"
