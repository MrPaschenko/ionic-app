import Foundation
import Capacitor
import NetworkExtension
import SystemConfiguration.CaptiveNetwork
import UIKit
import CoreLocation

@objc(WifiEapConfigurator)
public class WifiEapConfigurator: CAPPlugin {
    
    func getAuthType(authType : Int) -> NEHotspotEAPSettings.TTLSInnerAuthenticationType? {
        switch authType {
        case 3:
            return .eapttlsInnerAuthenticationMSCHAP
        case 4:
            return .eapttlsInnerAuthenticationMSCHAPv2
        case 5:
            return .eapttlsInnerAuthenticationPAP
        default:
            return nil
        }
    }
    
    func getEAPType(eapType: Int) -> NSNumber? {
        switch eapType {
        case 43:
            return NSNumber(value: NEHotspotEAPSettings.EAPType.EAPFAST.rawValue)
        case 25:
            return NSNumber(value: NEHotspotEAPSettings.EAPType.EAPPEAP.rawValue)
        case 13:
            return NSNumber(value: NEHotspotEAPSettings.EAPType.EAPTLS.rawValue)
        case 21:
            return NSNumber(value: NEHotspotEAPSettings.EAPType.EAPTTLS.rawValue)
        default:
            return nil
        }
    }
    
    func resetKeychain() {
        deleteAllKeysForSecClass(kSecClassGenericPassword)
        deleteAllKeysForSecClass(kSecClassInternetPassword)
        deleteAllKeysForSecClass(kSecClassCertificate)
        deleteAllKeysForSecClass(kSecClassKey)
        deleteAllKeysForSecClass(kSecClassIdentity)
    }

    func deleteAllKeysForSecClass(_ secClass: CFTypeRef) {
        let dict: [NSString : Any] = [kSecClass : secClass]
        let result = SecItemDelete(dict as CFDictionary)
        assert(result == noErr || result == errSecItemNotFound, "Error deleting keychain data (\(result))")
    }
    
