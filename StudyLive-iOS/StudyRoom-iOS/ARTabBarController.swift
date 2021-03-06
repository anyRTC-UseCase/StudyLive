//
//  ARTabBarController.swift
//  VideoLive-iOS
//
//  Created by 余生丶 on 2021/6/18.
//

import UIKit

let PingFang = "PingFang SC"
let PingFangBold = "PingFangSC-Semibold"

class ARTabBarController: UITabBarController {

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        if #available(iOS 13.0, *) {
            let appearance = UITabBarAppearance.init()
            appearance.stackedLayoutAppearance.normal.titlePositionAdjustment = UIOffset.init(horizontal: 0, vertical: -10)
            appearance.stackedLayoutAppearance.selected.titlePositionAdjustment = UIOffset.init(horizontal: 0, vertical: -10)
            
            appearance.stackedLayoutAppearance.normal.titleTextAttributes = [
                NSAttributedString.Key.font: UIFont(name: PingFangBold, size: 14) as Any,
                NSAttributedString.Key.foregroundColor: UIColor(hexString: "#171717")
                ]
            appearance.stackedLayoutAppearance.selected.titleTextAttributes = [
                NSAttributedString.Key.font: UIFont(name: PingFangBold, size: 14) as Any,
                NSAttributedString.Key.foregroundColor: UIColor(hexString: "#FF4316")
                ]
            
            self.tabBar.standardAppearance = appearance
        } else {
            // Fallback on earlier versions
            
            UITabBarItem.appearance().setTitleTextAttributes([NSAttributedString.Key.font: UIFont(name: PingFangBold, size: 14) as Any, NSAttributedString.Key.foregroundColor: UIColor(hexString: "#171717")],for: .normal)
            UITabBarItem.appearance().setTitleTextAttributes([NSAttributedString.Key.font: UIFont(name: PingFangBold, size: 14) as Any, NSAttributedString.Key.foregroundColor: UIColor(hexString: "#FF4316")],for: .selected)
            UITabBarItem.appearance().titlePositionAdjustment = UIOffset.init(horizontal: 0, vertical: -10)
        }
    }
    
    override func tabBar(_ tabBar: UITabBar, didSelect item: UITabBarItem) {
        if item.title == "大厅" {
            // refresh
        }
    }

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */

}
