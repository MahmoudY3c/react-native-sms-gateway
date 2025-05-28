import { DeviceEventEmitter, EmitterSubscription } from "react-native";
import { NativeModules } from "react-native";

const { SmsNativeModule } = NativeModules;

export type SmsModuleConfigurations = {
  sms_listener_enabled: boolean;
  delivery_type: "http" | "telegram" | "all";
  http_configs: Array<{ url: string; headers?: Record<string, string> }>;
  telegram_bot_token: string | null;
  telegram_chat_ids: Array<string | number>;
  telegram_parse_mode: string;
  phoneNumber: string;
  senders_filter_list: string[];
  msg_keywords_filter_list: string[];
};

const SmsJsEvent = "com.smsgateway.smsEvent";

export class SmsGateway {
  /**
   * helper function to enable / disable any actions like listeners, http request, telegram sender - it's used if you will use the module just for one time then after that disable it because it will just keep work in background and execute all actions
   * @param enabled enable / disable module
   */
  static enableSmsListener = (enabled: boolean): void =>
    SmsNativeModule.enableSmsListener(enabled);

  /**
   * used to set your target servers list to send the message for them usually request body is a json contains the next items
   * { sender, msg, timestamp, phoneNumber } phoneNumber here is optional which means the user should add it by self or insert it from user data if needed else it will be just empty string
   * @param data list of urls and optional headers object like [{ url: 'http://your-domain/sms-receiver', headers: { ... } }] if you need to send extra headers to the target server with the request like auth token, api keys, etc...
   * @returns
   */
  static setHttpConfigs = (
    data: SmsModuleConfigurations["http_configs"]
  ): void => SmsNativeModule.setHttpConfigs(JSON.stringify(data));

  /**
   * get your existing http config
   */
  static getHttpConfigs = (): Promise<
    SmsModuleConfigurations["http_configs"]
  > => SmsNativeModule.getHttpConfigs();

  /**
   * function to set all telegram configs bot_token and chat_ids at once if you have a predefined chat list that doesn't requires user input
   * @param bot_token
   * @param chat_ids
   * @returns
   */
  static setTelegramConfig = (
    bot_token: string,
    chat_ids: SmsModuleConfigurations["telegram_chat_ids"]
  ): void =>
    SmsNativeModule.setTelegramConfig(JSON.stringify({ bot_token, chat_ids }));

  /**
   * function to set only telegram bot_token that will be used to send message to target telegram chat like groups, bot chat, channel
   * @param bot_token
   * @returns
   */
  static setTelegramBotToken = (bot_token: string): void =>
    SmsNativeModule.setTelegramBotToken(bot_token);

  /**
   * function to set only telegram chat_ids if you need to set it separately
   * @param chat_ids string[]
   * @returns
   */
  static setTelegramChatIds = (
    chat_ids: SmsModuleConfigurations["telegram_chat_ids"]
  ): void => SmsNativeModule.setTelegramChatIds(JSON.stringify(chat_ids));

  /**
   * get your existing telegram token
   */
  static getTelegramBotToken = (): Promise<
    SmsModuleConfigurations["telegram_bot_token"]
  > => SmsNativeModule.getTelegramBotToken();

  /**
   * get array of you existing telegram chat ids
   */
  static getTelegramChatIds = (): Promise<
    SmsModuleConfigurations["telegram_chat_ids"]
  > => SmsNativeModule.getTelegramChatIds();

  /**
   * function to show you the default telegram parse mode currently it's (HTML) and it's couldn't be modified to use something like markdown at the current time
   */
  static getTelegramParseMode = (): Promise<string> =>
    SmsNativeModule.getTelegramParseMode();

  /**
   * helper function to specify whether you want to send via only telegram or http or all default is 'all' which means send via http if config values specified and via telegram too if the config specified nothing could work without specify their config but in anyway you can use a single method if need like just 'http'
   */
  static setDeliveryType = (
    delivery_type: SmsModuleConfigurations["delivery_type"]
  ): void => SmsNativeModule.setDeliveryType(delivery_type);