    @objc func configureAP(_ call: CAPPluginCall) {
        var ssid = call.getString("ssid")
            if call.getString("oid") != nil && call.getString("oid") != "" {
                ssid = ""
                // Do nothing, in iOS the ssid is not mandatory like in Android when HS20 configuration exists
            } else {
                if self.currentSSIDs().first == "" {
                    return call.success([
                        "message": "plugin.wifieapconfigurator.error.ssid.missing",
                        "success": false,
                    ])
                }
            }
   
        
        guard let eapType = call.get("eap", Int.self) else {
            return call.success([
                "message": "plugin.wifieapconfigurator.error.auth.invalid",
                "success": false,
            ])
        }
        
        
        resetKeychain()
        
        let eapSettings = NEHotspotEAPSettings()
        eapSettings.isTLSClientCertificateRequired = true
        eapSettings.supportedEAPTypes = [getEAPType(eapType: eapType)!]
        
        
        if let server = call.getString("servername"){
            if server != ""{
                //eapSettings.trustedServerNames = [server]
                // supporting multiple CN
                var serverNames: [String]? = server.components(separatedBy: ";")
                eapSettings.trustedServerNames = serverNames!
            }
        }
        
        if let anonymous = call.getString("anonymous") {
            if anonymous != ""{
                eapSettings.outerIdentity = anonymous
            }
        }
        
        var username:String? = nil
        var password:String? = nil
        var authType:Int? = nil
        
        var certificates: [SecCertificate]? = nil
        if let clientCertificate = call.getString("clientCertificate"){
            if let passPhrase = call.getString("passPhrase"){
                if let queries = addServerCertificate(certificate: clientCertificate, passPhrase: passPhrase) {
                    certificates = []
                    
                    for (index, query) in ((queries as? [[String: Any]])?.enumerated())! {
                        
                        var item: CFTypeRef?
                        let statusCertificate = SecItemCopyMatching(query as CFDictionary, &item)
                        
                        guard statusCertificate == errSecSuccess else {
                            return call.success([
                                "message": "plugin.wifieapconfigurator.error.clientIdentity.missing",
                                "success": false,
                            ])
                        }
                        
                        let certificate = item as! SecCertificate
                        
                        certificates?.append(certificate)
                        
                    }
                    
                    eapSettings.setTrustedServerCertificates(certificates!)
                }
                if let identity = addClientCertificate(certName: "client" + ssid!, certificate: clientCertificate, password: passPhrase)
                {
                    let id = identity as! SecIdentity
                    eapSettings.setIdentity(id)
                    eapSettings.isTLSClientCertificateRequired = true
                }
            }else{
                return call.success([
                    "message": "plugin.wifieapconfigurator.error.passPhrase.missing",
                    "success": false,
                ])
            }
        }
        else{
            if call.getString("username") != nil && call.getString("username") != ""{
                username = call.getString("username")
            } else {
                return call.success([
                    "message": "plugin.wifieapconfigurator.error.username.missing",
                    "success": false,
                ])
            }
            
            if call.getString("password") != nil && call.getString("password") != ""{
                password = call.getString("password")
            }
            else{
                return call.success([
                    "message": "plugin.wifieapconfigurator.error.password.missing",
                    "success": false,
                ])
            }
            
            if call.getInt("auth") != nil{
                authType = call.getInt("auth")
            }
            else{
                return call.success([
                    "message": "plugin.wifieapconfigurator.error.auth.missing",
                    "success": false,
                ])
            }
            
            eapSettings.username = username ?? ""
            eapSettings.password = password ?? ""
            eapSettings.ttlsInnerAuthenticationType = self.getAuthType(authType: authType ?? 0)!
        }
        
        if call.getString("caCertificate") != nil && call.getString("caCertificate") != "" {
            if let certificatesString = call.getString("caCertificate") {
                // supporting multiple CAs
                let certificatesStrings = certificatesString.components(separatedBy: ";")
                var index: Int = 0
                var certificates = [SecCertificate]();
                certificatesStrings.forEach { caCertificateString in
                    // building the name for the cert that will be installed
                    let certName: String = "getEduroamCertCA" + String(index);
                    // adding the certificate
                    if (addCertificate(certName: certName, certificate: caCertificateString) as? Bool ?? false)
                    {
                        let getquery: [String: Any] = [kSecClass as String: kSecClassCertificate,
                                                       kSecAttrLabel as String: certName,
                                                       kSecReturnRef as String: kCFBooleanTrue]
                        var item: CFTypeRef?
                        let status = SecItemCopyMatching(getquery as CFDictionary, &item)
                        guard status == errSecSuccess else { return }
                        let savedCert = item as! SecCertificate
                        certificates.append(savedCert);
                    }
                    else {
                        // return call.success(addCertificate(certName: certName, certificate: caCertificateString) as! Dictionary<String, AnyObject>)
                    }
                    index += 1
                }
                eapSettings.setTrustedServerCertificates(certificates)
            }
        }

        // HS20 support
        var oid = call.getString("oid")
        var id = call.getString("id")
        var displayName = call.getString("displayName")
        
        if call.getString("oid") != nil && call.getString("oid") != ""{
            oid = call.getString("oid")
        }
        if call.getString("id") != nil && call.getString("id") != ""{
            id = call.getString("id")
        }
        if call.getString("displayName") != nil && call.getString("displayName") != ""{
            displayName = call.getString("displayName")
        }
        var config:NEHotspotConfiguration
        // If HS20 was enabled
        if oid != nil && oid != ""{
            var oidStrings: [String]?
            oidStrings = oid?.components(separatedBy: ";")
            // HS20 object settings
            let hs20 = NEHotspotHS20Settings(
                domainName: id ?? "",
                roamingEnabled: true)
            hs20.roamingConsortiumOIs = oidStrings ?? [""];
            config = NEHotspotConfiguration(hs20Settings: hs20, eapSettings: eapSettings)
        } else {
            config = NEHotspotConfiguration(ssid: ssid ?? "", eapSettings: eapSettings)
        }
        // this line is needed in iOS 13 because there is a reported bug with iOS 13.0 until 13.1.0
        config.joinOnce = false
        config.lifeTimeInDays = NSNumber(integerLiteral: 825)

        let options: [String: NSObject] = [kNEHotspotHelperOptionDisplayName : "Join our WIFI" as NSObject]
        let queue: DispatchQueue = DispatchQueue(label: "com.emergya.eduroam", attributes: DispatchQueue.Attributes.concurrent)

        NSLog("Started wifi list scanning.")

        NEHotspotHelper.register(options: options, queue: queue) { (cmd: NEHotspotHelperCommand) in
          NSLog("Received command: \(cmd.commandType.rawValue)")
        }
        NEHotspotConfigurationManager.shared.apply(config) { [weak self] (error) in
                    print("error is \(String(describing: error))")
                    if let error = error {
                        let nsError = error as NSError
                        if nsError.domain == "NEHotspotConfigurationErrorDomain" {
                            if let configError = NEHotspotConfigurationError(rawValue: nsError.code) {
                                switch configError {
                                case .invalidWPAPassphrase:
                                    print("password error: \(error.localizedDescription)")
                                case .invalid, .invalidSSID, .invalidWEPPassphrase,
                                     .invalidEAPSettings, .invalidHS20Settings, .invalidHS20DomainName, .userDenied, .pending, .systemConfiguration, .unknown, .joinOnceNotSupported, .alreadyAssociated, .applicationIsNotInForeground, .internal:
                                    print("other error: \(error.localizedDescription)")
                                @unknown default:
                                    print("later added error: \(error.localizedDescription)")
                                }
                            }
                        } else {
                            print("some other error: \(error.localizedDescription)")
                        }
                    } else {
                        print("perhaps connected")

                        self?.printWifiInfo()
                    }
                }

            }

