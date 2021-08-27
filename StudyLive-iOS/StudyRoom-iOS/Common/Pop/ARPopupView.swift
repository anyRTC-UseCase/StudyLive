//
//  ARPopupView.swift
//  StudyRoom-iOS
//
//  Created by 余生丶 on 2021/8/7.
//

import UIKit

struct ARPopupModel {
    var headUrl: String?
    var name: String?
    var isJoin = false
}

enum ARPopUpState {
    case normal, ongoing, end
}

class ARPopupView: UIView {
    
    @IBOutlet weak var headImageView: UIImageView!
    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var stateLabel: UILabel!
    
    var state: ARPopUpState = .normal
    var complectionCallback: ((ARPopupView)->())?
    var originX: CGFloat = 0.0
    
    var popupModel: ARPopupModel? {
        didSet {
            state = .ongoing
            guard let popupModel = popupModel else {return}
            
            nameLabel.text = popupModel.name
            let width = getLableWidth(labelText: nameLabel.text ?? "" , font: UIFont(name: PingFang, size: 11)!, height: 15.5)
            if width < 64 {
                originX = 64 - width
                self.frame = CGRect(x: self.frame.origin.x, y: self.frame.origin.y, width: (174 - originX), height: 40)
            } else {
                self.frame = CGRect(x: self.frame.origin.x, y: self.frame.origin.y, width: 174, height: 40)
            }
            
            headImageView.sd_setImage(with: NSURL(string: popupModel.headUrl ?? "") as URL?, placeholderImage: UIImage(named: "icon_head"))
            stateLabel.text = popupModel.isJoin ? "进入直播间" : "退出直播间"
            
            startAnimation()
        }
    }
    
    class func loadPopupView() -> ARPopupView {
        let popUpView = Bundle.main.loadNibNamed("ARPopupView", owner: nil, options: nil)?.first as! ARPopupView
        
        return popUpView
    }
    
    private func startAnimation() {
        UIView.animate(withDuration: 0.25, animations: {
            self.frame.origin.x = self.originX
            
        }, completion: { (isFinished) in
            self.alpha = 1.0
            self.perform(#selector(self.stopAnimation), with: nil, afterDelay: 1.2)
        })
    }
    
    @objc private func stopAnimation() {
        state = .end
        UIView.animate(withDuration: 0.25, animations: {
            self.frame.origin.x = UIScreen.main.bounds.width
            self.alpha = 0.0
        }, completion: { (isFinished) in
            self.state = .normal
            self.originX = 0.0
            self.frame.origin.x = -self.bounds.width
            if (self.complectionCallback != nil) {
                self.complectionCallback!(self)
            }
        })
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        
        self.layer.cornerRadius = frame.height * 0.5
        self.layer.maskedCorners = [.layerMinXMinYCorner, .layerMinXMaxYCorner]
        self.layer.masksToBounds = true
    }
}
