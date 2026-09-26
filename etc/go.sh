cliApp/build/install/cliApp-jvm/bin/cliApp \
  --google-health-client-id=YOURCLIENTID \
  --google-health-client-secret=YOURCLIENTSECRET \
  --log-level=debug \
  --device-name=CYCPLUS \
  2>~/Tmp/walkingpad-fitbit-logs.txt \
  | tee ~/Tmp/walkingpad-fitbit-display.txt