            @IBAction func onInfo(_ sender: Any) {
                self.printWifiInfo()
            }

            private func printWifiInfo() {
                print("printWifiInfo:")
                if let wifi = self.getConnectedWifiInfo() {
                    if let connectedSSID = wifi["SSID"] {
                        print("we are currently connected with \(connectedSSID)")
                    }
                    print("further info:")
                    for (k, v) in wifi {
                        print(".  \(k) \(v)")
                    }
                }
                print()
            }

            private func getConnectedWifiInfo() -> [AnyHashable: Any]? {
                if let ifs = CFBridgingRetain( CNCopySupportedInterfaces()) as? [String],
                    let ifName = ifs.first as CFString?,
                    let info = CFBridgingRetain( CNCopyCurrentNetworkInfo((ifName))) as? [AnyHashable: Any] {
                        return info
                    }
                return nil
            }

    @objc func isNetworkAssociated(_ call: CAPPluginCall) {
        guard let ssidToCheck = call.getString("ssid") else {
            return call.success([
                "message": "plugin.wifieapconfigurator.error.ssid.missing",
                "success": false,
            ])
        }
        
        
        var iterator = false
        NEHotspotConfigurationManager.shared.getConfiguredSSIDs { (ssids) in
            for ssid in ssids {
                if ssidToCheck == ssid {
                    iterator = true
                }
            }
            
            if !iterator && ssids.count < 1 {
                call.success([
                    "message": "plugin.wifieapconfigurator.error.network.noNetworksFound",
                    "success": false,
                    "overridable": true
                ])
            }
                
            else if(iterator){
                call.success([
                    "message": "plugin.wifieapconfigurator.error.network.alreadyAssociated",
                    "success": false,
                    "overridable": true
                ])
            }else{
                call.success([
                    "message": "plugin.wifieapconfigurator.success.network.missing",
                    "success": true
                ])
            }
        }
    }
    
    
    @objc func removeNetwork(_ call: CAPPluginCall) {
        guard let ssidToCheck = call.getString("ssid") else {
            return call.success([
                "message": "plugin.wifieapconfigurator.error.ssid.missing",
                "success": false,
            ])
        }
        var foundNetwork = false
        NEHotspotConfigurationManager.shared.getConfiguredSSIDs { (ssids) in
            for ssid in ssids {
                if ssidToCheck == ssid {
                    foundNetwork = true
                }
            }
            
            if foundNetwork {
                NEHotspotConfigurationManager.shared.removeConfiguration(forSSID: ssidToCheck)
                var removed = true
                NEHotspotConfigurationManager.shared.getConfiguredSSIDs { (ssids) in
                    for ssid in ssids {
                        if ssidToCheck == ssid {
                            removed = false
                        }
                    }
                    
                    let message = removed ?  "plugin.wifieapconfigurator.success.network.removed": "plugin.wifieapconfigurator.error.network.removed"
                    call.success([
                        "message": message,
                        "success": removed
                    ])
                }
            }
            else{
                call.success([
                    "message": "plugin.wifieapconfigurator.error.network.notFound",
                    "success": false
                ])
            }
        }
        
    }
    
