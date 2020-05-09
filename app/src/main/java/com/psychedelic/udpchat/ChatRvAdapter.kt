package com.psychedelic.udpchat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.psychedelic.udpchat.databinding.ChatItemFromBinding
import com.psychedelic.udpchat.databinding.ChatItemToBinding

const val CHAT_TYPE_SEND_TXT = 1
const val CHAT_TYPE_GET_TXT = CHAT_TYPE_SEND_TXT + 1

class ChatRvAdapter(context: Context, list: ArrayList<ChatEntity>, variableId: Int) :
    RecyclerView.Adapter<ChatRvAdapter.ViewHolder>() {
    private val mContext = context
    private var mList: ArrayList<ChatEntity> = list
    private val mVariableId = variableId

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var binding: ViewDataBinding? = null
        fun getBinding(): ViewDataBinding {
            return binding!!
        }

        fun setBinding(binding: ViewDataBinding) {
            this.binding = binding
        }
    }

    fun refreshData(list:ArrayList<ChatEntity>){
        mList = list
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return mList[position].fromWho
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (viewType == CHAT_TYPE_SEND_TXT) {
            val chatSendBinding = DataBindingUtil.inflate<ChatItemToBinding>(
                LayoutInflater.from(mContext),
                R.layout.chat_item_to,
                parent,
                false
            )
            val viewHolder = ViewHolder(chatSendBinding.root)
            viewHolder.setBinding(chatSendBinding)
            return viewHolder
        } else {
            val chatFromBinding = DataBindingUtil.inflate<ChatItemFromBinding>(
                LayoutInflater.from(mContext),
                R.layout.chat_item_from,
                parent,
                false
            )
            val viewHolder = ViewHolder(chatFromBinding.root)
            viewHolder.setBinding(chatFromBinding)
            return viewHolder
        }

    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.getBinding().setVariable(mVariableId, mList[position])
        holder.getBinding().executePendingBindings()
    }
}