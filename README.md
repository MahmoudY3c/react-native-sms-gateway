# react-native-sms-gateway

Need to listen/forward incoming SMS to your server or even a Telegram chat, even if your app is completely closed or the phone is restarted? This package is designed to do just that. It can send SMS to a local or external service/third-party API or to a Telegram chat via bot token. It covers most SMS use cases, such as receiving urgent messages (like OTPs) when you are not home, or forwarding SMS to external tools.


> **⚠️ CAUTION:**
> This package **only works on Android**. There is **no public SMS API for iOS**, so iOS is **not supported** due to platform restrictions (Apple does not provide public APIs for SMS access) and cannot receive or forward SMS using this library.


---

## Features

- **Background SMS Listener:** Receives SMS even when the app is closed or after device reboot (Android only).
- **Forward SMS to HTTP Server:** Forwards SMS to one or more HTTP endpoints with optional custom headers.
- **Forward SMS to Telegram:** Forwards SMS to Telegram chats, groups, or channels using a bot.
- **Flexible Filtering:** Filter SMS by sender or message keywords (case-insensitive, partial match).
- **Configurable Delivery:** Choose to deliver via HTTP, Telegram, or both.
- **React Native Event Emitter:** Optionally receive SMS events in your JS app for custom handling.
- **Persistent Configuration:** All settings are stored in Android SharedPreferences.
- **Dual SIM Support:** Attempts to detect SIM slot (where possible).
- **Boot Persistence:** Listens after device reboot (requires permission).

---