    func currentSSIDs() -> [String] {
         guard let interfaceNames = CNCopySupportedInterfaces() as? [String] else {
             return []
         }
         return interfaceNames.compactMap { name in
             guard let info = CNCopyCurrentNetworkInfo(name as CFString) as? [String:AnyObject] else {
                 return nil
             }
             guard let ssid = info[kCNNetworkInfoKeySSID as String] as? String else {
                 return nil
             }
            return ssid
         }
     }
    
    func cleanCertificate(certificate: String) -> String{
        let certDirty = certificate
        
        let certWithoutHeader = certDirty.replacingOccurrences(of: "-----BEGIN CERTIFICATE-----\n", with: "")
        let certWithoutBlankSpace = certWithoutHeader.replacingOccurrences(of: "\n", with: "")
        let certClean = certWithoutBlankSpace.replacingOccurrences(of: "-----END CERTIFICATE-----", with: "")
        
        return certClean
    }
    
    
    func addCertificate(certName: String, certificate: String) -> Any? {
        let certBase64 = certificate
        var error: Unmanaged<CFError>?
        let attributesRSAPub: [String:Any] = [
            kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
            kSecAttrKeyClass as String: kSecAttrKeyClassPublic,
            kSecAttrKeySizeInBits as String: 2048,
            kSecAttrIsPermanent as String: false
        ]
        if var data = Data(base64Encoded: certBase64, options: Data.Base64DecodingOptions.ignoreUnknownCharacters) {
            let publicKeySec = SecKeyCreateWithData(data as CFData, attributesRSAPub as CFDictionary, &error)
            if let certRef = SecCertificateCreateWithData(kCFAllocatorDefault, data as CFData) {
                let addquery: [String: Any] = [kSecClass as String: kSecClassCertificate,
                                               kSecValueRef as String: certRef,
                                               kSecAttrLabel as String: certName]
                let status = SecItemAdd(addquery as CFDictionary, nil)
                guard status == errSecSuccess else {
                    if status == errSecDuplicateItem {
                        let getquery: [String: Any] = [kSecClass as String: kSecClassCertificate,
                                                       kSecAttrLabel as String: certName,
                                                       kSecReturnRef as String: kCFBooleanTrue]
                        var item: CFTypeRef?
                        let status = SecItemCopyMatching(getquery as CFDictionary, &item)
                        let statusDelete = SecItemDelete(getquery as CFDictionary)
                        guard statusDelete == errSecSuccess || status == errSecItemNotFound else { return false }
                    }
                    return false
                }
                
                if status == errSecSuccess {
                    return true
                }
                else {
                    return false
                }
            } else {
                return [
                    "message": "plugin.wifieapconfigurator.error.network.certificateRefConvertionFailed",
                    "success": false,
                ]
            }
        } else {
            return [
                "message": "plugin.wifieapconfigurator.error.network.certificateDataFailed",
                "success": false,
            ]
        }
    }
    
