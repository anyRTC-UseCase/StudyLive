package io.anyrtc.studyroom.widget

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.util.getItemView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.imageview.ShapeableImageView
import io.anyrtc.studyroom.R
import io.anyrtc.studyroom.utils.MyConst

class ChatAdapter
    : BaseQuickAdapter<MyConst.ChatData, BaseViewHolder>(R.layout.item_chat_msg) {

    private companion object {
        const val NOTIFY = 0x10000666
    }

    override fun convert(holder: BaseViewHolder, item: MyConst.ChatData) {
        if (item.isNotify) {
            holder.setText(R.id.content, item.content)
            return
        }
        holder.setGone(R.id.chat_flag, !item.isStreamer)
        holder.setText(R.id.chat_flag, String.format("%d号座", item.streamerNum))
        if (item.img.isNotBlank()) {
            holder.getView<AppCompatImageView>(R.id.chat_img).load(item.img)
        }

        holder.getView<ShapeableImageView>(R.id.chat_avatar).also {
            it.load(item.avatar, builder = {
                listener { _, _ ->
                    recyclerView.postDelayed({
                        recyclerView.smoothScrollToPosition(super.data.size - 1)
                    }, 250)
                }
            })
            it.strokeColor = ColorStateList.valueOf(
                if (item.isSelf) Color.parseColor("#FF4316") else Color.parseColor("#736C6A")
            )
        }
        holder.setText(R.id.chat_nickname, item.nickname)
        holder.setText(R.id.chat_msg, item.content)

        if (item.isSelf) {
            val redColor = ContextCompat.getColor(context, R.color.chat_red)
            holder.setTextColor(R.id.chat_nickname, redColor)
            holder.setTextColor(R.id.chat_msg, redColor)
        } else {
            val whiteColor = ContextCompat.getColor(context, R.color.white)
            val defNicknameColor = Color.parseColor("#BCBCBC")
            holder.setTextColor(R.id.chat_nickname, defNicknameColor)
            holder.setTextColor(R.id.chat_msg, whiteColor)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (data[position].isNotify)
            return NOTIFY
        return super.getItemViewType(position)
    }

    override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        if (viewType == NOTIFY) {
            return createBaseViewHolder(parent.getItemView(R.layout.item_chat_notify))
        }
        return super.onCreateDefViewHolder(parent, viewType)
    }

    override fun addData(data: MyConst.ChatData) {
        super.addData(data)
        if (this.data.size > 50) {
            this.removeAt(0)
        }
        recyclerView.scrollToPosition(super.data.size - 1)
    }
}
