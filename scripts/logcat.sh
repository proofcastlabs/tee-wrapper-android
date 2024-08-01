#!/bin/bash
function logcat {
  pkg="multiprooflabs.tee"
  device="$1"
  if [[ -n "$device" ]]; then device="-s $device"; fi

  shift 1
  if [ -z "$pkg" ]; then
    >&2 echo 'Usage: logcat pkg ...'
    return 1
  fi

  uid="$(adb $device shell pm list package -U $pkg | sed 's/.*uid://')"
  if [ -z "$uid" ]; then
    >&2 echo "pkg '$pkg' not found"
    return 1
  fi

  adb $device logcat --uid="$uid" -v color "$@"
}

logcat "$@"