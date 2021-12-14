//
//  ARMainViewController.swift
//  VideoLive-iOS
//
//  Created by 余生丶 on 2021/6/18.
//

import UIKit
import AttributedString
import SDWebImage
import MJRefresh
import SVProgressHUD

private let reuseIdentifier = "StudyRoom_CellID"

class RefreshGifHeader: MJRefreshHeader {
    var rotatingImage: UIImageView?
    
    override var state: MJRefreshState {
        didSet {
            switch state {
            case .idle,.pulling:
                rotatingImage?.stopAnimating()
                break
            case .refreshing:
                rotatingImage?.startAnimating()
                break
            default:
                print("")
            }
        }
    }
    
    override func prepare() {
        super.prepare()
        rotatingImage = UIImageView.init()
        rotatingImage?.image = UIImage(named: "icon_refresh")
        self.addSubview(rotatingImage!)
        
        let rotationAnim = CABasicAnimation(keyPath: "transform.rotation.z")
        rotationAnim.fromValue = 0
        rotationAnim.toValue = Double.pi * 2
        rotationAnim.repeatCount = MAXFLOAT
        rotationAnim.duration = 1
        rotationAnim.isRemovedOnCompletion = false
        rotatingImage!.layer.add(rotationAnim, forKey: "rotationAnimation")
    }
    
    override func placeSubviews() {
        super.placeSubviews()
        rotatingImage?.frame = CGRect.init(x: 0, y: 0, width: 40, height: 40)
        rotatingImage?.center = CGPoint(x: self.mj_w / 2, y: self.mj_h / 2)
    }
}

class ARMainViewController: UICollectionViewController {
    private var flowLayout: UICollectionViewFlowLayout!
    private var index = 0
    private let blacklistIdentifier = "blacklistIdentifier"
    private var blackList = NSMutableArray()
    
    var modelArr = [ARMainRoomListModel]()
    
    lazy var placeholder: UILabel = {
        let label: UILabel = UILabel()
        label.frame = CGRect.init(x: (collectionView.width - 200)/2, y: (collectionView.height - 218)/2, width: 200, height: 188)
        label.attributed.text = """
         \(.image(#imageLiteral(resourceName: "icon_nonet"), .custom(size: CGSize(width: 189, height: 141))), action: requestRoomList)
         \("\n 网络开小差了～", .foreground(UIColor(hexString: "#757575")), .font(UIFont(name: PingFang, size: 14)!), .action(requestRoomList))
         """
        label.numberOfLines = 0
        label.textAlignment = .center
        return label
    }()

