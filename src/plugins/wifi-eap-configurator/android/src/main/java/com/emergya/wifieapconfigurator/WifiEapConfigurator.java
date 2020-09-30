package com.emergya.wifieapconfigurator;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.UiAutomation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.hotspot2.ConfigParser;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.hotspot2.pps.HomeSp;
import android.net.wifi.hotspot2.pps.Credential;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
//import android.net.wifi.WifiNetworkSuggestion;
//import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.PluginResult;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import static androidx.core.content.ContextCompat.startActivity;
import static androidx.core.content.PermissionChecker.checkSelfPermission;
import static java.lang.System.in;

@NativePlugin(
        permissions={
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION
        })
public class WifiEapConfigurator extends Plugin {
    
    List<ScanResult> results = null;

    @PluginMethod()
    public void configureAP(PluginCall call) {
        String ssid = null;
        boolean res = true;
        
        String oid = null;
        if (call.getString("oid") != null && !call.getString("oid").equals("")) {
            oid = call.getString("oid");
        }
        
        if (!call.getString("ssid").equals("") && call.getString("ssid") != null) {
            ssid = call.getString("ssid");

        } else {
            // ssid OR oid are mandatory 
            if (oid == null) {
                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.error.ssid.missing");
                call.success(object);
                res = false;
            }
        }

        String clientCertificate = null;
        if (call.getString("clientCertificate") != null && !call.getString("clientCertificate").equals("")) {
            clientCertificate = call.getString("clientCertificate");
        }

        String passPhrase = null;
        if (call.getString("passPhrase") != null && !call.getString("passPhrase").equals("")) {
            passPhrase = call.getString("passPhrase");
        }

        String anonymousIdentity = null;
        if (call.getString("anonymous") != null && !call.getString("anonymous").equals("")) {
            anonymousIdentity = call.getString("anonymous");
        }

        String caCertificate = null;
        if (call.getString("caCertificate") != null && !call.getString("caCertificate").equals("")) {
            caCertificate = call.getString("caCertificate");
        }

        Integer eap = null;
        if (call.getInt("eap") != null && (call.getInt("eap") == 13 || call.getInt("eap") == 21
                || call.getInt("eap") == 25)) {
            eap = call.getInt("eap");
        } else {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.eap.missing");
            call.success(object);
            res = false;
        }

        String servername = null;
        if (call.getString("servername") != null && !call.getString("servername").equals("")) {
            servername = call.getString("servername");
        }

        String username = null;
        String password = null;
        Integer auth = null;
       
        String id = null;
        if (call.getString("id") != null && !call.getString("id").equals("")) {
            id = call.getString("id");
        }
        String displayName = null;
        if (call.getString("displayName") != null && !call.getString("displayName").equals("")) {
            displayName = call.getString("displayName");
        }

        if (clientCertificate == null && passPhrase == null) {
            if (call.getString("username") != null && !call.getString("username").equals("")) {
                username = call.getString("username");
            } else {
                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.error.username.missing");
                call.success(object);
                res = false;
            }

            if (call.getString("password") != null && !call.getString("password").equals("")) {
                password = call.getString("password");
            } else {
                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.error.password.missing");
                call.success(object);
                res = false;
            }


            if (call.getInt("auth") != null) {
                auth = call.getInt("auth");
            } else {
                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.error.auth.missing");
                call.success(object);
                res = false;
            }
        }

        if (res) {
            res = getNetworkAssociated(call, ssid);
        }

        if (res) {
            connectAP(ssid, username, password, servername, caCertificate, clientCertificate, passPhrase, eap, auth, anonymousIdentity, displayName, id, oid, call);
        }
    }

