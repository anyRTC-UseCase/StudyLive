//
//  ARVideoViewController.swift
//  StudyRoom-iOS
//
//  Created by 余生丶 on 2021/8/4.
//

import UIKit
import ARtcKit
import ARtmKit
import SwiftyJSON
import SVProgressHUD
import AttributedString

var infoVideoModel: ARRoomInfoModel!
var rtcKit: ARtcEngineKit!
var rtmEngine: ARtmKit!

class ARVideoViewController: ARBaseViewController {
    @IBOutlet weak var roomLabel: UILabel!
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var roomTextField: UITextField!
    @IBOutlet weak var bottomStackView: UIStackView!        /* 底部工具栏 */
    @IBOutlet weak var joinButton: UIButton!               /* 上麦 button */
    @IBOutlet weak var exitButton: UIButton!
    @IBOutlet weak var containerView: ARPopContainerView!   /* 进出提示框 */
    @IBOutlet weak var memberView: UIView!                /* 观众列表 */
    @IBOutlet weak var audienceStackView: UIStackView!     /* 观众头像 */
    @IBOutlet weak var audienceNumberLabel: UILabel!
    @IBOutlet weak var audioButton: UIButton!             /* 音频 button, 默认关闭 */
    @IBOutlet weak var muteAllAudioButton: UIButton!       /* 全员静音 */
    
    @IBOutlet weak var stackView0: UIStackView!           /* 1号麦位、2号麦位 */
    @IBOutlet weak var stackView1: UIStackView!           /* 3号麦位、4号麦位 */
    @IBOutlet weak var stackViewHeight0: NSLayoutConstraint!
    @IBOutlet weak var stackViewHeight1: NSLayoutConstraint!
    @IBOutlet weak var containerWidth: NSLayoutConstraint!
    @IBOutlet weak var audienceListConstraint: NSLayoutConstraint!
    @IBOutlet weak var topPadding: NSLayoutConstraint!
    @IBOutlet weak var bottomPadding: NSLayoutConstraint!
    weak var logVC: LogViewController?
    
    var allowRotation = false
    var videoArr = [ARVideoView]()
    var largeIndex: NSInteger = 0
    var broadCastArr = [ARMicModel]()
    var audienceArr = [ARMicModel]()
    var localMicModel: ARMicModel?
    var allMute: Bool = false
    var localMute: Bool = true
    private var destroy: Bool = false
    private var reason: ARLeaveReason = .normal
    /* all 音频状态 */
    lazy var broadCastAudioDic: NSMutableDictionary = {
        return NSMutableDictionary()
    }()
    
