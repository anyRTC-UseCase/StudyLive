package io.anyrtc.studyroom.widget

import coil.load
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.imageview.ShapeableImageView
import io.anyrtc.studyroom.R
import io.anyrtc.studyroom.utils.MyConst

class UserInfoAdapter
    : BaseQuickAdapter<MyConst.RoomUserList, BaseViewHolder>(R.layout.item_user_info) {

    override fun convert(holder: BaseViewHolder, item: MyConst.RoomUserList) {
        holder.setText(R.id.nickname, item.userName)
        if (item.avatar.isNotEmpty())
            holder.getView<ShapeableImageView>(R.id.avatar).load(item.avatar)
        if (item.seat > 0) {
            holder.setText(R.id.seat_num, String.format("%d号座", item.seat))
        }
    }
}
