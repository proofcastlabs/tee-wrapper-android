#!/bin/bash
function usage {
	local b # bold
	local n # normal
	b=$(tput bold)
	n=$(tput sgr0)

	echo "${b}Usage:${n} $0 [DEVICE_ID] [...ADB_ARGS]

  Launch the MainActivity of the Event Attestator on the
  selected DEVICE. You may optionally decide to extra adb
  parameters, these are going to be appended at the end of
  the adb command.

${b}Arguments:${n}

  DEVICE_ID              The device ID show through adb devices command. An empty
                         value is accepted only when a single device is attached
                         to the machine.

  ADB_ARGS               Parameters accepted by the adb activity manager (am)
                         command (i.e. intent extras like --es <key> <value>)
                         Check the acceptable intent extra keys into the MainActivity
                         code.

${b}Available options:${n}

  -h, --help             Shows this help

${b}Examples:${n}

  1. Launch with websocket host and port settings:

    $0 --es wsHost localhost --es wsPort 11111

  2. Launch on a specific device (8BKX1BBEE)

    $0 8BKX1BBEE
"
}

function main {
  if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    usage
    exit 0
  fi

  if [[ ! "$1" == -* && -n "$1" ]]; then
    device="-s $1"
    shift 1
  fi

  # You can add params to the launching command (i.e. intents)
  # ...such coolness :D
  adb $device shell am start -n proofcastlabs.tee/.MainActivity -a android.intent.action.MAIN $@
}

main "$@"
