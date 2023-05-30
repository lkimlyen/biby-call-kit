package com.connectycube.flutter.connectycube_flutter_call_kit

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject


class IncomingAcceptCallActivity : Activity() {
    private lateinit var callStateReceiver: BroadcastReceiver
    private lateinit var localBroadcastManager: LocalBroadcastManager

    private var callId: String? = null
    private var callType = -1
    private var callInitiatorId = -1
    private var callInitiatorName: String? = null
    private var callOpponents: ArrayList<Int>? = ArrayList()
    private var callUserInfo: String? = null
    private var actionAccept: Boolean = false


    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(resources.getIdentifier("activity_incoming_call", "layout", packageName))

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        processIncomingData(intent)
        initUi()
        initCallStateReceiver()
        registerCallStateReceiver()
        NotificationManagerCompat.from(this).cancel(callId.hashCode())

        Handler().postDelayed({
            startCall()
        }, 200)
    }

    private fun initCallStateReceiver() {
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        callStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null || TextUtils.isEmpty(intent.action)) return
                val action: String? = intent.action

                val callIdToProcess: String? = intent.getStringExtra(EXTRA_CALL_ID)
                if (TextUtils.isEmpty(callIdToProcess) || callIdToProcess != callId) {
                    return
                }
                when (action) {
                    ACTION_CALL_NOTIFICATION_CANCELED, ACTION_CALL_REJECT, ACTION_CALL_ENDED -> {
                        finishAndRemoveTask()
                    }

                    ACTION_CALL_ACCEPT -> finishDelayed()
                }
            }
        }
    }

    private fun finishDelayed() {
        Handler(Looper.getMainLooper()).postDelayed({
            finishAndRemoveTask()
        }, 1000)
    }

    private fun registerCallStateReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_CALL_NOTIFICATION_CANCELED)
        intentFilter.addAction(ACTION_CALL_REJECT)
        intentFilter.addAction(ACTION_CALL_ACCEPT)
        intentFilter.addAction(ACTION_CALL_ENDED)
        localBroadcastManager.registerReceiver(callStateReceiver, intentFilter)
    }

    private fun unRegisterCallStateReceiver() {
        localBroadcastManager.unregisterReceiver(callStateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unRegisterCallStateReceiver()
    }

    private fun processIncomingData(intent: Intent) {
        callId = intent.getStringExtra(EXTRA_CALL_ID)
        callType = intent.getIntExtra(EXTRA_CALL_TYPE, -1)
        callInitiatorId = intent.getIntExtra(EXTRA_CALL_INITIATOR_ID, -1)
        callInitiatorName = intent.getStringExtra(EXTRA_CALL_INITIATOR_NAME)
        callOpponents = intent.getIntegerArrayListExtra(EXTRA_CALL_OPPONENTS)
        callUserInfo = intent.getStringExtra(EXTRA_CALL_USER_INFO)
        Log.d("yenyen", callUserInfo.toString())

    }

    private fun initUi() {
        val isRecallTitleTxt: TextView =
            findViewById(resources.getIdentifier("is_recall", "id", packageName))
        val callTitleTxt: TextView =
            findViewById(resources.getIdentifier("user_name_txt", "id", packageName))
        val price: TextView =
            findViewById(resources.getIdentifier("user_price_txt", "id", packageName))


        val avatar: CircleImageView =
            findViewById(resources.getIdentifier("user_avatar", "id", packageName))
        var icCall: ImageView = findViewById(resources.getIdentifier("ic_call", "id", packageName))
        var obj = JSONObject(callUserInfo)
        var action = obj?.getInt("action")
        var caller = obj?.getString("caller")!!
        var callerObj = JSONObject(caller);
        var callerAvatar = callerObj?.getString("avatar")!!
        if (obj.has("message")) {
            isRecallTitleTxt.visibility = View.VISIBLE
        }

        if (action == 22) {
            var uri = "@drawable/ic_call"
            var imageResource = resources.getIdentifier(uri, null, packageName)
            var res = applicationContext.getDrawable(imageResource);
            icCall.setImageDrawable(res)
        }

        Glide.with(this).load(callerAvatar).placeholder(R.drawable.default_avatar)
            .into(avatar)
        callTitleTxt.text = callInitiatorName
        price.text = "${obj?.getString("pay_per_minute")} Dool / min"
    }

    // calls from layout file
    fun onEndCall(view: View?) {
        val bundle = Bundle()
        bundle.putString(EXTRA_CALL_ID, callId)
        bundle.putInt(EXTRA_CALL_TYPE, callType)
        bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
        bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
        bundle.putIntegerArrayList(EXTRA_CALL_OPPONENTS, callOpponents)
        bundle.putString(EXTRA_CALL_USER_INFO, callUserInfo)

        val endCallIntent = Intent(this, EventReceiver::class.java)
        endCallIntent.action = ACTION_CALL_REJECT
        endCallIntent.putExtras(bundle)
        applicationContext.sendBroadcast(endCallIntent)
    }

    // calls from layout file
    fun onStartCall(view: View?) {
        startCall()
    }

    private fun startCall() {
        val bundle = Bundle()
        bundle.putString(EXTRA_CALL_ID, callId)
        bundle.putInt(EXTRA_CALL_TYPE, callType)
        bundle.putInt(EXTRA_CALL_INITIATOR_ID, callInitiatorId)
        bundle.putString(EXTRA_CALL_INITIATOR_NAME, callInitiatorName)
        bundle.putIntegerArrayList(EXTRA_CALL_OPPONENTS, callOpponents)
        bundle.putString(EXTRA_CALL_USER_INFO, callUserInfo)

        val startCallIntent = Intent(this, EventReceiver::class.java)
        startCallIntent.action = ACTION_CALL_ACCEPT
        startCallIntent.putExtras(bundle)


        val sharedPreference =  this.getSharedPreferences("FlutterSharedPreferences",MODE_PRIVATE)
        val editor = sharedPreference.edit()
        editor.putString("flutter.call_user_info",callUserInfo)
        editor.apply()

        applicationContext.sendBroadcast(startCallIntent)
    }
}