    override func viewDidLoad() {
        super.viewDidLoad()
        (UserDefaults.string(forKey: .uid) != nil) ? login() : registered()
        let arr = UserDefaults.standard.array(forKey: blacklistIdentifier)
        arr?.count ?? 0 > 0 ? (blackList.addObjects(from: arr!)) : nil

        // Uncomment the following line to preserve selection between presentations
        // self.clearsSelectionOnViewWillAppear = false
        
        // Do any additional setup after loading the view.
        flowLayout = UICollectionViewFlowLayout.init()
        flowLayout.sectionInset = UIEdgeInsets(top: 15, left: 15, bottom: 0, right: 15)
        flowLayout?.scrollDirection = .vertical
        flowLayout?.minimumLineSpacing = 9
        flowLayout?.minimumInteritemSpacing = 9
        let width = (ARScreenWidth - 40)/2
        flowLayout?.itemSize = CGSize.init(width: width, height: width * 1.529)
        collectionView.collectionViewLayout = flowLayout
        collectionView.mj_header = RefreshGifHeader(refreshingBlock: {
              [weak self] () -> Void in
              self?.requestRoomList()
        })
        
        self.view.addSubview(placeholder)
        NotificationCenter.default.addObserver(self, selector: #selector(requestRoomList), name: UIResponder.studyRoomNotificationLoginSucess, object: nil)
    }
    
    @objc func requestRoomList() {
        if UserDefaults.string(forKey: .isLogin) == "true" {
            ARNetWorkHepler.getResponseData("getRoomList", parameters: nil, headers: true, success: { [self] (result) in
                if result["code"] == 0 {
                    modelArr.removeAll()
                    let jsonArr = result["data"].arrayValue
                    for json in jsonArr {
                        let roomModel = ARMainRoomListModel(jsonData: json)
                        if !blackList.contains(roomModel.roomId as Any) {
                            self.modelArr.append(roomModel)
                        }
                    }
                }
                
                placeholder.isHidden = (self.modelArr.count == 0) ? false : true
                collectionView.reloadData()
                collectionView.mj_header?.endRefreshing()
            }) { (error) in
                self.collectionView.mj_header?.endRefreshing()
            }
        } else {
            placeholder.isHidden = false
            self.collectionView.mj_header?.endRefreshing()
            login()
        }
    }
    
    //加入房间
    func requestJoinRoom(roomId: String) {
        SVProgressHUD.show(UIImage(named: "icon_loading")!, status: "加载中")
        let parameters: NSDictionary = ["roomId": roomId]
        ARNetWorkHepler.getResponseData("joinRoom", parameters: parameters as? [String : AnyObject], headers: true) { [weak self](result) in
            if result["code"] == 0 {
                SVProgressHUD.dismiss(withDelay: 0.5)
                let infoModel = ARRoomInfoModel(jsonData: result["data"])
                infoVideoModel = infoModel
                
                let storyboard = UIStoryboard.init(name: "Main", bundle: nil)
                let videoVc = storyboard.instantiateViewController(withIdentifier: "StudyRoom_Video") as! ARVideoViewController
                videoVc.hidesBottomBarWhenPushed = true
                videoVc.modalPresentationStyle = .fullScreen
                self?.present(videoVc, animated: true, completion: nil)
            } else if result["code"] == 800 {
                SVProgressHUD.dismiss()
                self?.showToast(text: "房间已解散或不存在", image: "icon_tip_warning")
            }
        } error: { (error) in
        
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.navigationController?.navigationBar.setBackgroundImage(createImage(UIColor(hexString: "#F5F6FA")), for: .any, barMetrics: .default)
        self.navigationController?.navigationBar.isTranslucent = false
        self.navigationController?.navigationBar.shadowImage = UIImage()
        self.hidesBottomBarWhenPushed = false
        self.extendedLayoutIncludesOpaqueBars = true
        self.navigationController?.navigationBar.isHidden = false
        
        let titleLabel = UILabel()
        titleLabel.text = "any自习室"
        titleLabel.textColor = UIColor(hexString: "##171717")
        titleLabel.font = UIFont.init(name: PingFangBold, size: 18)
        titleLabel.frame = CGRect.init(x: 0, y: 0, width: 100, height: 50)
        self.navigationItem.leftBarButtonItem = UIBarButtonItem(customView: titleLabel)
        collectionView.mj_header?.beginRefreshing()
    }
    
    private func setRoomToBlack(roomId: String) {
        //拉黑
        var arr = UserDefaults.standard.array(forKey: blacklistIdentifier)
        if arr?.count ?? 0 > 0 {
            arr?.append(roomId as Any)
        } else {
            arr = [roomId as Any]
        }
        UserDefaults.standard.setValue(arr, forKey: blacklistIdentifier)
        
        for index in 0..<modelArr.count {
            let roomModel = modelArr[index]
            if roomModel.roomId == roomId {
                modelArr.remove(at: index)
                placeholder.isHidden = (modelArr.count != 0)
                collectionView.reloadData()
                break
            }
        }
        blackList.add(roomId)
    }

    // MARK: UICollectionViewDataSource

    override func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        // #warning Incomplete implementation, return the number of items
        return modelArr.count
    }

    override func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let collectionViewCell: ARMainViewCell! = (collectionView.dequeueReusableCell(withReuseIdentifier: reuseIdentifier, for: indexPath) as! ARMainViewCell)
        let roomModel = modelArr[indexPath.row]
        collectionViewCell.updateMainCell(listModel: roomModel, row: indexPath.row)
        
        collectionViewCell?.onButtonTapped = { [weak self] (tag) in
            guard let weakself = self else { return }
            
            if tag == 50 {
                UIAlertController.showAlert(in: self!, withTitle: "屏蔽", message: "屏蔽该自习室", cancelButtonTitle: "取消", destructiveButtonTitle: nil, otherButtonTitles: ["确定"]) { (alertVc, action, index) in
                    if index == 2 {
                        weakself.setRoomToBlack(roomId: roomModel.roomId!)
                    }
                }
            } else if tag == 51 {
                weakself.requestJoinRoom(roomId: roomModel.roomId!)
            }
        }
        return collectionViewCell
    }
}

class ARMainViewCell: UICollectionViewCell {
    
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var containerView: UIView!
    
    var onButtonTapped : ((_ tag: NSInteger) -> Void)? = nil
    
    func updateMainCell(listModel: ARMainRoomListModel?, row: NSInteger) {
        let firstcharacter: Character = (listModel?.roomName?.characterAtIndex(index: 0))!
        titleLabel.attributed.text = """
         \(firstcharacter, .foreground(UIColor(hexString: "#FF4316")), .font(UIFont(name: PingFangBold, size: 24)!))\(" 号自习室", .foreground(UIColor(hexString: "#171717")), .font(UIFont(name: PingFangBold, size: 14)!), .baselineOffset(3))
         """

        for index in 0...3 {
            let headImageView = containerView.viewWithTag(index + 1)! as! UIImageView
            headImageView.layer.cornerRadius = ((ARScreenWidth - 40)/2 - 32)/4
            headImageView.image = UIImage(named: "icon_placeholder")
            if index < listModel?.avatars.count ?? 0 && row != 4 {
                let url = listModel?.avatars[index]
                if url?.count != 0 {
                    headImageView.sd_setImage(with: NSURL(string: listModel?.avatars[index] ?? "") as URL?, placeholderImage: UIImage(named: "icon_head"))
                }
            }
        }
    }
    
    @IBAction func didClickMainCellButton(_ sender: UIButton) {
        if let onButtonTapped = self.onButtonTapped {
            onButtonTapped(sender.tag)
        }
    }
}
