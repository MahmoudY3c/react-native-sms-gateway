import { DeviceEventEmitter } from 'react-native';
import { NativeModules } from 'react-native';

const { SmsNativeModule } = NativeModules;

export type SmsModuleConfigurations = {
  sms_listener_enabled: boolean;
  delivery_type: 'http' | 'telegram' | 'all';
  http_configs: Array<{ url: string; headers?: Record<string, string> }>;
  telegram_bot_token: string | null;
  telegram_chat_ids: Array<string | number>;
  telegram_parse_mode: string;
  phoneNumber: string;
  senders_filter_list: string[];
  msg_keywords_filter_list: string[];
};

const SmsJsEvent = 'com.smsgateway.smsEvent';

export class SmsGateway {
  static enableSmsListener = (enabled: boolean) =>
    SmsNativeModule.enableSmsListener(enabled);

  static setHttpConfigs = (data: SmsModuleConfigurations['http_configs']) =>
    SmsNativeModule.setHttpConfigs(JSON.stringify(data));

  static getHttpConfigs = (): Promise<
    SmsModuleConfigurations['http_configs']
  > => SmsNativeModule.getHttpConfigs();

  // function to set all telegram configs bot_token and chat_ids at once
  static setTelegramConfig = (
    bot_token: string,
    chat_ids: SmsModuleConfigurations['telegram_chat_ids'],
  ) =>
    SmsNativeModule.setTelegramConfig(JSON.stringify({ bot_token, chat_ids }));

  // function to set only telegram bot_token
  static setTelegramBotToken = (bot_token: string) =>
    SmsNativeModule.setTelegramBotToken(bot_token);

  static getTelegramBotToken = (): Promise<
    SmsModuleConfigurations['telegram_bot_token']
  > => SmsNativeModule.getTelegramBotToken();

  static getTelegramChatIds = (): Promise<
    SmsModuleConfigurations['telegram_chat_ids']
  > => SmsNativeModule.getTelegramChatIds();

  static getTelegramParseMode = (): Promise<string> =>
    SmsNativeModule.getTelegramParseMode();

  // function to set only telegram chat_ids
  static setTelegramChatIds = (
    chat_ids: SmsModuleConfigurations['telegram_chat_ids'],
  ) => SmsNativeModule.setTelegramChatIds(JSON.stringify(chat_ids));

  /**
   * helper function to specify whether you want to send via only telegram or http or all
   */
  static setDeliveryType = (
    delivery_type: SmsModuleConfigurations['delivery_type'],
  ) => SmsNativeModule.setDeliveryType(delivery_type);

  static getDeliveryType = (): Promise<
    SmsModuleConfigurations['delivery_type']
  > => SmsNativeModule.getDeliveryType();

  static isSmsListenerEnabled = async (): Promise<
    SmsModuleConfigurations['sms_listener_enabled']
  > => SmsNativeModule.isSmsListenerEnabled();

  static getUserPhoneNumber = (): Promise<
    SmsModuleConfigurations['phoneNumber']
  > => SmsNativeModule.getUserPhoneNumber();

  static setUserPhoneNumber = (
    phoneNumber: SmsModuleConfigurations['phoneNumber'],
  ) => SmsNativeModule.setUserPhoneNumber(phoneNumber);

  static getAllSettings = (): Promise<SmsModuleConfigurations> =>
    SmsNativeModule.getAllSettings();

  static setSendersFilterList = (list: string[]): void =>
    SmsNativeModule.setSendersFilterList(list);
  static getSendersFilterList = (): Promise<string[]> =>
    SmsNativeModule.getSendersFilterList();

  static setMsgKeywordsFilterList = (list: string[]): void =>
    SmsNativeModule.setMsgKeywordsFilterList(list);
  static getMsgKeywordsFilterList = (): Promise<string[]> =>
    SmsNativeModule.getMsgKeywordsFilterList();

  // ! >>>>>>>>>>>>>>>>>>>>>>> next functions used only if you want to receive the sms into your app they are not related to http, telegram methods each method run separately so next not the core of the module it's mainly designed to send SMS to external service like generic server or telegram group, channel anyway won't work until you specify their value also you can disable the whole module including sms in js event by using enableSmsListener(false) <<<<<<<<<<<<<<<<<<<<<<<<<<<<

  static addEventListener(
    eventHandler: (data: {
      msg: string;
      timestamp: string;
      phoneNumber: string;
      sender: string;
    }) => any,
  ) {
    const subscriber = DeviceEventEmitter.addListener(SmsJsEvent, eventHandler);

    return [
      () => subscriber.remove(), //  use subscriber.remove or remove function returned for cleanup
      subscriber,
    ];
  }

  static getEventListenersCount = () =>
    DeviceEventEmitter.listenerCount(SmsJsEvent);

  static removeAllSMSEventListeners = () => {
    if (this.getEventListenersCount() > 0) {
      DeviceEventEmitter.removeAllListeners(SmsJsEvent);
    }
  };
}
