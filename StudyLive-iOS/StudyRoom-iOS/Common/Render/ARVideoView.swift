//
//  ARVideoView.swift
//  VideoLive-iOS
//
//  Created by 余生丶 on 2021/6/21.
//

import UIKit

class ARVideoView: UIView {
    
    @IBOutlet weak var renderView: UIView!
    @IBOutlet weak var locationImageView: UIImageView!
    
    @IBOutlet weak var pointLabel: UILabel!
    @IBOutlet weak var timeLabel: UILabel!
    @IBOutlet weak var headImageView: UIImageView!
    @IBOutlet weak var toolView: UIView!
    @IBOutlet weak var audioImageView: UIImageView!
    
    var uid: String? {
        didSet {
            isFree = (uid == nil)
        }
    }
    
    var micModel: ARMicModel? {
        didSet {
            headImageView.sd_setImage(with: NSURL(string: micModel?.avatar ?? "") as URL?, placeholderImage: UIImage(named: "icon_head"))
            uid = micModel?.uid
        }
    }
    var isFree: Bool = true
    
    typealias videoBlock = (_ location: NSInteger, _ state: Bool) ->()
    var callback: videoBlock!
    
    class func videoView(location: NSInteger) -> ARVideoView {
        let video = Bundle.main.loadNibNamed("ARVideoView", owner: nil, options: nil)![0] as! ARVideoView
        video.locationImageView.image = UIImage(named: "icon_location_\(location)")
        video.tag = location
        video.addGestureRecognizer()
        return video
    }
    
    fileprivate func addGestureRecognizer() {
        let tap: UITapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(didClickVideo))
        self.addGestureRecognizer(tap)
        pointLabel.layer.add(alphaLight(time: 1), forKey: "aAlpha")
    }
    
    @objc func didClickVideo() {
        if let _ = callback {
            callback(self.tag, isFree)
        }
    }
    
    private func alphaLight(time: CGFloat) -> CABasicAnimation {
        let animation = CABasicAnimation.init(keyPath: "opacity")
        animation.fromValue = 1
        animation.toValue = 0
        animation.autoreverses = true
        animation.duration = CFTimeInterval(time)
        animation.repeatCount = Float(CGFloat.greatestFiniteMagnitude)
        animation.isRemovedOnCompletion = false
        animation.fillMode = .forwards
        animation.timingFunction = CAMediaTimingFunction.init(name: .easeIn)
        return animation
    }
}
