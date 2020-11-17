import {LoadingProvider} from "../providers/loading/loading";
import {DictionaryServiceProvider} from "../providers/dictionary-service/dictionary-service-provider.service";
import {Events} from "ionic-angular";
import { Plugins, ActionSheetOptionStyle } from '@capacitor/core';
import {GlobalProvider} from "../providers/global/global";
const { Toast, Network, Modals, Browser } = Plugins;
declare var window: any;
export abstract class BasePage {

  /**
   * It checks if navigation is active
   */
  protected activeNavigation: boolean = true;

  /**
   * It checks if toast message is active
   */
  protected messageShown: boolean = false;

  protected constructor(protected loading: LoadingProvider, protected dictionary: DictionaryServiceProvider,
      protected event:Events, protected global: GlobalProvider) {

      this.statusConnection();
      this.event.subscribe('connection', (data) => {
         this.activeNavigation = data == 'connected';
      });
  }

  /**
   * Method to check status connection
   */
  protected async statusConnection() {
    let connect = await Network.getStatus();
    this.activeNavigation = connect.connected;
  }

  /**
   * This method calls the getTranslation method form the service [DictionaryService]{@link ./providers/dictionary-service/DictionaryServiceProvider.html}.
   * @param key to search in the dictionary
   * @param section in which to look for the key
   * @return the translated phrase
   */
  protected getString(section: string, key:string){
      return this.dictionary.getTranslation(section, key);
  }

  /**
   * Method called to show spinner
   * @param methodResponse
   */
  protected async waitingSpinner(methodResponse){
      this.loading.createAndPresent();
      return methodResponse;
  }

  /**
   * Method called to remove loading spinner
   */
  protected removeSpinner() {
    this.loading?.dismiss();
  }

  /**
   * Method called to check status navigation
   */
  protected getActiveNavigation(){
      return this.activeNavigation;
  }

  /**
   * This method show a toast message
   */
  protected async alertConnectionDisabled() {
      this.showToast(this.dictionary.getTranslation('error', 'turn-on'));
  }

  /**
   * This method show a toast message
   */
  protected async showToast(message: string) {
      if (!this.messageShown) {
          await Toast.show({
              text: message,
              duration: 'long'
          });
          this.messageShown = true;
      }
  }
  /**
   * This method show a modal with support options
   */
  async modalSupport() {
    const providerInfo = this.global.getProviderInfo();
    let options = {
      title: this.dictionary.getTranslation('modalSupport', 'title'),
      message: this.dictionary.getTranslation('modalSupport', 'message'),
      options: []
    };
    // Options available
    if (providerInfo.helpdesk.webAddress) {
      options.options.push({action: 'web', title: this.dictionary.getTranslation('modalSupport', 'web') + this.shortenAddress(providerInfo.helpdesk.webAddress)});
    }
    if (providerInfo.helpdesk.emailAddress) options.options.push({action: 'email', title: this.dictionary.getTranslation('modalSupport', 'email') + providerInfo.helpdesk.emailAddress});
    if (providerInfo.helpdesk.phone) options.options.push({action: 'phone', title: this.dictionary.getTranslation('modalSupport', 'phone') + providerInfo.helpdesk.phone});
    // Include cancel button
    options.options.push({title: this.dictionary.getTranslation('modalSupport', 'cancel'), style: ActionSheetOptionStyle.Cancel });
    // Show modal
    let supportOption = await Modals.showActions(options);
    let selectedAction = options.options[supportOption.index].action;

    switch(selectedAction) {
        // Web option
      case 'web':
        return await Browser.open({url: providerInfo.helpdesk.webAddress});
        // Email option
      case 'email':
        return window.location.href = 'mailto:' + providerInfo.helpdesk.emailAddress + '?Subject=' + this.dictionary.getTranslation('modalSupport', 'help');
        // Phone option
      case 'phone':
        return window.location.href = 'tel:'+providerInfo.helpdesk.phone;
    }

  }

  shortenAddress(address?: string) {
    if (address) {
      return address.replace(/^https:\/\/([^/]+)\/.*?\/.*?([^/]+\/?)$/, '$1/…/$2');
    }
  }

  async termsModal(termsOfUse: any) {
    let splitTerms = termsOfUse.split(' ');

    let terms = splitTerms.map(res => {
      if (!!res.match(/\bwww?\S+/gi) ||  !!res.match(/\bhttps?\S+/gi) || res.match(/\bhttp?\S+/gi)) {
        const link = !!res.match(/\bwww?\S+/gi) ? 'http://'+res.match(/\bwww?\S+/gi)[0] :
          !!res.match(/\bhttps?\S+/gi) ? res.match(/\bhttps?\S+/gi)[0] : res.match(/\bhttp?\S+/gi)[0];

        res =`<a href="${link}">${res}</a>`;

      }
      return res
    });

    terms = terms.join(' ');

    const htmlView = `<title>${this.getString('label', 'terms')}</title>
      <meta name="charset" value="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
      <style type="text/css">
        @media (prefers-color-scheme: dark) { html { background: black; color: rgba(255,255,255,.75); } }
        html { font-family: sans-serif; margin-top: 2.4em; }
        a { color: #276fbf; }
      </style>
      ${terms}`;
    const pageContentUrl = 'data:text/html;base64,' + btoa(htmlView);
    /*
    const browser = window.cordova.InAppBrowser.open(
      pageContentUrl,
      '_blank',
      'location=no,hidenavigationbuttons=yes,hidespinner=yes,toolbarposition=top,beforeload=yes,footer=yes,closebuttoncaption=Done'
    );

    browser.addEventListener('beforeload', (params, callback) => {
      if (params.url == pageContentUrl) {
        callback(params.url);
      } else {
        Browser.open({url: params.url});
        setTimeout(() => browser.close(), 1000);
      }
    });

     */
  }
}
