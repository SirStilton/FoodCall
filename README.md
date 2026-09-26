# FoodCall 🍽️

**FoodCall** is a free and open-source, vibe-coded tool for sending food and meal notifications between devices.

This project was built mostly for fun and personal use. The source code is freely available so you can modify it, improve it, break it, rebuild it, or turn it into something completely different.

## What does it do?

FoodCall consists of two main parts:

- **FoodCall / Caller** – Used to send food alerts, for example when breakfast, lunch, dinner or a snack is ready.
- **FoodCall Client** – Receives FoodCall alerts and displays them prominently on an Android device.
- The Caller can send alerts to registered FoodCall clients using Firebase Cloud Messaging (FCM).
- FoodCall includes web-based Caller and Client interfaces.
- The Android client can display an alarm overlay, play an alarm sound and vibrate.
- Different meal types can be displayed with different symbols.
- The Caller can be used as a dedicated Android kiosk device.

FoodCall is a hobby project, not a polished commercial product. Things may be unfinished, weird, overengineered or held together by pure vibe coding. :)

## Admin password

The default admin password is:

```text
admi546
```

**This password can and should be changed.**

Because this repository is public, `admi546` must be considered a publicly known default password. Do not rely on it to protect anything sensitive or publicly accessible.

# ⚠️ Important warnings

## Kiosk mode

The FoodCall Caller includes **kiosk functionality** intended for dedicated devices.

Kiosk mode may restrict access to normal Android controls, settings, navigation, notifications or other parts of the device.

**Do not enable kiosk functionality on an important or everyday device unless you understand what it does and know how to disable it again.**

Using a spare device for testing is strongly recommended.

## 🔊 Loud notifications

FoodCall Client may use **very loud notification/alarm sounds** so that a food alert isn't missed.

**Check your volume before testing.**

Do not test loud alerts while wearing headphones or with the device close to your ears. Alerts may be significantly louder than expected depending on the device and Android configuration.

# Development & ADB testing

ADB can be used to test both Android applications locally.

Replace `<SERIAL>` in the commands below with the device identifier returned by:

```powershell
adb devices
```

For example:

```text
0123456789ABCDEF
```

# FoodCall Client

## Start the Client

```powershell
adb -s <SERIAL> shell am start -n com.cyberfox.foodcaller.client/.MainActivity
```

## Trigger a local food alert

This example triggers a **SNACK** alert, including the snack symbol:

```powershell
adb -s <SERIAL> shell am broadcast `
  -n com.cyberfox.foodcaller.client/.FoodAlertReceiver `
  -a com.cyberfox.foodcaller.client.FOOD_READY `
  --es alarmId test-snack `
  --es meal SNACK
```

Other available meal types:

```text
--es meal BREAKFAST
--es meal LUNCH
--es meal DINNER
--es meal SNACK
```

## Stop the current alert

Immediately stops the alarm, sound, vibration, notification and overlay:

```powershell
adb -s <SERIAL> shell am broadcast `
  -n com.cyberfox.foodcaller.client/.FoodAlertReceiver `
  -a com.cyberfox.foodcaller.client.FOOD_CLEAR
```

## Test the unlock reminder

Triggers the two-minute unlock reminder immediately:

```powershell
adb -s <SERIAL> shell am broadcast `
  -n com.cyberfox.foodcaller.client/.FoodAlertReceiver `
  -a com.cyberfox.foodcaller.client.FOOD_REMINDER
```

## Check whether the alarm overlay is running

```powershell
adb -s <SERIAL> shell dumpsys activity services com.cyberfox.foodcaller.client
```

## View FoodCall Client logs

```powershell
adb -s <SERIAL> logcat -d | Select-String "foodcaller|FirebaseMessaging|FoodOverlay|FoodCall"
```

## Reinstall without deleting the saved family PIN

```powershell
adb -s <SERIAL> install -r `
  "C:\Users\Csanarator\AndroidStudioProjects\FodCallClient\app\build\outputs\apk\debug\app-debug.apk"
```

The path above is an example development path. Change it to wherever your `app-debug.apk` is located.

---

# FoodCall Caller / Kiosk

For the Caller/Kiosk application, **always check the connected device first**:

```powershell
adb devices
```

## Manually open FoodCall

```powershell
adb -s <SERIAL> shell monkey -p com.cyberfox.foodcall 1
```

## Find the package name

If your build uses a different package name:

```powershell
adb -s <SERIAL> shell pm list packages | Select-String "food"
```

## Exit kiosk mode / return to Android

Open the normal Android home interface:

```powershell
adb -s <SERIAL> shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
```

If FoodCall immediately brings itself back to the foreground, temporarily force-stop it:

```powershell
adb -s <SERIAL> shell am force-stop com.cyberfox.foodcall
```

Then open the Android home screen again:

```powershell
adb -s <SERIAL> shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
```

## Start the kiosk again

```powershell
adb -s <SERIAL> shell monkey -p com.cyberfox.foodcall 1
```

## View Caller logs

Clear old Logcat output:

```powershell
adb -s <SERIAL> logcat -c
```

Then watch FoodCall, Chromium and WebView messages:

```powershell
adb -s <SERIAL> logcat | Select-String "FoodCall|chromium|WebView"
```

## Reload the WebView / Caller page

The simplest way to completely reload the current Caller page is to stop and restart the application:

```powershell
adb -s <SERIAL> shell am force-stop com.cyberfox.foodcall
adb -s <SERIAL> shell monkey -p com.cyberfox.foodcall 1
```

## Note about Device Owner mode

The current FoodCall kiosk does **not** require Android Device Owner mode.

Therefore, `force-stop` followed by the Android Home command is normally enough to leave the FoodCall kiosk.

A kiosk application configured as an actual Android **Device Owner** may intentionally impose stronger restrictions and require its configured management/unprovisioning procedure instead.

---

# Normal FoodCall operation

**ADB is not required for normal FoodCall use.**

For the real FoodCall flow, open the Caller interface and press **ESSEN RUFEN**.

The Caller sends the FCM alert to all registered FoodCall clients. The clients then handle and display the alert automatically.

The ADB commands above are intended only for development, debugging and local testing.

# Open source

FoodCall is intended to be freely available for learning, experimenting, modifying and building your own versions.

Fork it. Change it. Add features. Remove features. Make your own FoodCall.

**Use it at your own risk.**
