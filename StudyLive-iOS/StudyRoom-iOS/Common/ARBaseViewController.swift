//
//  ARBaseViewController.swift
//  AudioLive-iOS
//
//  Created by 余生丶 on 2021/2/24.
//

import UIKit

class ARBaseViewController: UIViewController {
    
    var keyBoardView: UIView!
    var chatTextField: UITextField!
    var confirmButton: UIButton!
    lazy var rightButton: UIButton = {
        let button = UIButton(type: .custom)
        button.frame = CGRect(x: 0, y: 0, width: 32, height: 32)
        button.setImage(UIImage(named: "icon_image"), for: .normal)
        button.addTarget(self, action: #selector(sendRandomImage), for: .touchUpInside)
        return button
    }()

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        self.view.addSubview(getInputAccessoryView())
        
        NotificationCenter.default.addObserver(self,selector:#selector(keyboardChange(notify:)), name: UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.addObserver(self,selector:#selector(keyboardChange(notify:)), name: UIResponder.keyboardWillHideNotification, object: nil)
    }
    
    func getInputAccessoryView() -> UIView {
        keyBoardView = UIView.init(frame: CGRect.init(x: 0, y: ARScreenHeight, width: appDelegate.allowRotation ? ARScreenHeight : ARScreenWidth, height: 44))
        keyBoardView.backgroundColor = UIColor(hexString: "#171717")
        
        let boardView = UIView()
        boardView.backgroundColor = UIColor.clear
        keyBoardView.addSubview(boardView)
        boardView.snp_makeConstraints { (make) in
            make.top.bottom.centerX.equalToSuperview()
            make.width.equalTo(ARScreenWidth)
        }
        
        chatTextField = UITextField.init(frame: CGRect.init(x: 15, y: 6, width: ARScreenWidth - 80, height: 32));
        chatTextField.textColor = UIColor.white
        chatTextField.font = UIFont(name: PingFang, size: 11)
        chatTextField.layer.masksToBounds = true
        chatTextField.layer.cornerRadius = 5
        chatTextField.returnKeyType = .send
        chatTextField.placeholder = "说点什么.."
        chatTextField.backgroundColor = UIColor(hexString: "#434343")
        chatTextField.delegate = self
        chatTextField.addTarget(self, action: #selector(chatTextFieldLimit), for: .editingChanged)
        chatTextField.placeHolderColor = UIColor(hexString: "#BCBCBC")
        
        let leftView = UIView.init(frame: CGRect(x: 0, y: 0, width: 8, height: 0))
        chatTextField.leftView = leftView
        chatTextField.leftViewMode = .always
        
//        let button = UIButton(type: .custom)
//        button.frame = CGRect(x: 0, y: 0, width: 32, height: 32)
//        button.setImage(UIImage(named: "icon_image"), for: .normal)
//        chatTextField.rightView = button
//        chatTextField.rightViewMode = .always
        boardView.addSubview(chatTextField)
        
        confirmButton = UIButton.init(type: .custom)
        confirmButton.frame = CGRect.init(x: ARScreenWidth - 59, y: 6, width: 49, height: 32)
        confirmButton.setTitleColor(UIColor(hexString: "#434343"), for: .normal)
        confirmButton.layer.masksToBounds = true
        confirmButton.titleLabel?.font = UIFont.init(name: PingFangBold, size: 14)
        confirmButton.layer.cornerRadius = 5
        confirmButton.setTitle("发送", for:.normal)
        confirmButton.addTarget(self, action: #selector(didSendChatTextField), for: .touchUpInside)
        boardView.addSubview(confirmButton)
        return keyBoardView
    }
    
    @objc func chatTextFieldLimit() {
        if chatTextField.text?.count ?? 0 > 128 {
            chatTextField.text = String((chatTextField.text?.prefix(128))!)
        }
        
        if isBlank(text: chatTextField.text) || stringAllIsEmpty(string: chatTextField.text ?? "") {
            confirmButton.setTitleColor(UIColor(hexString: "#434343"), for: .normal)
        } else {
            confirmButton.setTitleColor(UIColor(hexString: "#FF4316"), for: .normal)
        }
    }
    
    @objc public func didSendChatTextField() {
        // 发送消息
    }
    
    @objc public func sendRandomImage() {
        //发送图片消息
    }
    
    @objc func keyboardChange(notify:NSNotification){
        if chatTextField.isFirstResponder {
            //时间
            let duration : Double = notify.userInfo![UIResponder.keyboardAnimationDurationUserInfoKey] as! Double
            if notify.name == UIResponder.keyboardWillShowNotification {
                //键盘高度
                let keyboardY : CGFloat = (notify.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as! NSValue).cgRectValue.size.height
                let high = UIScreen.main.bounds.size.height - keyboardY - 44
                
                UIView.animate(withDuration: duration) {
                    self.keyBoardView.frame = CGRect(x: 0, y: high, width: appDelegate.allowRotation ? ARScreenHeight : ARScreenWidth, height: 44)
                    self.view.layoutIfNeeded()
                }
            } else if notify.name == UIResponder.keyboardWillHideNotification {
                
                UIView.animate(withDuration: duration, animations: {
                    self.keyBoardView.frame = CGRect(x: 0, y: ARScreenHeight, width: appDelegate.allowRotation ? ARScreenHeight : ARScreenWidth, height: 44)
                    self.view.layoutIfNeeded()
                })
            }
        }
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        view.endEditing(true)
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}

extension ARBaseViewController: UITextFieldDelegate {
    func textFieldDidBeginEditing(_ textField: UITextField) {
    }
    
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        didSendChatTextField()
        return true
    }
}


