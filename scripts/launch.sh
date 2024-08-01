#!/bin/bash
device="$1"
if [[ -n "$device" ]]; then device="-s $device"; fi
adb $device shell am start multiprooflabs.tee/.MainActivity