## Table of Contents
- [Features](#features)
- [Installation](#installation)
- [Platform Support](#platform-support)
- [Android Setup](#android-setup)
- [How it works](#how-it-works)
- [Usage](#usage)
- [API Reference](#api-reference)
  - [enableSmsListener](#enablesmslistenerenabled)
  - [setHttpConfigs](#sethttpconfigsdata)
  - [getHttpConfigs](#gethttpconfigs)
  - [setTelegramConfig](#settelegramconfigbot_token-chat_ids)
  - [setTelegramBotToken](#settelegrambottokenbot_token)
  - [setTelegramChatIds](#settelegramchatidschat_ids)
  - [getTelegramBotToken](#gettelegrambottoken)
  - [getTelegramChatIds](#gettelegramchatids)
  - [getTelegramParseMode](#gettelegramparsemode)
  - [setDeliveryType](#setdeliverytypedelivery_type)
  - [getDeliveryType](#getdeliverytype)
  - [isSmsListenerEnabled](#issmslistenerenabled)
  - [getUserPhoneNumber](#getuserphonenumber)
  - [setUserPhoneNumber](#setuserphonenumberphonenumber)
  - [getAllSettings](#getallsettings)
  - [setSendersFilterList](#setsendersfilterlistlist)
  - [getSendersFilterList](#getsendersfilterlist)
  - [setMsgKeywordsFilterList](#setmsgkeywordsfilterlistlist)
  - [getMsgKeywordsFilterList](#getmsgkeywordsfilterlist)
  - [addEventListener](#addeventlistenereventhandler)
  - [getEventListenersCount](#geteventlistenerscount)
  - [removeAllSMSEventListeners](#removeallsmseventlisteners)
- [Usage](#usage)
- [Telegram Setup](#telegram-setup)
- [Filtering](#filtering)
- [Tips & Issues](#tips--issues)
- [Future Features](#future-features)
- [Contributing](#contributing)
- [License](#license)

---

## Installation

```sh
npm install react-native-sms-gateway
# or
yarn add react-native-sms-gateway
```

---

## Platform Support

- **Android:** Fully supported.
- **iOS:** Not supported (Apple does not provide public APIs for SMS access).

---

## Android Setup

You must manually add the following permissions and receivers to your app's `AndroidManifest.xml` for the package to work correctly.

**Add these permissions at the top of your manifest (inside `<manifest>` like next):**

```xml
<manifest ...>
  ...
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.READ_SMS" />
  <uses-permission android:name="android.permission.RECEIVE_SMS" />
  <uses-permission android:name="android.permission.WAKE_LOCK" />
  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
  ...
</manifest>
```

**Add these receivers inside your `<application>` tag: like next**

```xml
 <application ....>
    ......
    <!-- SMS Receiver: required for receiving SMS in background -->
    <receiver 
      android:name="com.smsgateway.SmsGatewayReceiver"
      android:enabled="true" 
      android:exported="true"
      android:permission="android.permission.BROADCAST_SMS">
      <intent-filter android:priority="999">
        <action android:name="android.provider.Telephony.SMS_RECEIVED" />
      </intent-filter>
    </receiver>

    <!-- Boot Receiver: required for listening after device reboot -->
    <receiver 
      android:name="com.smsgateway.SmsGatewayBootReceiver" 
      android:enabled="true"
      android:exported="false">
      <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
      </intent-filter>
    </receiver>
    ....
  </application>
```

**Note:**
- These entries are required for the package to receive SMS in the background and after device reboot.
- You must also request SMS permissions at runtime
---

## How it works

- **Native Side (Android/Kotlin):**
  - [`SmsReceiver`](android/src/main/java/com/smsgateway/SmsReceiver.kt) listens for incoming SMS and applies sender/message filters.
  - If a match is found, it builds a payload and dispatches it to:
    - HTTP endpoints via [`HttpHelper`](android/src/main/java/com/smsgateway/HttpHelper.kt)
    - Telegram via [`TelegramHelper`](android/src/main/java/com/smsgateway/TelegramHelper.kt)
    - JS event emitter (if enabled)
  - All configuration is managed via [`ConfigProvider`](android/src/main/java/com/smsgateway/ConfigProvider.kt) and [`SmsNativeModule`](android/src/main/java/com/smsgateway/SmsNativeModule.kt).

- **JS Side (React Native):**
  - Use the [`SmsGateway`](index.ts) class to configure, enable/disable, and listen for SMS events.
  - All settings are persisted natively and survive app restarts.

---

---

## Usage

### Basic Setup

```ts
import { SmsGateway } from "react-native-sms-gateway";

// Enable SMS listener (required)
SmsGateway.enableSmsListener(true);

// Set HTTP endpoints (optional)
SmsGateway.setHttpConfigs([
  { url: "https://your-server.com/sms", headers: { Authorization: "Bearer TOKEN" } }
]);

// Set Telegram config (optional)
SmsGateway.setTelegramConfig(
  "YOUR_TELEGRAM_BOT_TOKEN",
  ["123456789", "-100987654321"] // chat IDs (user, group, or channel)
);

// Set delivery type: "http", "telegram", or "all"
SmsGateway.setDeliveryType("all");

// Set sender filter (optional)
SmsGateway.setSendersFilterList(["Vodafone", "010"]);

// Set message keyword filter (optional)
SmsGateway.setMsgKeywordsFilterList(["OTP", "gift"]);

// Set user phone number (optional, for forwarding)
SmsGateway.setUserPhoneNumber("+201234567890");
```

### Listen for SMS in JS

```ts
const subscription = SmsGateway.addEventListener((data) => {
  // data: { msg, timestamp, phoneNumber, sender }
  console.log("Received SMS:", data);
});

// Remove listener when done
subscription.remove();
```

### Get All Settings

```ts
const settings = await SmsGateway.getAllSettings();
console.log(settings);
```

## API Reference

### enableSmsListener(enabled)
Enable or disable the SMS listener (background service).
```ts
SmsGateway.enableSmsListener(true); // Enable
SmsGateway.enableSmsListener(false); // Disable
```

### setHttpConfigs(data)
Set HTTP endpoints and optional headers for forwarding SMS.
```ts
SmsGateway.setHttpConfigs([
  { url: "https://your-server.com/sms", headers: { Authorization: "Bearer TOKEN" } }
]);
```

### getHttpConfigs()
Get the current HTTP configuration.
```ts
const configs = await SmsGateway.getHttpConfigs();
```

### setTelegramConfig(bot_token, chat_ids)
Set Telegram bot token and chat IDs at once.
```ts
SmsGateway.setTelegramConfig("YOUR_BOT_TOKEN", ["YOUR_CHAT_ID"]);
```

### setTelegramBotToken(bot_token)
Set only the Telegram bot token.
```ts
SmsGateway.setTelegramBotToken("YOUR_BOT_TOKEN");
```

### setTelegramChatIds(chat_ids)
Set only the Telegram chat IDs.
```ts
SmsGateway.setTelegramChatIds(["123456789", "-100987654321"]);
```

### getTelegramBotToken()
Get the current Telegram bot token.
```ts
const token = await SmsGateway.getTelegramBotToken();
```

### getTelegramChatIds()
Get the current Telegram chat IDs.
```ts
const chatIds = await SmsGateway.getTelegramChatIds();
```

### getTelegramParseMode()
Get the Telegram parse mode (currently always 'HTML').
```ts
const mode = await SmsGateway.getTelegramParseMode();
```

### setDeliveryType(delivery_type)
Set delivery type: 'http', 'telegram', or 'all'.
```ts
SmsGateway.setDeliveryType("all");
```

### getDeliveryType()
Get the current delivery type.
```ts
const type = await SmsGateway.getDeliveryType();
```

### isSmsListenerEnabled()
Check if the SMS listener is enabled.
```ts
const enabled = await SmsGateway.isSmsListenerEnabled();
```

### getUserPhoneNumber()
Get the saved user phone number.
```ts
const phone = await SmsGateway.getUserPhoneNumber();
```

### setUserPhoneNumber(phoneNumber)
Set the user phone number for forwarding.
```ts
SmsGateway.setUserPhoneNumber("+201234567890");
```

### getAllSettings()
Get all current settings as an object.
```ts
const settings = await SmsGateway.getAllSettings();
```

### setSendersFilterList(list)
Set sender filter list (array of strings).
```ts
SmsGateway.setSendersFilterList(["Vodafone", "010"]);
```

### getSendersFilterList()
Get the current sender filter list.
```ts
const senders = await SmsGateway.getSendersFilterList();
```

### setMsgKeywordsFilterList(list)
Set message keywords filter list (array of strings).
```ts
SmsGateway.setMsgKeywordsFilterList(["OTP", "gift"]);
```

### getMsgKeywordsFilterList()
Get the current message keywords filter list.
```ts
const keywords = await SmsGateway.getMsgKeywordsFilterList();
```

### addEventListener(eventHandler)
Add a JS event listener for incoming SMS (works only when app is running).
```ts
const subscription = SmsGateway.addEventListener((data) => {
  // data: { msg, timestamp, phoneNumber, sender }
  console.log("Received SMS:", data);
});
// Remove listener when done
subscription.remove();
```

### getEventListenersCount()
Get the number of SMS event listeners currently added.
```ts
const count = await SmsGateway.getEventListenersCount();
```

### removeAllSMSEventListeners()
Remove all SMS event listeners.
```ts
SmsGateway.removeAllSMSEventListeners();
```

---

## Usage

### Basic Setup

```ts
import { SmsGateway } from "react-native-sms-gateway";

// Enable SMS listener (required)
SmsGateway.enableSmsListener(true);

// Set HTTP endpoints (optional)
SmsGateway.setHttpConfigs([
  { url: "https://your-server.com/sms", headers: { Authorization: "Bearer TOKEN" } }
]);

// Set Telegram config (optional)
SmsGateway.setTelegramConfig(
  "YOUR_TELEGRAM_BOT_TOKEN",
  ["123456789", "-100987654321"] // chat IDs (user, group, or channel)
);

// Set delivery type: "http", "telegram", or "all"
SmsGateway.setDeliveryType("all");

// Set sender filter (optional)
SmsGateway.setSendersFilterList(["Vodafone", "010"]);

// Set message keyword filter (optional)
SmsGateway.setMsgKeywordsFilterList(["OTP", "gift"]);

// Set user phone number (optional, for forwarding)
SmsGateway.setUserPhoneNumber("+201234567890");
```

### Listen for SMS in JS

```ts
const subscription = SmsGateway.addEventListener((data) => {
  // data: { msg, timestamp, phoneNumber, sender }
  console.log("Received SMS:", data);
});

// Remove listener when done
subscription.remove();
```

### Get All Settings

```ts
const settings = await SmsGateway.getAllSettings();
console.log(settings);
```

---

## Telegram Setup

1. **Create a Telegram Bot:**
   - Search for [@BotFather](https://t.me/BotFather) in Telegram.
   - Use `/newbot` to create a bot and get the bot token.

2. **Get Your Chat ID:**
   - Start a chat with your bot or add it to a group/channel.
   - Use [@userinfobot](https://t.me/userinfobot) or similar to get your user/group/channel ID.
   - For channels, you must make your bot an admin.

3. **Configure in JS:**
   ```ts
   SmsGateway.setTelegramConfig("YOUR_BOT_TOKEN", ["YOUR_CHAT_ID"]);
   ```

4. **Parse Mode:**
   - The package uses HTML parse mode for formatting. You cannot change this currently.

---

## Filtering

- **Sender Filter:** Only SMS from senders containing any of the specified strings (case-insensitive) will be forwarded.
- **Message Keyword Filter:** Only SMS containing any of the specified keywords (case-insensitive) will be forwarded.
- **If both filters are empty, all SMS are forwarded. If both are set, a message is forwarded if it matches either filter.**

---

## Tips & Issues

- **Permissions:** You must request SMS permissions at runtime on Android 6.0+.
- **Dual SIM:** SIM slot detection is best-effort and may not work on all devices.
- **Background/Boot:** The module works in the background and after reboot, but some OEMs may restrict background receivers.
- **Telegram:** Your bot must be an admin in groups/channels to send messages.
- **HTTP:** You can set multiple endpoints and custom headers for each.
- **JS Listener:** Works only when the app is running; use HTTP/Telegram for background delivery.

---

## Future Features

- **Custom notification support**
- **More flexible message templates**
  - Allow users to define custom Telegram message templates
- **SMS Backup / Logging**
  - Optional local database or file log for all received messages
  - Useful for debugging or offline sync
- **Webhook verification and retry logic**
  - Retry sending if HTTP request or Telegram fails
  - Optionally queue messages until internet is available
- **Rate Limiting / Throttling**
  - Prevent sending too many requests per second (e.g., for spam protection)
- **Use DataStore instead of SharedPreferences**
  - Migrate to Jetpack DataStore for improved reliability and performance

---

## Contributing

Contributions are welcome! Please open issues or pull requests for bugs, features, or documentation improvements.

---

## License

MIT

