# Android TEE wrapper

## Introduction

Given the low level nature of Trusted Execution Environments, an abstraction layer is needed in
order to facilitate the interaction for the end user. This project aims to create this layer by
creating a websocket connection to the host to whom the device is connected and will interpret
all the commands sent by the host as low levels calls.


## Architecture

The architecture consists into different layers:

 - App android: where the websocket client is started continously polling for the host connection
 - Core: the actual rust code which is being wrapped interacting with the underlying TEE. This is dynamically
   loaded when the Android app starts and where the commands are actually executed. Once the result is available
   it get serialized again and returned to the apps and forwarded to the host through the websocket connection.

## Setup

### Requirements

 - JAVA SDK 11 installed
 - Android SDK installed (v33)
 - Android NDK installed (v25)

### Optional

 - Android Studio

### Steps

1. Link the Rust library project into the rust folder (be sure it compiles when running `cargo build --release`):
2. Create a `local.properties` file into the project's root folder with the path to the android SDK and NDK 
additional properties as follows:

```env
sdk.dir=/opt/android-sdk
ndk.dir=/opt/android-sdk/ndk/25.2.9519653

rust.rustcCommand=/home/mauro/.cargo/bin/rustc
rust.cargoCommand=/home/mauro/.cargo/bin/cargo

ext.deviceId=<device-id>
ext.attestatorPath=/home/mauro/w/event-attestator
```

3. Plug the device and run

```bash
./scripts/setup-plugdev-perm.sh
```

4. Build and install the app

```bash
./gradlew clean installDebug
```


### Production ready

In order to setup a production-ready instance, you need:

1. Clone https://github.com/proofcastlabs/event-attestator/
2. Clone https://github.com/proofcastlabs/tee-wrapper-android
3. Follow the setup guide in the `event-attestator` repo and run the jsonrpc-app 
artifact with

```
./jsonrpc-app -c config.toml
```

Example `config.toml` file:

```toml
[log]
path = "./app.log"
level = "debug"
enabled = true
max_log_size = 1_000_000_000
max_num_logs = 1
use_file_logging = false

[core]
timeout = 5

[governance]
network_id = "eth"
address = "0x0000000000000000000000000000000000000000"

[networks.eth]
pnetwork_hub = "0x0000000000000000000000000000000000000000"
endpoints = [ "wss://bibidi-bobidi-boo.quiknode.pro:443/123123123123"]
sleep_duration = 10
network_id = "ethereum"
validate = false
gas_limit = 0
batch_size = 2
batch_duration = 0
pre_filter_receipts = false
# Add the event you want to listen to here
events = [
  ["0xdAC17F958D2ee523a2206206994597C13D831ec7", "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"],
]

[mongo]
enabled = true
uri_str = "mongodb://localhost:27017/"
database = "sentinel"
collection = "signed_events"
```

4. Build the android tee wrapper (you may need to do this step in a different environment)

```bash
./gradlew clean assembleDebug
```

5. Copy the resulting artifact (`tee-wrapper-debug.apk`) in the production server and install it to 
device:

```bash
adb -s <device-id> install -r tee-wrapper-debug.apk
```

**Note:** you can get the device id with 

```bash
adb devices
```

6. Launch the app 

```bash
./gradlew launch
```

The app will try to connect to the ws server ran by `jsonrpc-app`.

7. In order to start the syncing process, you may need to initialize the chain supported in the toml
file, then run:

```bash
# ./scripts/rpc.sh <method> [...params]
./scripts/rpc.sh init false 0x393ad7Bd0B94788b3B9EB15303E3845B4828E7Fb 50 10 eth 
```