    func addServerCertificate(certificate: String, passPhrase: String) -> Any? {
        let options = [ kSecImportExportPassphrase as String: passPhrase ]
        var rawItems: CFArray?
        let certBase64 = certificate
        /*If */let data = Data(base64Encoded: certBase64)!
        
        let statusImport = SecPKCS12Import(data as CFData, options as CFDictionary, &rawItems)
        guard statusImport == errSecSuccess else { return false }
        let items = rawItems! as! Array<Dictionary<String, Any>>
        let firstItem = items[0]
        let identity = firstItem[kSecImportItemIdentity as String] as! SecIdentity?
        let trust = firstItem[kSecImportItemTrust as String] as! SecTrust?
        if let chain = firstItem[kSecImportItemCertChain as String] as! [SecCertificate]? {
            var certificateQueries : [[String: Any]] = []
            
            for (index, cert) in chain.enumerated() {
                
                let certData = SecCertificateCopyData(cert) as Data
                
                if let certificateData = SecCertificateCreateWithData(nil, certData as CFData) {
                    let addquery: [String: Any] = [kSecClass as String: kSecClassCertificate,
                                                   kSecValueRef as String:  certificateData,
                                                   kSecAttrLabel as String: "getEduroamCertificate" + "\(index)"]
                    
                    let statusUpload = SecItemAdd(addquery as CFDictionary, nil)
                    
                    guard statusUpload == errSecSuccess else {
                        if statusUpload == errSecDuplicateItem {
                            let getquery: [String: Any] = [kSecClass as String: kSecClassCertificate,
                                                           kSecAttrLabel as String: "getEduroamCertificate" + "\(index)",
                                kSecReturnRef as String: kCFBooleanTrue]
                            
                            var item: CFTypeRef?
                            let status = SecItemCopyMatching(getquery as CFDictionary, &item)
                            let statusDelete = SecItemDelete(getquery as CFDictionary)
                            guard statusDelete == errSecSuccess || status == errSecItemNotFound else { return false }
                            return addServerCertificate(certificate: certificate, passPhrase: passPhrase)
                            
                        }
                        return false
                    }
                    
                    certificateQueries.append(
                        [kSecClass as String: kSecClassCertificate,
                         kSecAttrLabel as String: "getEduroamCertificate" + "\(index)",
                            kSecReturnRef as String: kCFBooleanTrue])
                }
            }
            
            
            return certificateQueries
            
        }
        
        return nil
    }
    
    func addClientCertificate(certName: String, certificate: String, password: String) -> Any? {
        
        let options = [ kSecImportExportPassphrase as String: password ]
        var rawItems: CFArray?
        let certBase64 = certificate
        /*If */let data = Data(base64Encoded: certBase64)!
        let statusImport = SecPKCS12Import(data as CFData,
                                           options as CFDictionary,
                                           &rawItems)
        guard statusImport == errSecSuccess else { return false }
        let items = rawItems! as! Array<Dictionary<String, Any>>
        let firstItem = items[0]
        if let identity = firstItem[kSecImportItemIdentity as String] as! SecIdentity? {
            
            let addquery: [String: Any] = [kSecValueRef as String: identity,
                                           kSecAttrLabel as String: certName]
            let status = SecItemAdd(addquery as CFDictionary, nil)
            guard status == errSecSuccess else {
                if status == errSecDuplicateItem {
                    let getquery: [String: Any] = [kSecValueRef as String: identity,
                                                   kSecAttrLabel as String: certName,
                                                   kSecReturnRef as String: kCFBooleanTrue]
                    var item: CFTypeRef?
                    let status = SecItemCopyMatching(getquery as CFDictionary, &item)
                    let statusDelete = SecItemDelete(getquery as CFDictionary)
                    guard statusDelete == errSecSuccess || status == errSecItemNotFound else { return false }
                    return addClientCertificate(certName: certName, certificate: certificate, password: password)
                }
                
                return false
            }
            
            return identity
            
        }
        
        return nil
    }
    
