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
- **Boot Persistence:** Listens after device reboot (requires permission).

---

## Table of Contents
- [react-native-sms-gateway](#react-native-sms-gateway)
  - [Features](#features)
  - [Table of Contents](#table-of-contents)
  - [Installation](#installation)
  - [Platform Support](#platform-support)
  - [Android Setup](#android-setup)
  - [How it works](#how-it-works)
  - [Usage](#usage)
    - [Basic Setup](#basic-setup)
    - [Listen for SMS in JS](#listen-for-sms-in-js)
    - [Get All Settings](#get-all-settings)
  - [API Reference](#api-reference)
    - [enableSmsListener(enabled)](#enablesmslistenerenabled)
    - [setHttpConfigs(data)](#sethttpconfigsdata)
    - [getHttpConfigs()](#gethttpconfigs)
    - [setTelegramConfig(bot\_token, chat\_ids)](#settelegramconfigbot_token-chat_ids)
    - [setTelegramBotToken(bot\_token)](#settelegrambottokenbot_token)
    - [setTelegramChatIds(chat\_ids)](#settelegramchatidschat_ids)
    - [getTelegramBotToken()](#gettelegrambottoken)
    - [getTelegramChatIds()](#gettelegramchatids)
    - [getTelegramParseMode()](#gettelegramparsemode)
    - [setDeliveryType(delivery\_type)](#setdeliverytypedelivery_type)
    - [getDeliveryType()](#getdeliverytype)
    - [isSmsListenerEnabled()](#issmslistenerenabled)
    - [getUserPhoneNumber()](#getuserphonenumber)
    - [setUserPhoneNumber(phoneNumber)](#setuserphonenumberphonenumber)
    - [getAllSettings()](#getallsettings)
    - [setSendersFilterList(list)](#setsendersfilterlistlist)
    - [getSendersFilterList()](#getsendersfilterlist)
    - [setMsgKeywordsFilterList(list)](#setmsgkeywordsfilterlistlist)
    - [getMsgKeywordsFilterList()](#getmsgkeywordsfilterlist)
    - [addEventListener(eventHandler)](#addeventlistenereventhandler)
    - [getEventListenersCount()](#geteventlistenerscount)
    - [removeAllSMSEventListeners()](#removeallsmseventlisteners)
  - [HTTP Forwarding Details](#http-forwarding-details)
    - [Example: Node.js HTTP Receiver](#example-nodejs-http-receiver)
  - [Telegram Setup](#telegram-setup)
  - [Filtering](#filtering)
  - [Tips \& Issues](#tips--issues)
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
Each time an SMS is received, a POST request is sent to each configured URL with the SMS data as JSON.

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
Check if the SMS listener is enabled. It's not checking if you are set an SMS listener into you app but it checks if the SMS listen service running in background it helpful to indicate whether you are enabled / disabled the service via [enableSmsListener(enabled)](#enablesmslistenerenabled)
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

## HTTP Forwarding Details

When an SMS is received and forwarded to your HTTP endpoint, the package sends a POST request with the following JSON body:

```json
{
  "msg": "Message content",
  "timestamp": 1717430000000,
  "phoneNumber": "+201234567890",
  "sender": "Vodafone"
}
```

- **HTTP Method:** `POST`
- **Content-Type:** `application/json`
- **Headers:** Any custom headers you set via `setHttpConfigs`.

### Example: Node.js HTTP Receiver

Here’s a minimal Node.js server to receive and log incoming SMS webhooks:

```js
// example/http-receiver.js
const express = require('express');
const app = express();
app.use(express.json());

app.post('/sms', (req, res) => {
  console.log('Received SMS:', req.body);
  res.status(200).send('OK');
});

app.listen(3000, () => console.log('Listening on port 3000'));
```

- Start with: `node http-receiver.js`
- Set your endpoint in the package config:
  ```ts
  SmsGateway.setHttpConfigs([
    { url: "http://your-server-ip:3000/sms", headers: {} }
  ]);
  ```

---

## Telegram Setup

1. **Create a Telegram Bot:**
   - Search for [@BotFather](https://t.me/BotFather) in Telegram.
   - Use `/newbot` to create a bot and get the bot token. 
   - Enter bot name like `your_bot` then you will get the bot token copy and save it we will use it later.
   - Next stay at  [@BotFather](https://t.me/BotFather) and use `/setcommands` to set command to help get the chat id after enter `/setcommands` you will get message like `Choose a bot to change the list of commands.` so enter your bot name like in example above `@your_bot`.
   - Next you will prompted to enter commands enter the following command `get_chat_id - display current chat id` so it will be used later to get chat it now you are ready

2. **Handle Send Chat Id Via `get_chat_id` Command**
  - By default telegram provide 2 ways to but / receive message via `webhook` or `long pooling` if you plan to deploy you bot to free server like vercel you can use `webhook` check the docs to understand how to use it it's easy to do it then you can use the next examples to get started [check telegram docs for get updates](https://core.telegram.org/bots/api#getting-updates)

  - Long pooling _(recommended for test)_
    - the easiest way to get chat id specially when you are testing the pkg is using long pooling the next example install `yarn add node-telegram-bot-api` then try next 

    - `index.js`
    ```js
    const TelegramBot = require('node-telegram-bot-api');
    const TOKEN = 'YOUR_BOT_TOKEN_HERE';
    const bot = new TelegramBot(TOKEN, { polling: true });  

    bot.onText(/\/get_chat_id/, (msg) => {
      const user_id = msg.from.id;
      const sender_username = msg.from.username;
      const chat_id = msg.chat.id;  
      bot.sendMessage(
        chat_id,
        `Your id is: \`${user_id}\`\nUsername is: \`${sender_username}\`\nCurrent chat id is: \`${chat_id}\``,
        {
          parse_mode: "Markdown",
        }
      );
    }); 

    console.log('waiting for "get_chat_id" command ...');
    ```
   
  - Nodejs _(recommended for production)_
    - **Set webhook url** create any js file copy next code and update to add bot token, webhook url then run it to set webhook url
    > **⚠️ CAUTION:** since you set webhook url you couldn't use long polling 
    ``` ts
    const BOT_TOKEN = '123456789:ABCDEF_your_bot_token_here';
    const WEBHOOK_URL = 'https://your-domain.com/your-webhook-path';

    const TELEGRAM_API_URL = `https://api.telegram.org/bot${BOT_TOKEN}/setWebhook`;
    fetch(TELEGRAM_API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ url: WEBHOOK_URL })
    })
    .then(res => res.json())
    .then(console.log)
    .catch(console.error)
    ```

    - node app example to send chat id via webhook first install `yarn add node-telegram-bot-api` then try next code 
    ``` ts
    import TelegramBot from 'node-telegram-bot-api';
    import cors from 'cors';
    import express from 'express';

    const app = express();
    const TELEGRAM_BOT_TOKEN = 'TELEGRAM_BOT_TOKEN'; /** your telegram bot token */
    const bot = new TelegramBot(TELEGRAM_BOT_TOKEN);

    app.use(cors({ origin: true, credentials: true, preflightContinue: true }));
    app.use(express.json({ limit: '50mb' }));
    app.use(express.urlencoded({ extended: false }));

    const telegramWebhooksController = async (req: express.Request, res: express.Response) => {
      const { message } = req.body;
      const chatId = message.chat?.id;
      // console.log('Received update:', chatId, JSON.stringify(message, null, 2));

      if (chatId) {
        let responseMsg = '';

        switch (message.text) {
          case '/get_chat_id':
            responseMsg = `Chat Id is: \`${chatId}\``;
            break;
          default:
            responseMsg = `You said: ${message.text}`;
            break;
        }


        await bot.sendMessage(chatId, responseMsg);
      }

      res.sendStatus(200);
    };

    app.post('/telegram/webhooks', telegramWebhooksController);
    app.listen(3000, () => console.log('Server ready on port 3000.'));
    ```


3. **Get Your Chat ID:**
   - Start a chat with your bot or add it to a group/channel anyway the easiest way to get started is to navigate into your bot by name so in telegram search enter `@your_bot` then after open bot chat press start then `/get_chat_id` it will show you the message from above.

4. **Configure in JS:**
   ```ts
   SmsGateway.setTelegramConfig("YOUR_BOT_TOKEN", ["YOUR_CHAT_ID"]);
   ```

5. **Parse Mode:**
   - The package uses HTML parse mode for formatting. You cannot change this currently.

6. **Telegram Template**
  - Currently the telegram HTML template is const and couldn't be modified at the current time in future update may i provide a way to customize it but for now it looks like next

  ```html
  <b>Date</b>: <code>{{date}}</code>
  <b>From:</b> <u><code>{{sender}}</code></u>
  <b>TO:</b> <u><code>{{phoneNumber}}</code></u>
  <b>Message:</b> 
  <pre>{{msg}}</pre>
  ```
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

