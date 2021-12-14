//
//  ARMemberViewController.swift
//  StudyRoom-iOS
//
//  Created by 余生丶 on 2021/8/10.
//

import UIKit
import AttributedString

let appDelegate: AppDelegate = UIApplication.shared.delegate as! AppDelegate

class ARMemberView: UIView {
    
    fileprivate var memberArr = [ARMicModel]()
    fileprivate var memberWidth = appDelegate.allowRotation ? ARScreenHeight/2 : ARScreenWidth
    fileprivate var memberHeight = appDelegate.allowRotation ? (ARScreenWidth - 50) : (ARScreenHeight/2 - 50)
    
    fileprivate lazy var placeholder: UILabel = {
        let label: UILabel = UILabel()
        label.frame = CGRect.init(x: (memberWidth - 200)/2, y: (memberHeight - 188)/2, width: 200, height: 188)
        label.attributed.text = """
         \(.image(#imageLiteral(resourceName: "icon_neterror"), .custom(size: CGSize(width: 130, height: 110))))
         \("\n 暂无数据～", .foreground(UIColor(hexString: "#757575")), .font(UIFont(name: PingFang, size: 14)!))
         """
        label.isHidden = false
        label.numberOfLines = 0
        label.textAlignment = .center
        return label
    }()
    
    fileprivate lazy var tableView: UITableView = {
        let tableView: UITableView = UITableView(frame: CGRect(x: 0.0, y: 0.0, width: memberWidth, height: memberHeight), style: .plain)
        tableView.delegate = self
        tableView.dataSource = self
        tableView.rowHeight = 52
        tableView.tableFooterView = UIView()
        return tableView
    }()
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        self.addSubview(placeholder)
        self.addSubview(tableView)
    }
    
    func reloadData() {
        memberArr = memberArr.sorted(by: { $0.seat < $1.seat })
        tableView.isHidden = (memberArr.count == 0)
        tableView.reloadData()
    }
}

extension ARMemberView: UITableViewDelegate, UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return memberArr.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        var cell = tableView.dequeueReusableCell(withIdentifier: "StudyRoom_MemberCell")
        if cell == nil {
            cell = UITableViewCell.init(style: .value1, reuseIdentifier: "StudyRoom_MemberCell")
        }
        
        let model = memberArr[indexPath.row]
        cell?.textLabel?.text = model.userName
        if model.seat != 0 {
            cell?.detailTextLabel?.text = "\(model.seat)号座"
        }
        cell?.textLabel?.textColor = UIColor(hexString: "#171717")
        cell?.textLabel?.font = UIFont(name: PingFang, size: 12)
        cell?.imageView?.sd_setImage(with: NSURL(string: model.avatar ?? "") as URL?, placeholderImage: UIImage(named: "icon_head"))
        cell?.detailTextLabel?.textColor = UIColor(hexString: "#BCBCBC")
        cell?.detailTextLabel?.font = UIFont(name: PingFang, size: 12)

        let itemSize = CGSize(width: 42, height: 42)
        UIGraphicsBeginImageContextWithOptions(itemSize, false, UIScreen.main.scale)
        let imageRect = CGRect(x: 0, y: 0, width: itemSize.width, height: itemSize.height)
        cell?.imageView?.image?.draw(in: imageRect)
        cell?.imageView?.image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        cell?.imageView?.layer.cornerRadius = 21
        cell?.imageView?.layer.masksToBounds = true
        cell?.selectionStyle = .none
        return cell!
    }
    
    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        if cell.responds(to:#selector(setter: UIView.layoutMargins)) {
            cell.layoutMargins = UIEdgeInsets.zero
        }
        
        if cell.responds(to: #selector(setter: UITableViewCell.separatorInset)) {
            cell.separatorInset = UIEdgeInsets.zero
        }
    }
}

class ARMemberViewController: UIViewController {
    
    @IBOutlet weak var backView: UIView!
    @IBOutlet weak var topView: UIView!
    @IBOutlet weak var stackView: UIStackView!
    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var broadcasterButton: UIButton!
    @IBOutlet weak var audienceButton: UIButton!
    @IBOutlet weak var broadcasterView: ARMemberView!
    @IBOutlet weak var audienceView: ARMemberView!
    
    lazy var lineView: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor(hexString: "#FF4316")
        view.frame = CGRect(x: 0.25 * ARScreenWidth - 11, y: 42, width: 22, height: 4)
        return view
    }()
    
    fileprivate var memberWidth = appDelegate.allowRotation ? ARScreenHeight/2 : ARScreenWidth
    fileprivate var memberHeight = appDelegate.allowRotation ? (ARScreenWidth - 50) : (ARScreenHeight/2 - 50)
    var videoVc: ARVideoViewController!
    
    let tap = UITapGestureRecognizer()
    var selectIndex: NSInteger = 0
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        appDelegate.allowRotation ? (stackView.axis = .horizontal) : (stackView.axis = .vertical)
        topView.addSubview(lineView)
        scrollView.contentInsetAdjustmentBehavior = .never
        tap.addTarget(self, action: #selector(dismissMemberVc))
        self.stackView.subviews[0].addGestureRecognizer(tap)
        updateData()
        
        NotificationCenter.default.addObserver(self, selector: #selector(updateData), name: UIResponder.studyRoomNotificationMemberUpdate, object: nil)
    }
    
    @objc func updateData() {
        if broadcasterView != nil {
            broadcasterView.memberArr = videoVc.broadCastArr
            audienceView.memberArr = videoVc.audienceArr
            broadcasterView.reloadData()
            audienceView.reloadData()
            
            let number0 = broadcasterView.memberArr.count
            let title0 = (broadcasterView.memberArr.count == 0) ? "自习室成员" : "自习室成员 \(number0)"
            broadcasterButton.setTitle(title0, for: .normal)
            
            let number1 = audienceView.memberArr.count
            let title1 = (audienceView.memberArr.count == 0) ? "观众" : "观众 \(number1)"
            audienceButton.setTitle(title1, for: .normal)
        }
    }
    
    @objc func dismissMemberVc() {
        self.dismiss(animated: true, completion: nil)
    }
    
    @IBAction func didClickMemberButton(_ sender: UIButton) {
        self.selectIndex = sender.tag
        if !sender.isSelected {
            let selected = broadcasterButton.isSelected
            broadcasterButton.isSelected = audienceButton.isSelected
            audienceButton.isSelected = selected
            changeScrollerViewContentSize()
            changeLinePlaceWithIndex()
        }
    }
    
    func changeScrollerViewContentSize() {
        UIView.animate(withDuration: 0.25) { [self] in
            var offset = self.scrollView.contentOffset
            offset.x = memberWidth * CGFloat(self.selectIndex)
            self.scrollView.contentOffset = offset
        }
    }
    
    func changeLinePlaceWithIndex() {
        UIView.animate(withDuration: 0.25) {
            var frame = self.lineView.frame
            frame.origin.x = (self.selectIndex == 0) ? self.memberWidth * 0.25 - 11 : (self.memberWidth * 0.75 - 11)
            
            self.lineView.frame = frame
        }
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}

extension ARMemberViewController: UIScrollViewDelegate {
    
    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        selectIndex = NSInteger(scrollView.contentOffset.x / memberWidth)
        
        let selected = (selectIndex == 0)
        broadcasterButton.isSelected = selected
        audienceButton.isSelected = !selected
        changeLinePlaceWithIndex()
    }
}