    void connectAP(String ssid, String username, String password, String servername, String caCertificate, String clientCertificate, String passPhrase,
                   Integer eap, Integer auth, String anonymousIdentity, String displayName, String id, String oid, PluginCall call) {

        WifiEnterpriseConfig enterpriseConfig = new WifiEnterpriseConfig();

        if (anonymousIdentity != null && !anonymousIdentity.equals("")) {
            enterpriseConfig.setAnonymousIdentity(anonymousIdentity);
        }

        if (servername != null && !servername.equals("")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String longestCommonSuffix = null;
                if (call.getString("longestCommonSuffix") != null && !call.getString("longestCommonSuffix").trim().equals("")) {
                    longestCommonSuffix = call.getString("longestCommonSuffix");
                    enterpriseConfig.setDomainSuffixMatch(longestCommonSuffix);
                }
                // now we have to configure the DNS
                String[] servernames = servername.split(";");
                for (int i = 0; i < servernames.length; i++) {
                    servernames[i] = "DNS:" + servernames[i];
                }
                enterpriseConfig.setAltSubjectMatch(String.join(";", servernames));
            }
        }

        Integer eapMethod = getEapMethod(eap, call);
        enterpriseConfig.setEapMethod(eapMethod);

        CertificateFactory certFactory = null;
        X509Certificate[] caCerts = null;
        List<X509Certificate> certificates = new ArrayList<X509Certificate>();
        if (caCertificate != null && !caCertificate.equals("")) {
            // Multi CA-allowing
            String[] caCertificates = caCertificate.split(";");
            // building the certificates
            for (String certString : caCertificates) {
                byte[] bytes = Base64.decode(certString, Base64.NO_WRAP);
                ByteArrayInputStream b = new ByteArrayInputStream(bytes);

                try {
                    certFactory = CertificateFactory.getInstance("X.509");
                    certificates.add((X509Certificate) certFactory.generateCertificate(b));
                } catch (CertificateException e) {
                    JSObject object = new JSObject();
                    object.put("success", false);
                    object.put("message", "plugin.wifieapconfigurator.error.ca.invalid");
                    call.success(object);
                    e.printStackTrace();
                    Log.e("error", e.getMessage());
                } catch (IllegalArgumentException e) {
                    JSObject object = new JSObject();
                    object.put("success", false);
                    object.put("message", "plugin.wifieapconfigurator.error.ca.invalid");
                    call.success(object);
                    e.printStackTrace();
                    Log.e("error", e.getMessage());
                }
            }
            // Adding the certificates to the configuration
            caCerts = certificates.toArray(new X509Certificate[certificates.size()]);
            try {
                enterpriseConfig.setCaCertificates(caCerts);
            } catch (IllegalArgumentException e) {
                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.error.ca.invalid");
                call.success(object);
                e.printStackTrace();
                Log.e("error", e.getMessage());
            }
        }

