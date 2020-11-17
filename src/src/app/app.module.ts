import { NgModule, ErrorHandler } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { IonicApp, IonicModule } from 'ionic-angular';
import { GeteduroamApp } from './app.component';
import { ScreenOrientation } from '@ionic-native/screen-orientation/ngx';
import { StatusBar } from '@ionic-native/status-bar/ngx';
import { SplashScreen } from '@ionic-native/splash-screen/ngx';
import { AndroidPermissions } from '@ionic-native/android-permissions/ngx';
import { PagesModule } from '../pages/pages.module';
import { GeteduroamServices } from '../providers/geteduroam-services/geteduroam-services';
import { HTTP } from '@ionic-native/http/ngx';
import { ErrorHandlerProvider } from '../providers/error-handler/error-handler';
import { LoadingProvider } from '../providers/loading/loading';
import { NetworkInterface } from '@ionic-native/network-interface/ngx'
import { StoringProvider } from '../providers/storing/storing';
import { GlobalProvider } from '../providers/global/global';
import { ErrorServiceProvider } from "../providers/error-service/error-service";
import { OauthConfProvider } from '../providers/oauth-conf/oauth-conf';
import { InstitutionSearch } from '../pages/institutionSearch/institutionSearch';
import { ReactiveFormsModule } from '@angular/forms';

@NgModule({
  declarations: [
    GeteduroamApp
  ],
  imports: [
    BrowserModule,
    ReactiveFormsModule,
    PagesModule,
    IonicModule.forRoot(GeteduroamApp, { preload: InstitutionSearch }),
  ],
  bootstrap: [IonicApp],
  entryComponents: [
    GeteduroamApp
  ],
  providers: [
    AndroidPermissions,
    StatusBar,
    SplashScreen,
    HTTP,
    {provide: ErrorHandler, useClass: ErrorHandlerProvider},
    GeteduroamServices,
    ScreenOrientation,
    LoadingProvider,
    NetworkInterface,
    StoringProvider,
    GlobalProvider,
    OauthConfProvider,
    ErrorServiceProvider
  ],
  exports:[],
})
export class AppModule {

}

