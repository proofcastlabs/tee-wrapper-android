#!/bin/bash

usage() {
  echo './run.sh <method> [...params]'
}

url=${RPC_URL:-'http://127.0.0.1:3030/v1/rpc'}

if [[ "$#" -eq 0 ]]; then
  echo 'Invalid args'
  usage
  exit 1
fi

method=$1

shift 1

params=$(sed 's/ /","/g' <<< $@)
params="\"$params\""

echo "$params"
curl \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":666,"method":"'$method'","params":['$params']}' \
  "$url"