    fileprivate var rtmChannel: ARtmChannel?

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        initializeUI()
        initializeEngine()
        joinChannel()
    }
    
    private func initializeUI() {
        roomLabel.attributed.text = .init("""
          \(infoVideoModel.roomName!) \(.image(#imageLiteral(resourceName: "icon_report"), .custom(size: CGSize(width: 18, height: 18))), action: reportRoom)
         """)
        titleLabel.attributed.text = .init("""
         \(infoVideoModel.roomName!, .font(UIFont(name: PingFangBold, size: 16)!), .foreground(UIColor.white)) \(.image(#imageLiteral(resourceName: "icon_report"), .custom(size: CGSize(width: 18, height: 18))), action: reportRoom)
         """)

        roomTextField.rightView = rightButton
        roomTextField.rightViewMode = .always
        
        for index in 0...3 {
            var videoView: ARVideoView!
            videoView = ARVideoView.videoView(location: index + 1)
            (index < 2) ? (stackView0.addArrangedSubview(videoView)) : (stackView1.addArrangedSubview(videoView))
            videoView.callback = { [weak self](location, free) in
                if free {
                    if self?.localMicModel?.seat == 0 {
                        self?.applyMic()
                    }
                } else {
                    self?.swicthVideo(location: location)
                }

            }
            videoArr.append(videoView)
        }
        
        self.stackViewHeight0.constant = ARScreenWidth/2 * 0.688
        self.stackViewHeight1.constant = ARScreenWidth/2 * 0.688
        
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(enterMemberVc))
        memberView.addGestureRecognizer(tapGesture)
        
        ARSourceTimer.start(1) { [weak self] (time) in
            guard let weakSelf = self else { return }
            for object in weakSelf.videoArr {
                let video: ARVideoView = object
                if video.micModel != nil {
                    video.timeLabel.text = transToHourMinSec(time: Float(Int(Date().timeIntervalSince1970) - video.micModel!.seatTime))
                }
            }
        }
        
        getUserList(uType: 1)
        getUserList(uType: 2)
        updateJoinButtonState()
    }
    
    private func initializeEngine() {
        // init ARtcEngineKit
        rtcKit = ARtcEngineKit.sharedEngine(withAppId: UserDefaults.string(forKey: .appid)!, delegate: self)
        rtcKit.setChannelProfile(.liveBroadcasting)
        rtcKit.enableVideo()
        
        // init ARtmKit
        rtmEngine = ARtmKit.init(appId: UserDefaults.string(forKey: .appid)!, delegate: self)
        rtmEngine.login(byToken: infoVideoModel.rtmToken, user: UserDefaults.string(forKey: .uid) ?? "0") { [weak self](errorCode) in
            guard let weakself = self else {return}
            weakself.rtmChannel = rtmEngine.createChannel(withId: infoVideoModel.roomId!, delegate: self)
            weakself.rtmChannel?.join(completion: { (errorCode) in
                let dic: NSDictionary! = ["cmd": "enterTip", "userName": UserDefaults.string(forKey: .userName) as Any, "avatar": UserDefaults.string(forKey: .avatar) as Any]
                weakself.sendChannelMessage(text: weakself.getJSONStringFromDictionary(dictionary: dic))
            })
        }
    }
    
    func joinChannel() {
        let uid = UserDefaults.string(forKey: .uid)
        rtcKit.joinChannel(byToken: infoVideoModel.rtcToken, channelId: infoVideoModel.roomId!, uid: uid) {(channel, uid, elapsed) in
            print("joinChannel sucess")
        }
    }
    
    func leaveChannel() {
        rtcKit.leaveChannel { (stats) in
            print("leaveChannel")
        }
    }
    
    @IBAction func didClickVideoButton(_ sender: UIButton) {
        sender.isSelected.toggle()
        
        if sender.tag == 50 {
            // 音频
            if !allMute || localMicModel?.seat == 1 {
                localMute = !sender.isSelected
                rtcKit.muteLocalAudioStream(localMute)
                for object in videoArr {
                    let video = object
                    if video.uid == UserDefaults.string(forKey: .uid) {
                        video.audioImageView.image = UIImage(named: sender.isSelected ? "icon_audio_open" : "icon_audio_close")
                        break
                    }
                }
                
            } else {
                // 全员禁言
                sender.isSelected.toggle()
                showToast(text: "主持人已开启全员静音", image: "icon_tip_warning")
            }
            
        } else if sender.tag == 51 {
            // 全体静音
            addOrUpdateChannel(key: "allAudioState", value: sender.isSelected ? "1" : "0")
            
        } else if sender.tag == 52 {
            // 旋转摄像头
            rtcKit.switchCamera()
        } else if sender.tag == 53 {
            // 上麦
            sender.isSelected = false
            if broadCastArr.count < 4 {
                applyMic()
            } else {
                showToast(text: "麦位已满，请稍后重试", image: "icon_tip_warning")
            }
        } else if sender.tag == 54 {
            // 离开
            if localMicModel?.seat != 0 {
                UIAlertController.showAlert(in: self, withTitle: "退出自习室", message: "是否退出", cancelButtonTitle: "取消", destructiveButtonTitle: nil, otherButtonTitles: ["确认"]) { [unowned self] (alertVc, action, index) in
                    if index == 2 {
                        self.reason = .normal
                        self.destroyRoom()
                    }
                }
            } else {
                reason = .normal
                destroyRoom()
            }
        }
    }
    
    func swicthVideo(location: NSInteger) {
        if location == largeIndex {
            if !allowRotation {
                stackView0.addArrangedSubview(stackView1.subviews[2])
                largeIndex = 0
                
                self.stackViewHeight0.constant = ARScreenWidth/2 * 0.688
                self.stackViewHeight1.constant = ARScreenWidth/2 * 0.688
            }
        } else {
            var defaultIndex = 0
            var defaultVideo: ARVideoView?
            videoArr.forEach { (video) in
                if video == videoArr[location - 1] {
                    if !stackView0.arrangedSubviews.contains(video) {
                        for (index, object) in stackView1.arrangedSubviews.enumerated() {
                            if video == object {
                                defaultIndex = index
                                break
                            }
                        }
                        stackView0.addArrangedSubview(video)
                    }
                } else {
                    if !stackView1.arrangedSubviews.contains(video) {
                        stackView1.addArrangedSubview(video)
                        defaultVideo = video
                    }
                }
            }
            
            if defaultVideo != nil {
                stackView1.insertArrangedSubview(defaultVideo!, at: defaultIndex)
            }
            
            largeIndex = location
            if !allowRotation {
                stackViewHeight0.constant = ARScreenWidth * 0.577
                stackViewHeight1.constant = ARScreenWidth/3 * 0.677
            }
        }
    }
    
    @objc private func applyMic() {
        SVProgressHUD.show(UIImage(named: "icon_loading")!, status: "上麦中")
        ARNetWorkHepler.getResponseData("applyMike", parameters: ["roomId": infoVideoModel.roomId] as [String : AnyObject], headers: true, success: { [self] (result) in
            if result["code"] == 0 {
                removeMicAllStatus()
                let jsonArr = result["data"].arrayValue
                for json in jsonArr {
                    let model = ARMicModel(jsonData: json)
                    refreshMicData(model: model)
                }
                
                for (index, model) in audienceArr.enumerated() {
                    if model.uid == UserDefaults.string(forKey: .uid) {
                        audienceArr.remove(at: index)
                        updateAudinceList()
                        break
                    }
                }
                
                allowRotation = true
                setNewOrientation()
            } else if result["code"] == 1055 {
                // 满员
                updateJoinButtonState()
            }
            SVProgressHUD.dismiss()
        }) { (error) in
            SVProgressHUD.dismiss(withDelay: 0.5)
        }
    }
    
    private func removeMicAllStatus() {
        for video in videoArr {
            video.toolView.isHidden = true
            if video.uid != nil {
                let videoCanvas = ARtcVideoCanvas()
                videoCanvas.uid = video.uid!
                if video.uid == UserDefaults.string(forKey: .uid) {
                    rtcKit.setupLocalVideo(videoCanvas)
                } else {
                    rtcKit.setupRemoteVideo(videoCanvas)
                }
                video.uid = nil
            }
        }
    }
    
    private func refreshMicData(model: ARMicModel) {
        if model.seat == 1 {
            muteAllAudioButton.isHidden = !(model.uid == UserDefaults.string(forKey: .uid))
        }
        
        let video = videoArr[model.seat - 1]
        video.micModel = model
        video.toolView.isHidden = false
        
        
        let videoCanvas = ARtcVideoCanvas()
        videoCanvas.view = videoArr[model.seat - 1].renderView
        videoCanvas.uid = model.uid!
        
        if model.uid == UserDefaults.string(forKey: .uid) {
            if (localMicModel?.seat != 1 || localMicModel == nil) && model.seat == 1 {
                let dic: NSDictionary! = ["cmd": "hostTip", "userName": UserDefaults.string(forKey: .userName) as Any]
                sendChannelMessage(text: getJSONStringFromDictionary(dictionary: dic))
                
                logVC?.log(logModel: ARLogModel(userName: UserDefaults.string(forKey: .userName), status: .broadcast))
                
                audioButton.isSelected = !localMute
                audioButton.setImage(UIImage(named: "icon_unaudio"), for: .normal)
                audioButton.setImage(UIImage(named: "icon_audio"), for: .selected)
            }
            
            self.localMicModel = model
            rtcKit.setClientRole(.broadcaster)
            rtcKit.setupLocalVideo(videoCanvas)
            rtcKit.muteLocalAudioStream(localMute)
            if allowRotation == false {
                allowRotation = true
                self.perform(#selector(setNewOrientation), with: nil, afterDelay: 0.5)
            }
            video.audioImageView.image = UIImage(named: localMute ? "icon_audio_close" : "icon_audio_open")
        } else {
            rtcKit.setupRemoteVideo(videoCanvas)
            
            let arr: NSArray = broadCastAudioDic.allKeys as NSArray
            if arr.contains(video.uid as Any) {
                let audio: Bool = broadCastAudioDic.object(forKey: video.uid as Any) as! Bool
                video.audioImageView.image = UIImage(named: audio ? "icon_audio_open" : "icon_audio_close")
            }
        }
    }
    
    func getUserList(uType: NSInteger) {
        let parameter = ["roomId": infoVideoModel.roomId as Any, "uType": uType]
        ARNetWorkHepler.getResponseData("getSrUserList", parameters:parameter as [String : AnyObject], headers: true, success: { [self] (result) in
            if result["code"] == 0 {
                if uType == 1 {
                    removeMicAllStatus()
                    self.broadCastArr.removeAll()
                } else {
                    self.audienceArr.removeAll()
                }
        
                let jsonArr = result["data"].arrayValue
                for json in jsonArr {
                    let model = ARMicModel(jsonData: json)
                    if uType == 1 {
                        refreshMicData(model: model)
                        self.broadCastArr.append(model)
                    } else {
                        self.audienceArr.append(model)
                        self.updateAudinceList()
                    }
                    
                    if model.uid == UserDefaults.string(forKey: .uid) {
                        self.localMicModel = model
                    }
                }
                
                updateJoinButtonState()
                updateMemberVc()
            }
        }) { (error) in
            
        }
    }
    
    private func addOrUpdateChannel(key: String, value: String) {
        // 更新频道属性
        let channelAttribute = ARtmChannelAttribute()
        channelAttribute.key = key
        channelAttribute.value = value
        
        let attributeOptions = ARtmChannelAttributeOptions()
        attributeOptions.enableNotificationToChannelMembers = true
        
        rtmEngine.addOrUpdateChannel(infoVideoModel.roomId!, attributes: [channelAttribute], options: attributeOptions) { (errorCode) in
            print("addOrUpdateChannel code: \(errorCode.rawValue)")
        }
    }
    
    private func updateMemberVc() {
        if topViewController() is ARMemberViewController {
            NotificationCenter.default.post(name: UIResponder.studyRoomNotificationMemberUpdate, object: nil)
        }
    }
    
    @objc func enterMemberVc() {
        // 成员列表
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let memberVc = storyboard.instantiateViewController(withIdentifier: "StudyRoom_Member") as! ARMemberViewController
        memberVc.videoVc = self
        memberVc.modalPresentationStyle = .overFullScreen
        self.present(memberVc, animated: true, completion: nil)
    }
    
    @objc func reportRoom() {
        let storyboard = UIStoryboard.init(name: "Main", bundle: nil)
        let reportVc = storyboard.instantiateViewController(withIdentifier: "StudyRoom_Report")
        self.present(reportVc, animated: true, completion: nil)
    }
    
    @objc func destroyRoom() {
        
        if !destroy {
            destroy.toggle()
            leaveXHToast(reason: reason)
            NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(destroyRoom), object: nil)
            dismissLoadingView()
            
            if allowRotation {
                allowRotation = false
                setNewOrientation()
            }
            
            let topVc = topViewController()
            if !(topVc is ARVideoViewController) {
                topVc.dismiss(animated: false, completion: nil)
            }
            
            ARSourceTimer.stop()
            let dic: NSDictionary! = ["cmd": "leaveTip", "userName": UserDefaults.string(forKey: .userName) as Any, "avatar": UserDefaults.string(forKey: .avatar) as Any]
            sendChannelMessage(text: getJSONStringFromDictionary(dictionary: dic))
            leaveRoom(roomId: infoVideoModel.roomId!)
            
            leaveChannel()
            ARtcEngineKit.destroy()
            
            rtmChannel?.channelDelegate = nil
            rtmEngine.destroyChannel(withId: (infoVideoModel.roomId)!)
            rtmEngine.aRtmDelegate = nil
            rtmEngine.logout(completion: nil)
            self.dismiss(animated: false, completion: nil)
        }
    }
    
    @objc func sendChannelMessage(text: String) {
        // 发送频道消息
        let rtmMessage: ARtmMessage = ARtmMessage.init(text: text)
        let options: ARtmSendMessageOptions = ARtmSendMessageOptions()
        rtmChannel?.send(rtmMessage, sendMessageOptions: options) { (errorCode) in
            print("Send Channel Message")
        }
    }
    
    override func sendRandomImage() {
        // 发送图片
        let imageDic = randomImage()
        
        let index = imageDic.allKeys[0] as! Int
        let width: CGFloat = index > 9 ? 600.0 : 400.0
        let height: CGFloat = index > 9 ? 400.0 : 600.0
        
        let imageUrl: String = imageDic.allValues[0] as! String
        logVC?.log(logModel: ARLogModel(userName: UserDefaults.string(forKey: .userName), uid: UserDefaults.string(forKey: .uid), seat: localMicModel?.seat ?? 0, imageUrl: imageUrl, status: .image, avatar: UserDefaults.string(forKey: .avatar), imageWidth: width, imageHeight: height))
        
        let dic: NSDictionary! = ["cmd": "picMsg", "userName": UserDefaults.string(forKey: .userName) as Any, "avatar": UserDefaults.string(forKey: .avatar) as Any, "imgUrl": imageUrl, "imageWidth": width, "imageHeight": height, "setNum": localMicModel?.seat as Any]
        sendChannelMessage(text: getJSONStringFromDictionary(dictionary: dic))
    }
    
    override func didSendChatTextField() {
        // 发送文本消息
        let text = chatTextField.text
        if text?.count ?? 0 > 0 && !stringAllIsEmpty(string: text ?? "") {
            
            let dic: NSDictionary! = ["cmd": "msg", "content": chatTextField.text as Any, "userName": UserDefaults.string(forKey: .userName) as Any, "avatar": UserDefaults.string(forKey: .avatar) as Any, "setNum": localMicModel?.seat ?? 0]
            sendChannelMessage(text: getJSONStringFromDictionary(dictionary: dic))
            
            logVC?.log(logModel: ARLogModel(userName: UserDefaults.string(forKey: .userName), uid: UserDefaults.string(forKey: .uid), text: text, seat: localMicModel?.seat ?? 0, avatar: UserDefaults.string(forKey: .avatar)))
            
            chatTextField.text = ""
            chatTextField.resignFirstResponder()
            confirmButton.alpha = 0.3
        }
    }
    
    private func connectionStateChanged(state: Int) {
        // 连接状态发生改变
        if !destroy {
            if state == 1 {
                if reason != .timeOut {
                    NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(destroyRoom), object: nil)
                    reason = .timeOut
                    showLoadingView(text: "连接中...", count: Float(Int.max))
                    self.perform(#selector(destroyRoom), with: nil, afterDelay: TimeInterval(10))
                }
            } else if state == 3 {
                NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(destroyRoom), object: nil)
                reason = .normal
                dismissLoadingView()
            }
        }
    }
    
    private func updateJoinButtonState() {
        if broadCastArr.count >= 4 {
            joinButton.backgroundColor = UIColor(hexString: "#434343")
            joinButton.setImage(UIImage(), for: .normal)
            joinButton.setTitleColor(UIColor(hexString: "#757575"), for: .normal)
            joinButton.setTitle("座位已满", for: .normal)
        } else {
            joinButton.backgroundColor = UIColor(hexString: "#FF4316")
            joinButton.setImage(UIImage(named: "icon_mic"), for: .normal)
            joinButton.setTitleColor(UIColor(hexString: "#FFFFFF"), for: .normal)
            joinButton.setTitle("加入", for: .normal)
        }
    }
    
    private func updateAudinceList() {
        for (index, object) in audienceStackView.arrangedSubviews.enumerated() {
            let imageView = object as! UIImageView
            imageView.isHidden = true
            if index < audienceArr.count {
                imageView.isHidden = false
                let model = audienceArr[index]
                imageView.sd_setImage(with: NSURL(string: model.avatar ?? "") as URL?, placeholderImage: UIImage(named: "icon_head"))
            }
        }
        audienceNumberLabel.text = "\(audienceArr.count)个观众"
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        guard let identifier = segue.identifier else {
            return
        }
        
        if identifier == "EmbedLogViewController",
            let vc = segue.destination as? LogViewController {
            self.logVC = vc
        }
    }
    
    @objc func setNewOrientation() {
        
        for object in bottomStackView.arrangedSubviews {
            let button = object as! UIButton
            (button == joinButton) ? (button.isHidden = allowRotation) : (button.isHidden = !allowRotation)
        }
        muteAllAudioButton.isHidden = !(localMicModel?.seat == 1)
        exitButton.isHidden = false
        
        if !allowRotation {
            // 竖屏
            appDelegate.allowRotation = false
            UIDevice.current.setValue(NSNumber(value: UIInterfaceOrientation.portrait.rawValue), forKey: "orientation")
            
            DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.1) {
                self.bottomStackView.backgroundColor = UIColor.clear
                self.bottomPadding.constant = 74
                self.topPadding.constant = 88
                self.stackViewHeight0.constant = ARScreenWidth/2 * 0.688
                self.stackViewHeight1.constant = ARScreenWidth/2 * 0.688
            }
        } else {
            // 横屏
            if self.stackView0.arrangedSubviews.count > 1 {
                self.stackView1.addArrangedSubview(stackView0.arrangedSubviews[1])
                self.stackView1.insertArrangedSubview(stackView1.arrangedSubviews[2], at: 0)
            }
            
            audienceListConstraint.priority = UILayoutPriority(rawValue: 1000)
            titleLabel.isHidden = false
            
            appDelegate.allowRotation = true
            UIDevice.current.setValue(NSNumber(value: UIInterfaceOrientation.landscapeRight.rawValue), forKey: "orientation")
            
            DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.1) {
                let stackView = self.view.viewWithTag(100) as! UIStackView
                stackView.axis = .horizontal
                
                self.bottomStackView.backgroundColor = UIColor(hexString: "#434343")
                self.topPadding.constant = 0
                self.bottomPadding.constant = 64
                self.stackViewHeight0.constant = 0.72 * ARScreenWidth
                self.stackViewHeight1.constant = 0.28 * ARScreenWidth
                self.containerWidth.constant = 0.549 * ARScreenHeight
            }
        }
    }
    
    override var preferredStatusBarStyle: UIStatusBarStyle {
        return .lightContent
    }
    
    override func textFieldDidBeginEditing(_ textField: UITextField) {
        if textField == roomTextField {
            textField.resignFirstResponder()
            DispatchQueue.main.async {
                self.chatTextField.becomeFirstResponder()
            }
        }
    }
}

