#!/usr/bin/env bash
# Helpers for driving dress-up on the local emulator from a shell (used by agents for manual UI tests).
#
# Usage: source scripts/emulator-ui.sh
#   open_app                  start dress-up and dismiss emulator "isn't responding" dialogs
#   texts                     print all visible texts
#   tap_text "regex"          tap the first element whose text/content-description matches
#   shot name                 screenshot to $SHOTS/name.png
#
# Always targets the emulator, never a connected phone (AGENTS.md: the phone only on the user's request).
E="${DRESSUP_EMULATOR:-emulator-5554}"
SHOTS="${SHOTS:-/tmp/dress-up-shots}"
mkdir -p "$SHOTS"
dump() { adb -s $E shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb -s $E shell cat /sdcard/ui.xml; }
# center of the first node whose text or content-desc matches (regex, exact match)
find_xy() {
  dump | python3 -c "
import sys,re
xml=sys.stdin.read()
try: pat=re.compile(sys.argv[1])
except re.error: pat=re.compile(re.escape(sys.argv[1]))
for m in re.finditer(r'<node [^>]*>', xml):
    n=m.group(0)
    t=(re.search(r' text=\"([^\"]*)\"',n) or re.match('()','')).group(1)
    d=(re.search(r'content-desc=\"([^\"]*)\"',n) or re.match('()','')).group(1)
    if sys.argv[1] in (t, d) or pat.fullmatch(t) or pat.fullmatch(d):  # literal text first, then regex
        x1,y1,x2,y2=map(int,re.findall(r'\d+',re.search(r'bounds=\"([^\"]*)\"',n).group(1)))
        print((x1+x2)//2,(y1+y2)//2); break
" "$1"
}
tap_text() { local xy; xy=$(find_xy "$1"); if [ -z "$xy" ]; then echo "NOT FOUND: $1"; return 1; fi; adb -s $E shell input tap $xy; sleep ${2:-2}; }
texts() { dump | grep -oE ' text="[^"]+"' | sed 's/ text=//' | tr '\n' ' '; echo; }
shot() { adb -s $E exec-out screencap -p > $SHOTS/$1.png; }
# Dismiss "X isn't responding" dialogs of the slow emulator (system UI/launcher, not dress-up).
calm() { for i in 1 2 3 4 5; do if dump | grep -q "isn't responding"; then tap_text "Wait" 2 >/dev/null; else return 0; fi; done; }
open_app() { adb -s $E shell am start -n com.dressup.app.debug/com.dressup.app.MainActivity >/dev/null 2>&1; sleep 3; calm; }
