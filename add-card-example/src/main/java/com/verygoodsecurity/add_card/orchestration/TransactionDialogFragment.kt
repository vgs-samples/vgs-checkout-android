package com.verygoodsecurity.add_card.orchestration

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.gson.JsonParser
import com.verygoodsecurity.add_card.BuildConfig.CLIENT_HOST
import com.verygoodsecurity.add_card.BuildConfig.TRANSFERS_ENDPOINT
import com.verygoodsecurity.add_card.R
import com.verygoodsecurity.add_card.orchestration.network.HttpClient

class TransactionDialogFragment : DialogFragment() {

    private val tnt: String? by lazy {
        arguments?.getString(TNT)
    }

    private val code: Int by lazy {
        arguments?.getInt(CODE) ?: -1
    }

    private val body: String? by lazy {
        arguments?.getString(BODY)
    }

    private val applicationClient: HttpClient by lazy {
        HttpClient()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return setupDialog()
    }

    private fun setupDialog(): Dialog {
        return activity?.let { activity ->
            val view = activity.setupView()
            AlertDialog.Builder(activity)
                .setView(view)
                .create().also { transactionDialog ->
                    view?.apply {
                        okBtn.setOnClickListener {
                            transactionDialog.dismiss()
                        }
                        retryBtn.setOnClickListener {
                            transactionDialog.dismiss()
                            (activity as MainActivity).tryAgainCheckout()
                        }
                    }
                }

        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private lateinit var title: TextView
    private lateinit var message: TextView
    private lateinit var transactionProgressView: View
    private lateinit var failedImgView: View
    private lateinit var successImgView: View
    private lateinit var okBtn: View
    private lateinit var retryBtn: View

    private fun Context.setupView(): View? {
        val view = LayoutInflater.from(this).inflate(
            R.layout.dialog_payment_transaction_state,
            null
        ).also {
            title = it.findViewById(R.id.title)
            message = it.findViewById(R.id.message)
            transactionProgressView = it.findViewById(R.id.transactionProgressView)
            failedImgView = it.findViewById(R.id.failedImgView)
            successImgView = it.findViewById(R.id.successImgView)
            okBtn = it.findViewById(R.id.okBtn)
            retryBtn = it.findViewById(R.id.retryBtn)
        }

        if (code in 200..299) {
            transactionProgressView.visibility = View.VISIBLE
            startPaymentTransaction()
        } else {
            transactionProgressView.visibility = View.INVISIBLE
            failedImgView.visibility = View.VISIBLE
            okBtn.visibility = View.VISIBLE
            retryBtn.visibility = View.VISIBLE
        }

        return view
    }

    private fun startPaymentTransaction() {
        parseUserId().takeIf { it.isNotEmpty() }
            ?.let { userId ->
                applicationClient.enqueue(
                    CLIENT_HOST + TRANSFERS_ENDPOINT,
                    userId.mapToJson(),
                ) { code: Int, body: String? ->
                    handlePaymentResponse(code, body)
                }
            }
    }

    private fun handlePaymentResponse(code: Int, body: String?) {

        if (code in 200..299) {
            this@TransactionDialogFragment.activity?.runOnUiThread {
                title.text = "Thank You"
                message.visibility = View.VISIBLE
                message.text = "Your purchased was successful"
                transactionProgressView.visibility = View.INVISIBLE
                successImgView.visibility = View.VISIBLE
                okBtn.visibility = View.VISIBLE
            }
        } else {
            this@TransactionDialogFragment.activity?.runOnUiThread {
                title.text = "Transfer Failed"
                message.visibility = View.VISIBLE
                message.text = "Invalid transaction,\nplease check" +
                        " the submitted card information and try again."
                transactionProgressView.visibility = View.INVISIBLE
                failedImgView.visibility = View.VISIBLE
                retryBtn.visibility = View.VISIBLE
                okBtn.visibility = View.VISIBLE
            }
        }
    }

    private fun String.mapToJson(): String {
        return "{\"fi_id\":\"$this\"," +
                "\"tnt\":\"$tnt\"," +
                "\"amount\": 1," +
                "\"currency\": \"usd\"" +
                "}"
    }

    private fun parseUserId(): String {
        return if (body.isNullOrEmpty()) {
            ""
        } else {
            JsonParser
                .parseString(body)
                .asJsonObject
                .run {
                    takeIf { has("data") && get("data").isJsonObject }
                        ?.get("data")?.asJsonObject
                        ?.get("id")?.asString
                        ?: ""
                }
        }
    }

    companion object {
        const val CODE = "code"
        const val TNT = "tnt"
        const val BODY = "body"
    }
}

