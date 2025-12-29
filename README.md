# Blessr: Bluetooth LE Speed Sensor Reader

A CLI tool to read speed and distance data from Bluetooth Low Energy (BLE) speed sensors.

Data is printed to the console and also sent to the Fitbit backend using the Fitbit API.

This has been tested and known to work well with [this device](https://www.amazon.fr/dp/B085VTC3YQ?ref=ppx_yo2ov_dt_b_fed_asin_title&th=1),
although it should work with any BLE speed sensor that follows the standard BLE Speed and Cadence Service.

## Usage

Download [the latest release](https://github.com/BoD/blessr/releases/download/v1.0.0/cliApp-1.0.0-jvm.zip) and unzip it.

Run the CLI tool with the following command:

```bash
bin/cliApp --fitbit-client-id=YOUR_CLIENT_ID --device-name=YOUR_DEVICE_NAME
```

## How to get a Fitbit Client ID

Follow [the guide](https://dev.fitbit.com/build/reference/web-api/developer-guide/getting-started/#Creating-a-Developer-Account-for-Fitbit).
