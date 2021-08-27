//
//  AppDelegate.swift
//  StudyRoom-iOS
//
//  Created by 余生丶 on 2021/7/30.
//

import UIKit
import SVProgressHUD

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?
    var allowRotation = false
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.
        SVProgressHUD.setDefaultStyle(.light)
        SVProgressHUD.setDefaultMaskType(.black)
        SVProgressHUD.setShouldTintImages(false)
        SVProgressHUD.setMinimumSize(CGSize.init(width: 120, height: 120))
        
        Thread.sleep(forTimeInterval: 0.5)
        return true
    }
    
    func application(_ application: UIApplication, supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
        return allowRotation ? .landscapeRight : .portrait
    }

}

