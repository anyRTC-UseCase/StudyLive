package io.anyrtc.studyroom.widget

import android.util.Log
import androidx.appcompat.widget.AppCompatImageView
import coil.load
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import io.anyrtc.studyroom.R
import io.anyrtc.studyroom.utils.MyConst
import java.util.regex.Pattern

class HomeGridAdapter
    : BaseQuickAdapter<MyConst.RoomListItemBean, BaseViewHolder>(R.layout.item_room_card) {
    private val regex = Pattern.compile("\\d+").toRegex()

    private val iconIdArray =
        arrayOf(R.id.visitor_1, R.id.visitor_2, R.id.visitor_3, R.id.visitor_4)

    override fun convert(holder: BaseViewHolder, item: MyConst.RoomListItemBean) {
        holder.setText(R.id.num, regex.find(item.roomName)?.value)
        if (item.avatars.isNotEmpty()) item.avatars.forEachIndexed { index, url ->
            if (url.isNotBlank()) holder.getView<AppCompatImageView>(iconIdArray[index]).load(url)
        }
    }
}
