import UIKit
import Capacitor
import UserNotifications

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {
	
	var window: UIWindow?
	
	func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
		UNUserNotificationCenter.current().delegate = self
		UserDefaults.standard.set(false, forKey: "initFromNotification")
		
		return true
	}
	
	func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
		application.applicationIconBadgeNumber = 0
		UserDefaults.standard.set(true, forKey: "initFromNotification")
	}
	func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
		UserDefaults.standard.set(false, forKey: "initFromNotification")
		let vc = window?.rootViewController as! CAPBridgeViewController
		return handleOpenUrl(app, open: url)
	}
	
	func applicationWillResignActive(_ application: UIApplication) {
		// Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
		// Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
	}
	
	func applicationDidEnterBackground(_ application: UIApplication) {
		// Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
		// If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
	}
	
	func applicationWillEnterForeground(_ application: UIApplication) {
		// Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
	}
	
	func applicationDidBecomeActive(_ application: UIApplication) {
		// Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
	}
	
	func applicationWillTerminate(_ application: UIApplication) {
		// Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
	}
	
	func handleOpenUrl(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
		// Called when the app was launched with a url. Feel free to add additional processing here,
		// but if you want the App API to support tracking app url opens, make sure to keep this call
		return CAPBridge.handleOpenUrl(url, options)
	}
	
	func application(_ application: UIApplication, continue userActivity: NSUserActivity, restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {
		// Called when the app was launched with an activity, including Universal Links.
		// Feel free to add additional processing here, but if you want the App API to support
		// tracking app url opens, make sure to keep this call
		return CAPBridge.handleContinueActivity(userActivity, restorationHandler)
	}
	
	override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
		super.touchesBegan(touches, with: event)
		let statusBarRect = UIApplication.shared.statusBarFrame
		guard let touchPoint = event?.allTouches?.first?.location(in: self.window) else { return }
		if statusBarRect.contains(touchPoint) {
			NotificationCenter.default.post(CAPBridge.statusBarTappedNotification)
		}
	}
	
	#if USE_PUSH
	
	func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
		NotificationCenter.default.post(name: Notification.Name(CAPNotifications.DidRegisterForRemoteNotificationsWithDeviceToken.name()), object: deviceToken)
	}
	
	func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
		NotificationCenter.default.post(name: Notification.Name(CAPNotifications.DidFailToRegisterForRemoteNotificationsWithError.name()), object: error)
	}
	
	#endif
	
}

extension AppDelegate: UNUserNotificationCenterDelegate{
	
	// This function will be called when the app receive notification
	func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
		// show the notification alert (banner), and with sound
		completionHandler([.alert, .sound, .badge])
	}
	
	// This function will be called right after user tap on the notification
	func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
		// Remove badge icon
		UIApplication.shared.applicationIconBadgeNumber = 0
		
		let dateExpired = UserDefaults.standard.object(forKey: "expireDate") as! Date
		let ssidExpired = UserDefaults.standard.array(forKey: "ssidToExpire") as! [String]
		let domainExpired = UserDefaults.standard.array(forKey: "domainToExpire") as! [String]
		let dateNow = Date()
		
		if (ssidExpired.count > 0 && dateNow > dateExpired){
			//Remove
			print("Remove connections: ", domainExpired)
			print("Remove connections: ", ssidExpired)
		}
		else {
			// Set navigation to configured profile
			UserDefaults.standard.set(true, forKey: "initFromNotification")
			// State foreground
			if(UIApplication.shared.applicationState == .active){}
			// State background
			if(UIApplication.shared.applicationState == .inactive){}
			
		
		}
		// tell the app that we have finished processing the user’s action / response
		completionHandler()
	}
}

