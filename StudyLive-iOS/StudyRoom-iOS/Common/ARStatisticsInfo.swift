//
//  ARStatisticsInfo.swift
//  AudioLive-iOS
//
//  Created by 余生丶 on 2021/3/2.
//

import UIKit
import SwiftyJSON
import ARtcKit
import SVProgressHUD

enum ARLeaveReason {
    case normal
    case broadcastOffline
    case timeOut
    case tokenExpire
}

struct ARMainRoomListModel {
    var roomId: String?
    var roomName: String?
    var avatarStr: String?
    var avatars = [String]()
    
    init(jsonData: JSON) {
        roomId = jsonData["roomId"].stringValue
        roomName = jsonData["roomName"].stringValue
        avatarStr = jsonData["userNum"].string
        for object in jsonData["avatars"].arrayValue {
            avatars.append(object.stringValue)
        }
    }
}

struct ARUserModel {
    var uid: String?
    var userName: String?
    var avatar: String?
    
    init(jsonData: JSON) {
        uid = jsonData["uid"].stringValue
        userName = jsonData["userName"].stringValue
        avatar = jsonData["avatar"].stringValue
    }
}

struct ARRoomInfoModel {
    var roomId: String?
    var roomName: String?
    var rtcToken: String?
    var rtmToken: String?
    
    init(jsonData: JSON) {
        roomName = jsonData["roomName"].stringValue
        roomId = jsonData["roomId"].stringValue
        rtcToken = jsonData["rtcToken"].stringValue
        rtmToken = jsonData["rtmToken"].stringValue
    }
}

class ARReportItem: NSObject {
    var title: String?
    var isSelected: Bool = false
    
    init(text: String, selected: Bool) {
        title = text
        isSelected = selected
    }
}

struct ARMicModel {
    var avatar: String?
    var seatTime: NSInteger = 0
    var uid: String?
    var seat: NSInteger = 0
    var userName: String?
    
    init(jsonData: JSON) {
        avatar = jsonData["avatar"].stringValue
        seatTime = jsonData["seatTime"].intValue
        uid = jsonData["uid"].stringValue
        seat = jsonData["seat"].intValue
        userName = jsonData["userName"].stringValue
    }
}

extension NSObject {
    
    func registered() {
        //注册
        SVProgressHUD.show(UIImage(named: "icon_loading")!, status: "载入中")
        let parameters : NSDictionary = ["sex": 0, "userName": randomCharacter(length: 6)]
        ARNetWorkHepler.getResponseData("signUp", parameters: parameters as? [String : AnyObject], headers: false, success: { [weak self] (result) in
            let uid: String = result["data"]["uid"].stringValue
            UserDefaults.set(value: uid , forKey: .uid)
            self?.login()
        }) { (error) in
            print(error)
        }
    }
    
    func login() {
        // 登录
        SVProgressHUD.show(UIImage(named: "icon_loading")!, status: "载入中")
        if UserDefaults.string(forKey: .uid)?.count ?? 0 > 0 {
            UserDefaults.set(value: "false" , forKey: .isLogin)
            let parameters : NSDictionary = ["cType": 2, "uid": UserDefaults.string(forKey: .uid) as Any, "pkg": Bundle.main.infoDictionary!["CFBundleIdentifier"] as Any]
            ARNetWorkHepler.getResponseData("signIn", parameters: parameters as? [String : AnyObject], headers: false, success: { [weak self](result) in
                if result["code"] == 0 {
                    UserDefaults.set(value: result["data"]["avatar"].stringValue , forKey: .avatar)
                    UserDefaults.set(value: result["data"]["userName"].stringValue , forKey: .userName)
                    UserDefaults.set(value: result["data"]["userToken"].stringValue , forKey: .userToken)
                    UserDefaults.set(value: result["data"]["appid"].stringValue , forKey: .appid)
                    UserDefaults.set(value: "true" , forKey: .isLogin)
                    NotificationCenter.default.post(name: UIResponder.studyRoomNotificationLoginSucess, object: self, userInfo: nil)
                    SVProgressHUD.dismiss(withDelay: 0.5)
                } else if result["code"] == 1000 {
                    // 用户不存在
                    self?.registered()
                }
            }) { (error) in
                print(error)
            }
        } else {
            registered()
        }
    }
    
    func leaveRoom(roomId: String) {
        //离开房间
        let parameters : NSDictionary = ["roomId": roomId as Any]
        ARNetWorkHepler.getResponseData("leaveRoom", parameters: parameters as? [String : AnyObject], headers: true, success: { (result) in
            
        }) { (error) in
            print(error)
        }
    }
}