extension ARVideoViewController: ARtcEngineDelegate {
    
    func rtcEngine(_ engine: ARtcEngineKit, tokenPrivilegeWillExpire token: String) {
        // Token 过期回调
        reason = .tokenExpire
        destroyRoom()
    }
    
    func rtcEngine(_ engine: ARtcEngineKit, remoteAudioStateChangedOfUid uid: String, state: ARAudioRemoteState, reason: ARAudioRemoteStateReason, elapsed: Int) {
        // 远端音频状态发生改变回调
        if reason == .reasonRemoteMuted {
            broadCastAudioDic.setValue(false, forKey: uid)
        } else if reason == .reasonRemoteUnmuted {
            broadCastAudioDic.setValue(true, forKey: uid)
        }
        
        for object in videoArr {
            let video: ARVideoView = object
            if video.uid == uid{
                if reason == .reasonRemoteMuted {
                    video.audioImageView.image = UIImage(named: "icon_audio_close")
                } else if reason == .reasonRemoteUnmuted {
                    video.audioImageView.image = UIImage(named: "icon_audio_open")
                }
            }
        }
    }
    
    func rtcEngine(_ engine: ARtcEngineKit, connectionChangedTo state: ARConnectionStateType, reason: ARConnectionChangedReason) {
        // 网络连接状态已改变回调
        let stateValue = (reason == .interrupted) ? 1 : state.rawValue
        connectionStateChanged(state: stateValue)
    }
}