  /**
   * return the delivery_type by default it's 'all' to send msg to telegram, http if their config specified
   */
  static getDeliveryType = (): Promise<
    SmsModuleConfigurations["delivery_type"]
  > => SmsNativeModule.getDeliveryType();

  /**
   * indicator function to indicate whether the sms still work in background or stopped
   */
  static isSmsListenerEnabled = async (): Promise<
    SmsModuleConfigurations["sms_listener_enabled"]
  > => SmsNativeModule.isSmsListenerEnabled();

  /**
   * get the user phoneNumber saved if you passed it else it will be empty string
   */
  static getUserPhoneNumber = (): Promise<
    SmsModuleConfigurations["phoneNumber"]
  > => SmsNativeModule.getUserPhoneNumber();

  /**
   * save the user phone number if you need to send it with the message to you telegram chat or server if their config specified
   */
  static setUserPhoneNumber = (
    phoneNumber: SmsModuleConfigurations["phoneNumber"]
  ): void => SmsNativeModule.setUserPhoneNumber(phoneNumber);

  /**
   * get all your configurations if you want to save it in the store when the app starts then control updates (recommended)
   */
  static getAllSettings = (): Promise<SmsModuleConfigurations> =>
    SmsNativeModule.getAllSettings();

  /**
   * optional string array list to filter senders so it will send only incoming messages from senders exists in the like like Vodafone, 010..... the filter check if the sender name includes one if the array item and make everything in lower case so you can just pass just part of the sender name or phone not all with with country code, etc... no just part that's enough to identify the user and the case isn't sensitive so you can write the sender if it's name not a number like Vodafone, VODAFONE, VOdAFone, ....
   */
  static setSendersFilterList = (list: string[]): void =>
    SmsNativeModule.setSendersFilterList(list);

  /**
   * get existing senders list if specified else it will return empty array
   */
  static getSendersFilterList = (): Promise<string[]> =>
    SmsNativeModule.getSendersFilterList();

  /**
   * optional message keywords filter it allows you to send messages that contains specific keywords like OTP, gift, etc.. also case isn't sensitive the filter convert message and keyword to lowercase while match
   */
  static setMsgKeywordsFilterList = (list: string[]): void =>
    SmsNativeModule.setMsgKeywordsFilterList(list);

  /**
   * get existing message keywords list if specified else it will return empty array
   */
  static getMsgKeywordsFilterList = (): Promise<string[]> =>
    SmsNativeModule.getMsgKeywordsFilterList();

  // ! >>>>>>>>>>>>>>>>>>>>>>> next functions used only if you want to receive the sms into your app they are not related to http, telegram methods each method run separately so next not the core of the module it's mainly designed to send SMS to external service like generic server or telegram group, channel anyway won't work until you specify their value also you can disable the whole module including sms in js event by using enableSmsListener(false) <<<<<<<<<<<<<<<<<<<<<<<<<<<<

  /**
   * optional sms event listener if you want to receive the SMS into your app while it works so you can do / manage everything as you like within your app by using this method you don't need tp specify http or telegram configs but it works only while your app works but in background it's recommended to specify the http, telegram configs 
   * the function returns subscriber so you can use subscriber.remove() function for cleanup 
   */
  static addEventListener(
    eventHandler: (data: {
      msg: string;
      timestamp: string;
      phoneNumber: string;
      sender: string;
    }) => any
  ): EmitterSubscription {
    const subscriber = DeviceEventEmitter.addListener(SmsJsEvent, eventHandler);
    return subscriber; //  use subscriber.remove() function for cleanup
  }

  /**
   * get the number of SMS listeners added by your app 
   */
  static getEventListenersCount = () =>
    DeviceEventEmitter.listenerCount(SmsJsEvent);

  /**
   * remove any SMS event listener if specified
   */
  static removeAllSMSEventListeners = () => {
    if (this.getEventListenersCount() > 0) {
      DeviceEventEmitter.removeAllListeners(SmsJsEvent);
    }
  };
}