    @objc func isConnectedSSID(_ call: CAPPluginCall) {
        let infoNetwork = SSID.fetchNetworkInfo()
        guard infoNetwork?.first?.ssid != "" else {
            return call.success([
                "message": "plugin.wifieapconfigurator.error.ssid.missing",
                "success": false,
            ])
        }
        guard let interfaceNames = CNCopySupportedInterfaces() as? [String] else {
            return call.success([
                "message": "plugin.wifieapconfigurator.error.network.notConnected",
                "success": false,
                "isConnected": false
            ])
        }
        
        for i in 0...interfaceNames.count {
            let test = interfaceNames[i] as String;
            guard (test == infoNetwork?.first?.ssid)  else {
                return call.success([
                    "message": "plugin.wifieapconfigurator.error.network.notConnected",
                    "success": false,
                    "isConnected": false
                ])
            }
            guard (test != infoNetwork?.first?.ssid) else {
                return call.success([
                    "message": "plugin.wifieapconfigurator.error.network.notConnected",
                    "success": false,
                    "isConnected": false
                ])
            }
            return call.success([
                "message": "plugin.wifieapconfigurator.success.network.connected",
                "success": true,
                "isConnected": true
            ])
        }
    }
}

extension Error {
    var code: Int { return (self as NSError).code }
    var domain: String { return (self as NSError).domain }
}

extension String {
    func base64Encoded() -> String? {
        return data(using: .utf8)?.base64EncodedString()
    }
    
    func base64Decoded() -> String? {
        guard let data = Data(base64Encoded: self) else { return nil }
        return String(data: data, encoding: .utf8)
    }
}

class ViewController: UIViewController, CLLocationManagerDelegate {
    
    var locationManager = CLLocationManager()
    var currentNetworkInfos: Array<NetworkInfo>? {
        get {
            return SSID.fetchNetworkInfo()
        }
    }
    @IBOutlet weak var ssidLabel: UILabel!
    @IBOutlet weak var bssidLabel: UILabel!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        if #available(iOS 13.0, *) {
            let status = CLLocationManager.authorizationStatus()
            if status == .authorizedWhenInUse {
                updateWiFi()
            } else {
                locationManager.delegate = self
                locationManager.requestWhenInUseAuthorization()
            }
        } else {
            updateWiFi()
        }
    }
    func getSSID() -> String {
        return String(currentNetworkInfos?.first?.ssid ?? "")
    }
    
    func getBSSID() -> String {
        return String(currentNetworkInfos?.first?.bssid ?? "")
    }
    
    func updateWiFi() -> NetworkInfo? {

        // ssidLabel.text = currentNetworkInfos?.first?.ssid
        // bssidLabel.text = currentNetworkInfos?.first?.bssid
        return currentNetworkInfos?.first
    }
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        if status == .authorizedWhenInUse {
            updateWiFi()
        }
    }
    
}

public class SSID {
    class func fetchNetworkInfo() -> [NetworkInfo]? {
        if let interfaces: NSArray = CNCopySupportedInterfaces() {
            var networkInfos = [NetworkInfo]()
            for interface in interfaces {
                let interfaceName = interface as! String
                var networkInfo = NetworkInfo(interface: interfaceName,
                                              success: false,
                                              ssid: nil,
                                              bssid: nil)
                if let dict = CNCopyCurrentNetworkInfo(interfaceName as CFString) as NSDictionary? {
                    networkInfo.success = true
                    networkInfo.ssid = dict[kCNNetworkInfoKeySSID as String] as? String
                    networkInfo.bssid = dict[kCNNetworkInfoKeyBSSID as String] as? String
                }
                networkInfos.append(networkInfo)
            }
            return networkInfos
        }
        return nil
    }
}


struct NetworkInfo {
    var interface: String
    var success: Bool = false
    var ssid: String?
    var bssid: String?
}
