import {Config, Events, Platform} from 'ionic-angular';
import { Component } from '@angular/core';

import { ProfilePage } from '../pages/profile/profile';
import { ConfigurationScreen } from '../pages/configScreen/configScreen';
import { ScreenOrientation } from '@ionic-native/screen-orientation/ngx';
import { Transition } from '../providers/transition/Transition';
import { NetworkInterface } from '@ionic-native/network-interface/ngx';
import { AppUrlOpen, Plugins } from '@capacitor/core';
import { GlobalProvider } from '../providers/global/global';
import { ErrorHandlerProvider } from '../providers/error-handler/error-handler';
import {ProfileModel} from "../shared/models/profile-model";
import {DictionaryServiceProvider} from "../providers/dictionary-service/dictionary-service-provider.service";
import {NetworkStatus} from "@capacitor/core/dist/esm/core-plugin-definitions";

const { Toast, Network, App } = Plugins;
declare var Capacitor;
const { WifiEapConfigurator } = Capacitor.Plugins;

@Component({
  templateUrl: 'app.html'
})
/**
 * @class MyApp
 *
 * @description Init class with rootPage Welcome
 *
 **/
export class GeteduroamApp {
  rootPage;

  rootParams = {};

  profile: ProfileModel;
  /**
   * @constructor
   *
   */
  constructor(private platform: Platform, private config: Config,
              private screenOrientation: ScreenOrientation, public errorHandler: ErrorHandlerProvider,
              private networkInterface: NetworkInterface, private global: GlobalProvider, private dictionary: DictionaryServiceProvider,
              public event: Events) {

    this.platform.ready().then(async () => {
      // Transition provider, to navigate between pages
      this.config.setTransition('transition', Transition);
      // Setting the dictionary
      this.setDictionary();
      // ScreenOrientation plugin require first unlock screen and locked it after in mode portrait orientation
      this.screenOrientation.unlock();
      await this.screenOrientation.lock(this.screenOrientation.ORIENTATIONS.PORTRAIT_PRIMARY);
      // Listener to get status connection, apply when change status network
      await this.checkConnection();

      // Add listeners to app
      await this.addListeners();
    });
  }

  /**
   * This method check if network is enabled and show a error message to user remove network already associated
   * manually
   */
  async removeAssociatedManually() {
    let connect = await this.statusConnection();

    if (connect.connected) {

      await this.errorHandler.handleError(
        this.dictionary.getTranslation('error', 'available1') + this.global.getSsid() +
        this.dictionary.getTranslation('error', 'available2') +
        this.global.getSsid() + '.', false, '', 'removeConnection', true);

    } else {

      await this.errorHandler.handleError(
        this.dictionary.getTranslation('error', 'available1') +
        this.global.getSsid() + this.dictionary.getTranslation('error', 'available2') +
        this.global.getSsid() + '.\n' + this.dictionary.getTranslation('error', 'turn-on') +
        this.global.getSsid() + '.', false, '', 'enableAccess', false);
    }
  }

  /**
   * This method throw the app when is opened from a file
   */
  async handleOpenUrl(uri: string | any) {
    this.profile = new ProfileModel();
    this.profile.eapconfig_endpoint = !!uri.url ? uri.url : uri;
    this.profile.oauth = false;
    this.profile.id = "FileEap";
    this.profile.name = "FileEap";
    this.global.setProfile(this.profile);
  }

  /**
   * This method add listeners needed to app
   */
  addListeners() {
    // Listening to changes in network states, it show toast message when status changed
    Network.addListener('networkStatusChange', async () => {
      let connectionStatus: NetworkStatus = await this.statusConnection();

      this.connectionEvent(connectionStatus);

      !connectionStatus.connected ?
          this.alertConnection(this.dictionary.getTranslation('error', 'turn-on') +
            this.global.getSsid() + '.') :
          this.alertConnection(this.dictionary.getTranslation('text', 'network-available'));
    });

    // Listening to open app when open from a file
    App.addListener('appUrlOpen', async (urlOpen: AppUrlOpen) => {
      this.navigate(urlOpen.url);
    });

    App.addListener('backButton', () => {
      this.platform.backButton.observers.pop();

    });
  }

  /**
   * This method open ProfilePage when the app is initialize from an eap-config file
   * @param uri
   */
  async navigate(uri: string) {
    if (!!uri.includes('.eap-config') || !!uri.includes('file')) {
      await this.handleOpenUrl(uri);
      this.rootPage = ProfilePage;
    }
  }

  /**
   * This method shown an error message when network is disconnect
   */
  async notConnectionNetwork() {
      await this.errorHandler.handleError(this.dictionary.getTranslation('error', 'turn-on') +
        this.global.getSsid() + '.', false, '', 'enableAccess', true);
  }

  /**
   * This method check connection to initialized app
   * and show Toast message
   */
  private async checkConnection() {
    this.rootPage = ConfigurationScreen;
    let connectionStatus = await this.statusConnection();

    this.connectionEvent(connectionStatus);

    if (!connectionStatus.connected){
      this.notConnectionNetwork();
    }
  }

  /**
   * This method enable wifi on Android devices.
   *
   */
  async enableWifi() {
    await WifiEapConfigurator.enableWifi();
  }

  /**
   * This method throw an event to disabled button when network is disconnected.
   * @param connectionStatus
   */
  protected connectionEvent(connectionStatus: NetworkStatus){
    connectionStatus.connected ? this.event.publish('connection', 'connected') :
      this.event.publish('connection', 'disconnected');
  }

  /**
   * This method check status of connection
   */
  private async statusConnection(): Promise<NetworkStatus> {
    return await Network.getStatus()
  }

  /**
   * This method show a toast message
   * @param text
   */
  async alertConnection(text: string) {
    await Toast.show({
      text: text,
      duration: 'long'
    })
  }

  /**
   * This method sets the global dictionary
   *  Default: 'en'
   */
  private setDictionary(){
    this.dictionary.loadDictionary('en');
  }
}