// MARK: - ARtmDelegate,ARtmChannelDelegate

extension ARVideoViewController: ARtmDelegate, ARtmChannelDelegate {
    
    func rtmKit(_ kit: ARtmKit, connectionStateChanged state: ARtmConnectionState, reason: ARtmConnectionChangeReason) {
        // 连接状态改变回调
        connectionStateChanged(state: state.rawValue)
    }
    
    func channel(_ channel: ARtmChannel, messageReceived message: ARtmMessage, from member: ARtmMember) {
        //收到频道消息回调
        let dic = getDictionaryFromJSONString(jsonString: message.text)
        let value: String? = dic.object(forKey: "cmd") as? String
        if value == "enterTip" || value == "leaveTip" {
            containerView.showPopupView(ARPopupModel(headUrl: dic.object(forKey: "avatar") as? String, name: dic.object(forKey: "userName") as? String, isJoin: (value == "enterTip") ? true : false))
        } else if value == "seatChange" {
            // 位置改变
            broadCastArr.removeAll()
            removeMicAllStatus()
            let arr: NSArray = getArrayFromJSONString(jsonString: dic["data"] as! String)
            for object in arr {
                let model = ARMicModel(jsonData: JSON(object))
                if model.seat != 0 {
                    broadCastArr.append(model)
                    refreshMicData(model: model)
                }
            }
            
            updateJoinButtonState()
            getUserList(uType: 2)
        } else if value == "msg" {
            // 文本消息
            logVC?.log(logModel: ARLogModel(userName: dic.object(forKey: "userName") as? String, uid: member.uid, text: dic .object(forKey: "content") as? String, seat: dic.object(forKey: "setNum") as! NSInteger, status: .text, avatar: dic .object(forKey: "avatar") as? String))
        } else if value == "picMsg" {
            // 图片消息
            logVC?.log(logModel: ARLogModel(userName: dic.object(forKey: "userName") as? String, uid: member.uid, seat: dic.object(forKey: "setNum") as! NSInteger, imageUrl: dic .object(forKey: "imgUrl") as? String, status: .image, avatar: dic .object(forKey: "avatar") as? String, imageWidth: dic.object(forKey: "imageWidth") as! CGFloat, imageHeight: dic.object(forKey: "imageHeight") as! CGFloat))
        } else if value == "hostTip" {
            logVC?.log(logModel: ARLogModel(userName: dic.object(forKey: "userName") as? String, status: .broadcast))
        }
    }
    