        if ((clientCertificate == null || clientCertificate.equals("")) && (passPhrase == null || passPhrase.equals(""))) {
            enterpriseConfig.setIdentity(username);
            enterpriseConfig.setPassword(password);

            Integer authMethod = getAuthMethod(auth, call);
            enterpriseConfig.setPhase2Method(authMethod);

        } else {

            KeyStore pkcs12ks = null;
            try {
                pkcs12ks = KeyStore.getInstance("pkcs12");

                byte[] bytes = Base64.decode(clientCertificate, Base64.NO_WRAP);
                ByteArrayInputStream b = new ByteArrayInputStream(bytes);
                InputStream in = new BufferedInputStream(b);
                pkcs12ks.load(in, passPhrase.toCharArray());

                Enumeration<String> aliases = pkcs12ks.aliases();

                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    X509Certificate cert = (X509Certificate) pkcs12ks.getCertificate(alias);
                    PrivateKey key = (PrivateKey) pkcs12ks.getKey(alias, passPhrase.toCharArray());
                    enterpriseConfig.setClientKeyEntry(key, cert);
                }

            } catch (KeyStoreException e) {
                sendClientCertificateError(e, call);
                e.printStackTrace();
            } catch (CertificateException e) {
                sendClientCertificateError(e, call);
                e.printStackTrace();
            } catch (IOException e) {
                sendClientCertificateError(e, call);
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                sendClientCertificateError(e, call);
                e.printStackTrace();
            } catch (UnrecoverableKeyException e) {
                sendClientCertificateError(e, call);
                e.printStackTrace();
            }
        }

        WifiManager myWifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);

        this.testPasspoint(myWifiManager, id, displayName, oid, enterpriseConfig, call);

        //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        if(ssid != null) {
            connectWifiBySsid(myWifiManager, ssid, enterpriseConfig, call, displayName, null, null);
        }

        final PackageManager packageManager = getContext().getPackageManager();
        FeatureInfo[] hard = packageManager.getSystemAvailableFeatures();

        if (oid != null) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_PASSPOINT)) {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    removePasspoint(myWifiManager, id);
                }
                connectPasspoint(myWifiManager, id, displayName, oid, enterpriseConfig, call);
            } else {
                this.connectWifiBySsid(myWifiManager, "#Passpoint", enterpriseConfig, call, displayName, oid, id);
            }
        }

        /*} else {
            PasspointConfiguration passpointConfig =  null;
            if (oid != null) {
                passpointConfig = new PasspointConfiguration();

                HomeSp homeSp = new HomeSp();
                if (displayName != null) {
                    homeSp.setFriendlyName(displayName);
                } else {
                    homeSp.setFriendlyName("geteduroam configured HS20");
                }
                homeSp.setFqdn(id);
                // oid can be a list with commas.
                String[] consortiumOIDs = oid.split(";");
                long[] roamingConsortiumOIDs = new long[consortiumOIDs.length];
                int index = 0;
                for(String roamingConsortiumOIDString : consortiumOIDs) {
                    roamingConsortiumOIDs[index] = Long.decode(roamingConsortiumOIDString);
                    index++;
                }
                homeSp.setRoamingConsortiumOis(roamingConsortiumOIDs);
                passpointConfig.setHomeSp(homeSp);
            }

            if (connectWifiAndroidQ(ssid, enterpriseConfig, passpointConfig)) {
                JSObject object = new JSObject();
                object.put("success", true);
                object.put("message", "plugin.wifieapconfigurator.success.network.linked");
                call.success(object);
            } else {
                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.success.network.reachable");
                call.success(object);
            }
        }*/
    }

    private void removePasspoint(WifiManager wifiManager, String id) {
        List passpointsConfigurated = new ArrayList();
        try {
            passpointsConfigurated = wifiManager.getPasspointConfigurations();
            int pos = 0;
            boolean enc = false;
            while(passpointsConfigurated.size() > pos && !enc){
                if((passpointsConfigurated.get(pos)).equals(id)){
                    enc = true;
                }else{
                    pos++;
                }
            }
            if(enc){
                wifiManager.removePasspointConfiguration(id);
            }
        } catch (IllegalArgumentException e) {}
    }

    private void connectPasspoint(WifiManager wifiManager, String id, String displayName, String oid, WifiEnterpriseConfig enterpriseConfig, PluginCall call){
        PasspointConfiguration passpointConfig = new PasspointConfiguration();

        HomeSp homeSp = new HomeSp();
        if (displayName != null) {
            homeSp.setFriendlyName(displayName);
        } else {
            homeSp.setFriendlyName("geteduroam configured HS20");
        }
        homeSp.setFqdn(id);
        // oid can be a list with commas.
        String[] consortiumOIDs = oid.split(";");
        long[] roamingConsortiumOIDs = new long[consortiumOIDs.length];
        int index = 0;
        for(String roamingConsortiumOIDString : consortiumOIDs) {
            if ( !roamingConsortiumOIDString.startsWith("0x")) {
                roamingConsortiumOIDString = "0x" + roamingConsortiumOIDString;
            }
            roamingConsortiumOIDs[index] = Long.decode(roamingConsortiumOIDString);
            index++;
        }
        homeSp.setRoamingConsortiumOis(roamingConsortiumOIDs);
        passpointConfig.setHomeSp(homeSp);

        Credential.SimCredential simCred = new Credential.SimCredential();


        try{
            wifiManager.addOrUpdatePasspointConfiguration(passpointConfig);
            JSObject object = new JSObject();
            object.put("success", true);
            object.put("message", "plugin.wifieapconfigurator.success.passpoint.linked");
            call.success(object);
        } catch (IllegalArgumentException e){
            this.connectWifiBySsid(wifiManager, "#Passpoint", enterpriseConfig, call, displayName, oid, id);
        }
    }

    /*private boolean createSuggestion(WifiManager wifiManager, String ssid, WifiEnterpriseConfig enterpriseConfig, PasspointConfiguration passpointConfig){
            boolean configured = false;
            if (getPermission(Manifest.permission.CHANGE_NETWORK_STATE)) {

                ArrayList<WifiNetworkSuggestion> suggestions = new ArrayList<>();
                WifiNetworkSuggestion.Builder suggestionBuilder =  new WifiNetworkSuggestion.Builder();

                if (ssid != null) {
                    suggestionBuilder.setSsid(ssid);
                }
                suggestionBuilder.setWpa2EnterpriseConfig(enterpriseConfig);

                if (passpointConfig != null && Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    suggestionBuilder.setPasspointConfig(passpointConfig);
                }
                final WifiNetworkSuggestion suggestion = suggestionBuilder.build();

                // WifiNetworkSuggestion approach
                suggestions.add(suggestion);
               
                wifiManager.removeNetworkSuggestions(new ArrayList<WifiNetworkSuggestion>());
                int status = wifiManager.addNetworkSuggestions(suggestions);

                if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {

                    final IntentFilter intentFilter =
                            new IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);

                    final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (!intent.getAction().equals(
                                    WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                                return;
                            }
                        }
                    };
                    getContext().registerReceiver(broadcastReceiver, intentFilter);

                    configured = true;
                } else {
                    Log.d("STATUS ERROR", "" + status);
                    configured = false;
                }
            }
            return configured;

    }*/

    private void connectWifiBySsid(WifiManager myWifiManager, String ssid, WifiEnterpriseConfig enterpriseConfig, PluginCall call, String displayName, String oid, String id) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + ssid + "\"";
        config.priority = 1;
        config.status = WifiConfiguration.Status.ENABLED;
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
        config.enterpriseConfig = enterpriseConfig;

        if(oid != null){
            /*String[] consortiumOIDs = oid.split(";");
            long[] roamingConsortiumOIDs = new long[consortiumOIDs.length];
            int index = 0;
            for(String roamingConsortiumOIDString : consortiumOIDs) {
                if ( !roamingConsortiumOIDString.startsWith("0x")) {
                    roamingConsortiumOIDString = "0x" + roamingConsortiumOIDString;
                }
                roamingConsortiumOIDs[index] = Long.decode(roamingConsortiumOIDString);
                index++;
            }
            config.roamingConsortiumIds = roamingConsortiumOIDs;*/
            if (displayName != null) {
                config.providerFriendlyName = displayName;
            } else {
                config.providerFriendlyName = "geteduroam configured HS20";
            }
            config.FQDN = id;
        }

        try {
            int wifiIndex = myWifiManager.addNetwork(config);
            myWifiManager.disconnect();
            myWifiManager.enableNetwork(wifiIndex, true);
            myWifiManager.reconnect();

            WifiManager wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(true);

            JSObject object = new JSObject();
            object.put("success", true);
            object.put("message", "plugin.wifieapconfigurator.success.network.linked");
            call.success(object);
        } catch (java.lang.SecurityException e) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.network.linked");
            call.success(object);
            e.printStackTrace();
            Log.e("error", e.getMessage());
        }
    }

    /*
    private boolean connectWifiAndroidQ(String ssid, WifiEnterpriseConfig enterpriseConfig, PasspointConfiguration passpointConfig) {
        boolean configured = false;
        if (getPermission(Manifest.permission.CHANGE_NETWORK_STATE)) {

            ArrayList<WifiNetworkSuggestion> suggestions = new ArrayList<>();
            WifiNetworkSuggestion.Builder suggestionBuilder =  new WifiNetworkSuggestion.Builder();

            suggestionBuilder.setSsid(ssid);
            suggestionBuilder.setWpa2EnterpriseConfig(enterpriseConfig);

            if (passpointConfig != null) {
                suggestionBuilder.setPasspointConfig(passpointConfig);
            }
            final WifiNetworkSuggestion suggestion = suggestionBuilder.build();

            // WifiNetworkSuggestion approach
            suggestions.add(suggestion);
            WifiManager wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int status = wifiManager.addNetworkSuggestions(suggestions);

            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {

                final IntentFilter intentFilter =
                        new IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);

                final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (!intent.getAction().equals(
                                WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                            return;
                        }
                    }
                };
                getContext().registerReceiver(broadcastReceiver, intentFilter);

                configured = true;
            } else {
                Log.d("STATUS ERROR", "" + status);
                configured = false;
            }
        }
        return configured;
    }*/

    private void sendClientCertificateError(Exception e, PluginCall call) {
        JSObject object = new JSObject();
        object.put("success", false);
        object.put("message", "plugin.wifieapconfigurator.error.clientCertificate.invalid - " + e.getMessage());
        call.success(object);
        Log.e("error", e.getMessage());
    }

    @PluginMethod
    public boolean removeNetwork(PluginCall call) {
        String ssid = null;
        boolean res = false;

        if (call.getString("ssid") != null && !call.getString("ssid").equals("")) {
            ssid = call.getString("ssid");
        } else {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.ssid.missing");
            call.success(object);
            return res;
        }

        WifiManager wifi = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        /*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { */
            List<WifiConfiguration> configuredNetworks = wifi.getConfiguredNetworks();
            for (WifiConfiguration conf : configuredNetworks) {
                if (conf.SSID.toLowerCase().equals(ssid.toLowerCase()) || conf.SSID.toLowerCase().equals("\"" + ssid.toLowerCase() + "\"")) {
                    wifi.removeNetwork(conf.networkId);
                    wifi.saveConfiguration();
                    JSObject object = new JSObject();
                    object.put("success", true);
                    object.put("message", "plugin.wifieapconfigurator.success.network.removed");
                    call.success(object);
                    res = true;
                }
            }
        /*} else {
            wifi.removeNetworkSuggestions(new ArrayList<WifiNetworkSuggestion>());
            JSObject object = new JSObject();
            object.put("success", true);
            object.put("message", "plugin.wifieapconfigurator.success.network.removed");
            call.success(object);
            res = true;
        }*/

        if (!res) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.success.network.missing");
            call.success(object);
        }

        return res;
    }

    @PluginMethod
    public void enableWifi(PluginCall call) {
        //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            WifiManager wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager.setWifiEnabled(true)) {
                JSObject object = new JSObject();
                object.put("success", true);
                object.put("message", "plugin.wifieapconfigurator.success.wifi.enabled");
                call.success(object);
            } else {
                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.error.wifi.disabled");
                call.success(object);
            }
        /*} else{
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.wifi.disabled");
            call.success(object);
        }*/
    }

    @PluginMethod
    public boolean isNetworkAssociated(PluginCall call) {
        String ssid = null;
        boolean res = false, isOverridable = false;

        //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (call.getString("ssid") != null && !call.getString("ssid").equals("")) {
                ssid = call.getString("ssid");
            } else {
                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.error.ssid.missing");
                call.success(object);
                return res;
            }

            WifiManager wifi = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            List<WifiConfiguration> configuredNetworks = wifi.getConfiguredNetworks();
            for (WifiConfiguration conf : configuredNetworks) {
                if (conf.SSID.toLowerCase().equals(ssid.toLowerCase()) || conf.SSID.toLowerCase().equals("\"" + ssid.toLowerCase() + "\"")) {

                    String packageName = getContext().getPackageName();
                    if (conf.toString().toLowerCase().contains(packageName.toLowerCase())) {
                        isOverridable = true;
                    }

                    JSObject object = new JSObject();
                    object.put("success", false);
                    object.put("message", "plugin.wifieapconfigurator.error.network.alreadyAssociated");
                    object.put("overridable", isOverridable);
                    call.success(object);
                    res = true;
                    break;
                }
            }

            if (!res) {
                JSObject object = new JSObject();
                object.put("success", true);
                object.put("message", "plugin.wifieapconfigurator.success.network.missing");
                call.success(object);
            }
        /*} else{
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.ssid.missing");
            call.success(object);
        }*/

        return res;
    }

    @PluginMethod
    public void reachableSSID(PluginCall call) {
        String ssid = null;
        boolean isReachable = false;
        if (call.getString("ssid") != null && !call.getString("ssid").equals("")) {
            ssid = call.getString("ssid");
        } else {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.ssid.missing");
            call.success(object);
        }

        boolean granted = getPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        boolean location = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!location) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.location.disabled");
            call.success(object);
        } else if (granted) {

            WifiManager wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            Iterator<ScanResult> results = wifiManager.getScanResults().iterator();

            while (isReachable == false && results.hasNext()) {
                ScanResult s = results.next();
                if (s.SSID.toLowerCase().equals(ssid.toLowerCase()) || s.SSID.toLowerCase().equals("\"" + ssid.toLowerCase() + "\"")) {
                    isReachable = true;
                }
            }

            String message = isReachable ? "plugin.wifieapconfigurator.success.network.reachable" : "plugin.wifieapconfigurator.error.network.notReachable";

            JSObject object = new JSObject();
            object.put("success", true);
            object.put("message", message);
            object.put("isReachable", isReachable);
            call.success(object);
        } else {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.permission.notGranted");
            call.success(object);
        }
    }

    @PluginMethod
    public void isConnectedSSID(PluginCall call) {
        String ssid = null;
        boolean isConnected = false;
        if (call.getString("ssid") != null && !call.getString("ssid").equals("")) {
            ssid = call.getString("ssid");
        } else {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.ssid.missing");
            call.success(object);
        }

        boolean granted = getPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        boolean location = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!location) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.location.disabled");
            call.success(object);
        } else if (granted) {
            WifiManager wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifiManager.getConnectionInfo();
            String currentlySsid = info.getSSID();
            if (currentlySsid != null && (currentlySsid.toLowerCase().equals("\"" + ssid.toLowerCase() + "\"") || currentlySsid.toLowerCase().equals(ssid.toLowerCase()))) {
                isConnected = true;
            }

            String message = isConnected ? "plugin.wifieapconfigurator.success.network.connected" : "plugin.wifieapconfigurator.error.network.notConnected";

            JSObject object = new JSObject();
            object.put("success", true);
            object.put("message", message);
            object.put("isConnected", isConnected);
            call.success(object);
        } else {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.permission.notGranted");
            call.success(object);
        }

    }
    
    private boolean getNetworkAssociated(PluginCall call, String ssid) {
        boolean res = true, isOverridable = false;

        //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            WifiManager wifi = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            List<WifiConfiguration> configuredNetworks = wifi.getConfiguredNetworks();

            for (WifiConfiguration conf : configuredNetworks) {
                if (conf.SSID.toLowerCase().equals(ssid.toLowerCase()) || conf.SSID.toLowerCase().equals("\"" + ssid.toLowerCase() + "\"")) {
                    String packageName = getContext().getPackageName();
                    if (conf.toString().toLowerCase().contains(packageName.toLowerCase())) {
                        isOverridable = true;
                    }

                    JSObject object = new JSObject();
                    object.put("success", false);
                    object.put("message", "plugin.wifieapconfigurator.error.network.alreadyAssociated");
                    object.put("overridable", isOverridable);
                    call.success(object);
                    res = false;
                    break;
                }
            }
        //}
        return res;
    }

    boolean checkEnabledWifi(PluginCall call) {
        boolean res = true;
        WifiManager wifi = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (!wifi.isWifiEnabled()) {
            JSObject object = new JSObject();
            object.put("success", false);
            object.put("message", "plugin.wifieapconfigurator.error.wifi.disabled");
            call.success(object);
            res = false;
        }
        return res;
    }

    private Integer getEapMethod(Integer eap, PluginCall call) {
        Integer res = null;
        switch (eap) {
            case 13:
                res = WifiEnterpriseConfig.Eap.TLS;
                break;
            case 21:
                res = WifiEnterpriseConfig.Eap.TTLS;
                break;
            case 25:
                res = WifiEnterpriseConfig.Eap.PEAP;
                break;
            default:
                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.error.eap.invalid");
                call.success(object);
                res = 0;
                break;
        }
        return res;
    }

    private Integer getAuthMethod(Integer auth, PluginCall call) {
        Integer res = null;
        switch (auth) {
            case 3:
                res = WifiEnterpriseConfig.Phase2.MSCHAP;
                break;
            case 4:
                res = WifiEnterpriseConfig.Phase2.MSCHAPV2;
                break;
            case 5:
                res = WifiEnterpriseConfig.Phase2.PAP;
                break;
            case 6:
                res = WifiEnterpriseConfig.Phase2.GTC;
                break;
            default:
                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.error.auth.invalid");
                call.success(object);
                res = 0;
                break;
        }
        return res;
    }

    boolean getPermission(String permission) {
        boolean res = true;
        if (!(checkSelfPermission(getContext(), permission) == PackageManager.PERMISSION_GRANTED)) {
            res = false;
            ActivityCompat.requestPermissions(getActivity(), new String[]{permission}, 123);
        }

        return res;
    }

    private Integer getEapType(Integer eap, PluginCall call) {
        Integer res = null;
        switch (eap) {
            case 1:
                res = 13;
                break;
            case 2:
                res = 21;
                break;
            case 0:
                res = 25;
                break;
            default:
                JSObject object = new JSObject();
                object.put("success", false);
                object.put("message", "plugin.wifieapconfigurator.error.eap.invalid");
                call.success(object);
                res = 0;
                break;
        }
        return res;
    }

    public void testPasspoint(WifiManager wifiManager, String id, String displayName, String oid, WifiEnterpriseConfig enterpriseConfig, PluginCall call) {
        // TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        // String imsi = telephonyManager.getSubscriberId();
        PasspointConfiguration config = new PasspointConfiguration();
        HomeSp homeSp = new HomeSp();
        homeSp.setFqdn(id);
        if (displayName != null) {
            homeSp.setFriendlyName(displayName);
        } else {
            homeSp.setFriendlyName("geteduroam configured HS20");
        }
        String[] consortiumOIDs = oid.split(";");
        long[] roamingConsortiumOIDs = new long[consortiumOIDs.length];
        int index = 0;
        for(String roamingConsortiumOIDString : consortiumOIDs) {
            if ( !roamingConsortiumOIDString.startsWith("0x")) {
                roamingConsortiumOIDString = "0x" + roamingConsortiumOIDString;
            }
            roamingConsortiumOIDs[index] = Long.decode(roamingConsortiumOIDString);
            index++;
        }
        homeSp.setRoamingConsortiumOis(roamingConsortiumOIDs);
        config.setHomeSp(homeSp);
        Credential.SimCredential simCred = new Credential.SimCredential();
        simCred.setImsi("123456*");
        simCred.setEapType(21);
        Credential cred = new Credential();
        cred.setRealm("realm");
        cred.setSimCredential(simCred);
        config.setCredential(cred);

        // Create and install a Passpoint configuration
        PasspointConfiguration passpointConfiguration = config;
        try{
            wifiManager.addOrUpdatePasspointConfiguration(passpointConfiguration);
            JSObject object = new JSObject();
            object.put("success", true);
            object.put("message", "plugin.wifieapconfigurator.success.passpoint.linked");
        } catch (IllegalArgumentException e){
            JSObject object = new JSObject();
            object.put("success", true);
            object.put("message", "plugin.wifieapconfigurator.success.passpoint.linked");
            call.success(object);
            e.printStackTrace();
            Log.e("error", e.getMessage());
        }
    }

}
