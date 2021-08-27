//
//  ARPopContainerView.swift
//  StudyRoom-iOS
//
//  Created by 余生丶 on 2021/8/7.
//

import UIKit

private let count = 3
private let margin : CGFloat = 10

class ARPopContainerView: UIView {

    fileprivate lazy var popViewArr: [ARPopupView] = [ARPopupView]()
    fileprivate lazy var cachePopArr: [ARPopupModel] = [ARPopupModel]()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupUI()
    }
    
    private func setupUI() {
        let h : CGFloat = 40
        for i in 0..<count {
            let y = (h + margin) * CGFloat(i)
            let popUpView = ARPopupView.loadPopupView()
            popUpView.frame.origin.y = y
            popUpView.alpha = 0
            popUpView.tag = i
            addSubview(popUpView)
            popViewArr.append(popUpView)
            
            popUpView.complectionCallback = { popView in
                guard self.cachePopArr.count != 0 else { return }
                let popModel = self.cachePopArr.first!
                self.cachePopArr.removeFirst()
                
                popView.popupModel = popModel
            }
        }
    }
    
    func showPopupView(_ giftModel: ARPopupModel) {
        if let channelView = getFreePopupView() {
            channelView.popupModel = giftModel
            return
        }

        cachePopArr.append(giftModel)
    }
    
    private func getFreePopupView() -> ARPopupView? {
        for popupView in popViewArr {
            if popupView.state == .normal {
                return popupView
            }
        }
        return nil
    }

}