    func channel(_ channel: ARtmChannel, attributeUpdate attributes: [ARtmChannelAttribute]) {
        // 频道属性更新
        for attribute in attributes {
            if attribute.key == "allAudioState" {
                muteAllAudioButton.isSelected = (attribute.value == "1")
                allMute = (attribute.value == "1") ? true : false
                
                logVC?.log(logModel: ARLogModel(text: allMute ? "主持人 已开启全员静音" : "主持人 已关闭全员静音", status: .audioMute))
                
                if allMute {
                    if localMicModel?.seat != 1 {
                        audioButton.isSelected = false
                        audioButton.setImage(UIImage(named: "icon_allmute"), for: .normal)
                        rtcKit.muteLocalAudioStream(true)
                    }
                } else {
                    audioButton.isSelected = !localMute
                    audioButton.setImage(UIImage(named: "icon_unaudio"), for: .normal)
                    audioButton.setImage(UIImage(named: "icon_audio"), for: .selected)
                    rtcKit.muteLocalAudioStream(localMute)
                }
                
                for object in videoArr {
                    let video = object
                    if video.uid == UserDefaults.string(forKey: .uid) {
                        if allMute {
                            if localMicModel?.seat != 1 {
                                video.audioImageView.image = UIImage(named: "icon_audio_close")
                            }
                        } else {
                            video.audioImageView.image = UIImage(named: localMute ? "icon_audio_close" : "icon_audio_open")
                        }
                        break
                    }
                }
            }
        }
    }
    
    func channel(_ channel: ARtmChannel, memberJoined member: ARtmMember) {
        // 频道成员加入频道回调
        getUserList(uType: 2)
    }
    
    func channel(_ channel: ARtmChannel, memberLeft member: ARtmMember) {
        // 频道成员离开频道回调
        for (index, model) in audienceArr.enumerated() {
            if model.uid == member.uid {
                audienceArr.remove(at: index)
                updateAudinceList()
                updateMemberVc()
                break
            }
        }
    }
}
