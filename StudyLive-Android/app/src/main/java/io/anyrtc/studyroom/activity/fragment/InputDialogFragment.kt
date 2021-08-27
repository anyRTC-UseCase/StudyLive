package io.anyrtc.studyroom.activity.fragment

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import io.anyrtc.studyroom.R

class InputDialogFragment : DialogFragment() {

    private lateinit var inputListener: (String) -> Unit
    var textChangeListener: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val window = dialog?.window
        val view = layoutInflater.inflate(
            R.layout.dialog_input,
            window?.findViewById(android.R.id.content),
            false
        )
        window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            it.setGravity(Gravity.BOTTOM)
            it.setDimAmount(0f)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val etContent = view.findViewById<EditText>(R.id.input)
        etContent.run {
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }
        view.postDelayed({
            val inputManager: InputMethodManager =
                etContent.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.showSoftInput(etContent, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
        view.findViewById<TextView>(R.id.send).setOnClickListener {
            if (etContent.text.toString().isEmpty()) {
                Toast.makeText(view.context, "请输入要发送的内容", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                inputListener.invoke(etContent.text.toString())
                etContent.text.clear()
                dismiss()
            }
        }

        etContent.addTextChangedListener {
            it ?: return@addTextChangedListener
            textChangeListener?.invoke(it.toString())
        }
    }

    fun show(manager: FragmentManager, callback: (String) -> Unit) {
        this.inputListener = callback
        show(manager, "input")
    }
}