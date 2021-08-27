//
//  LogViewController.swift
//  AudioLive-iOS
//
//  Created by 余生丶 on 2021/3/9.
//

import UIKit
import AttributedString
import SnapKit

enum ARLogStatus {
    case text, image, broadcast, system, audioMute
}

struct ARLogModel {
    var userName: String?
    var uid: String?
    var text: String?
    var seat: NSInteger = 0
    var imageUrl: String?
    var status: ARLogStatus = .text
    var avatar: String?
    var imageWidth: CGFloat = 0.0
    var imageHeight: CGFloat = 0.0
}

class LogCell: UITableViewCell {
    
    @IBOutlet weak var contentLabel: UILabel!
    @IBOutlet weak var colorView: UIView!

    override func awakeFromNib() {
        super.awakeFromNib()
        colorView.layer.cornerRadius = 12.25
    }
    
    func update(logModel: ARLogModel) {
        if logModel.status == .broadcast {
            var userName = logModel.userName
            (userName == nil) ? userName = "" : nil
            contentLabel.text = "\(userName!) 已成为主持人"
            contentLabel.textColor = UIColor.white
        } else {
            contentLabel.text = logModel.text
            contentLabel.textColor = UIColor(hexString: (logModel.status == .audioMute) ? "#FFFFFF" : "#FFEC99")
        }
    }
}

class ARChatCell: UITableViewCell {
    
    @IBOutlet weak var headImageView: UIImageView!
    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var contentLabel: UILabel!
    @IBOutlet weak var seatLabel: UILabel!
    
    func update(logModel: ARLogModel) {
        var userName = logModel.userName
        (userName == nil) ? userName = "" : nil
        if logModel.uid == UserDefaults.string(forKey: .uid) {
            nameLabel.attributed.text = """
             \(logModel.userName ?? "", .foreground(UIColor(hexString: "#FF4316")))
             """
            
            contentLabel.attributed.text = """
            \(logModel.text ?? "", .foreground(UIColor(hexString: "#FF4316")))
            """
        } else {
            nameLabel.attributed.text = """
             \(logModel.userName ?? "", .foreground(UIColor(hexString: "#BCBCBC")))
             """
            
            contentLabel.attributed.text = """
            \(logModel.text ?? "", .foreground(UIColor.white))
            """
        }
        headImageView.sd_setImage(with: NSURL(string: logModel.avatar ?? "") as URL?, placeholderImage: UIImage(named: "icon_head"))
        
        if logModel.seat == 0 {
            seatLabel.isHidden = true
        } else {
            seatLabel.isHidden = false
            seatLabel.attributed.text = .init("""
              \(.image(#imageLiteral(resourceName: "icon_chat"), .custom(size: CGSize(width: 10, height: 10)))) \(logModel.seat)号座
             """)
        }
    }
}

class ARImageCell: UITableViewCell {
    
    @IBOutlet weak var headImageView: UIImageView!
    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var logImageView: UIImageView!
    @IBOutlet weak var seatLabel: UILabel!
    
    func update(logModel: ARLogModel) {
        
        var userName = logModel.userName
        (userName == nil) ? userName = "" : nil
        if logModel.uid == UserDefaults.string(forKey: .uid) {
            nameLabel.attributed.text = """
             \(logModel.userName ?? "", .foreground(UIColor(hexString: "#FF4316")))
             """
        } else {
            nameLabel.attributed.text = """
             \(logModel.userName ?? "", .foreground(UIColor(hexString: "#BCBCBC")))
             """
        }
        
        
        headImageView.sd_setImage(with: NSURL(string: logModel.avatar ?? "") as URL?, placeholderImage: UIImage(named: "icon_head"))
        logImageView.sd_setImage(with: NSURL(string: logModel.imageUrl ?? "") as URL?, placeholderImage: nil)
        
        let imageWH = (logModel.imageWidth > logModel.imageHeight) ? true : false
        
        let width = imageWH ? (ARScreenWidth * 0.54) : (ARScreenWidth * 0.296)
        let multipliedBy = imageWH ? 0.667 : 1.5
        logImageView.snp_remakeConstraints { (make) in
            make.top.equalTo(nameLabel.snp_bottom).offset(8)
            make.left.equalTo(nameLabel.snp_left)
            make.width.equalTo(width)
            make.height.equalTo(logImageView.snp_width).multipliedBy(multipliedBy)
            make.bottom.equalToSuperview().offset(-15)
        }
        
        if logModel.seat == 0 {
            seatLabel.isHidden = true
        } else {
            seatLabel.isHidden = false
            seatLabel.attributed.text = .init("""
              \(.image(#imageLiteral(resourceName: "icon_chat"), .custom(size: CGSize(width: 10, height: 10)))) \(logModel.seat)号座
             """)
        }
    }
}

class LogViewController: UITableViewController {

    private lazy var list = [ARLogModel]()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 220
        tableView.autoresizesSubviews = true
        tableView.autoresizingMask = .flexibleWidth
        list.append(ARLogModel(text: "系统：严禁传播违法违规、低俗色情、血腥暴力、造谣诈骗等不良信息。欢迎同学监督不良行为，净化学习环境，营造绿色自习室！", status: .system))
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return list.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let logModel = list[indexPath.row]
        if logModel.status == .image {
            let cell = tableView.dequeueReusableCell(withIdentifier: "ImageCell", for: indexPath) as! ARImageCell
            cell.update(logModel: logModel)
            return cell
        } else if logModel.status == .text {
            let cell = tableView.dequeueReusableCell(withIdentifier: "ChatCell", for: indexPath) as! ARChatCell
            cell.update(logModel: logModel)
            return cell
        } else {
            let cell = tableView.dequeueReusableCell(withIdentifier: "LogCell", for: indexPath) as! LogCell
            cell.update(logModel: logModel)
            return cell
        }
    }
}

extension LogViewController {
    func log(logModel: ARLogModel) {
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.25) {
            self.list.append(logModel)
            let index = IndexPath(row: self.list.count - 1, section: 0)
            self.tableView.insertRows(at: [index], with: .automatic)
            self.tableView.scrollToRow(at: index, at: .middle, animated: false)
        }
    }
}
