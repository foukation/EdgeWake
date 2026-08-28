package com.example.device_control;

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.telephony.TelephonyManager
import android.text.TextUtils
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.net.toUri
import com.example.device_control.alarm.AlarmParser
import com.example.device_control.alarm.AlarmScheduler
import com.example.device_control.data.AppData
import com.example.device_control.utils.CalculatorUtils
import com.example.device_control.utils.RecorderLauncher
import com.fxzs.lingxiagent.util.AppPermissionRequestManager
import com.google.gson.GsonBuilder

open class SchedulerManager(active: Activity):BaseManager() {

    private var context = active
    private var intentStr = ""

    private val CALL_PERMISSION: Int = 1
    private val READ_CONTACT: Int = 2
    private val SEND_MESSAGE: Int = 3
    private val PICK_IMAGE: Int = 4
    private val REQUEST_CODE = 1001
    private val CAMERA_PERMISSION = 2

    fun updateIntent(intentStr:String){
        this.intentStr = intentStr
    }
    /*
     * 初始化意图结果
     * */
    override fun start(): AgentResult {
        val intentResult = GsonBuilder().create()?.fromJson("{\"intents\":${intentStr}}", IntentsContent::class.java)
        val intent = intentResult?.intents?.get(0)
        val domain = intent?.domain ?:""
        if (intent != null) {
            return if (domain.contains(IntentDomain.TELECOM_SERVICE.alias) || domain.contains(IntentDomain.MEMBERSHIP.alias)){//电信服务  客户服务 会员权益
                execTelecomServiceAgent(intent)
            }
            else if (domain == IntentDomain.CUSTOMERSERVICE.alias){
                openCustomerService(intent.intent,intent.slotsSourceMap)
            }
            else if (domain == IntentDomain.HEALTHCARE.alias){//移动爱家
                openHealthWeb(intent.intent,intent.slotsSourceMap)
            }
            else{//系统控制
                execAgent(intent.intent, intent.slotsSourceMap)
            }

        }
        return AgentResult(false,"暂时不支持相关操作")
    }

    /*
     * 执行agent动作
     * */
    fun execAgent (intentStr: String, slot: SlotsData): AgentResult {
        when (intentStr) {

            "CarControl.Light" -> {
                val action = slot.Action
                if (action == "On") {
                    return flagFlashlight(true)
                }
                if (action == "Off") {
                    return flagFlashlight(false)
                }
            }

            "CarControl.DashCam" -> {
                val action = slot.Action
                if (action == "On") {
                    return openDashCam()
                }
            }

            "SystemControl.Volume" -> {
                val action = slot.Action
                if (action == VolumeType.UP.alias) {
                    return setVolume(VolumeType.UP)
                }
                if (action == VolumeType.DOWN.alias) {
                    return setVolume(VolumeType.DOWN)
                }
            }

            "SystemControl.Page" -> {
                val action = slot.Action
                val name = slot.Name
                val page = slot.Page
                if (name != null){
                    if (name.contains("手电")) {
                        if (action == "Open") {
                            return flagFlashlight(true)
                        }
                        if (action == "Off" || action == "Close") {
                            return flagFlashlight(false)
                        }
                    }
                    if (name == "蓝牙") {
                        return enableBluetooth(action ?:"")
                    }
                }

                if (page != null){
                    if ("蓝牙" == page){
                        return enableBluetooth(action ?:"")
                    }

                    if (page.contains("手电")) {
                        if (action == "Open") {
                            return flagFlashlight(true)
                        }
                        if (action == "Off" || action == "Close") {
                            return flagFlashlight(false)
                        }
                    }
                }

            }

            "SystemControl.Display" -> {
                val action = slot.Action
                val function = slot.Function
                val brightness = slot.Brightness
                if (action == "Increase" || action == "IncreaseLittle" || action == "Set" ) {
                    if (brightness != null){
                        return setBrightness(brightness.toInt())
                    }
                    return setBrightness(50)
                }
                if (action == "Decrease" || action == "DecreaseLittle") {
                    if (brightness != null){
                        return setBrightness(-brightness.toInt())
                    }
                    return setBrightness(-50)
                }

                if (function != null && function == "Torch"){//手电筒
                    if (action == "On") {
                        return flagFlashlight(true)
                    }
                    if (action == "Off") {
                        return flagFlashlight(false)
                    }
                }
            }

            "SystemControl.APP" -> {
                val action = slot.Action
                val name = slot.Name
                if (action == "Open") {
                    if (appInfoList != null) {
                        val appData = appInfoList?.firstOrNull { name?.contains(it.name, ignoreCase = true) == true }
                        if (appData != null){
                            return openWeChat(appData)
                        }

                    }
                    if (name == "照片" || name =="相册") {
                        return openAlbum()
                    }
                }
                if (name == "蓝牙"){
                    return enableBluetooth(action ?:"")
                }

                if (name == "手电筒"){
                    if (action == "Open") {
                        return flagFlashlight(true)
                    }
                    if (action == "Close") {
                        return flagFlashlight(false)
                    }
                }

                if (name == "计算器"){
                    if (action == "Open") {
                        return CalculatorUtils.openSystemCalculator(context)
                    }
                }
                if (name == "手机壁纸"){//无法实现
//                    if (action == "Open") {
//                        openMiuiPersonalizeWallpaper(context)
//                    }
                    return AgentResult(false, "暂时不支持相关操作")
                }

                if (name == "录音"){//无法实现
                    if (action == "Open") {
                        return RecorderLauncher.launch(context)
                    }
                }

                return AgentResult(false, "暂时不支持相关操作")

            }

            "Phone.Call" -> {
                val action = slot.Action
                val contact = slot.Contact
                val phoneNumber = slot.PhoneNumber
                if (action == "Call") {
                    if (contact != null) {
                        return makeCall(contact)
                    }

                    if (phoneNumber != null){
                        return makeCall(phoneNumber)
                    }
                }

                if (action == "Search"){//获取联系人手机号
                    return getCallPhone(contact ?:"")
                }
            }

            "Phone.Msg" -> {
                val action = slot.Action
                val contact = slot.Contact
                val phoneNumber = slot.PhoneNumber
                if (action == "Send") {
                    if (contact != null || phoneNumber != null ) {
                        val contactDetails = contact ?: phoneNumber ?: ""
                        return sendSMS(contactDetails, slot.Text ?:"")
                    }
                    return AgentResult(false, "联系人不能为空")
                }
            }

            "Alarm.Manage" ->{//闹钟
               return execAlarm(intentStr,slot)
            }
            "SystemControl.Lighting" ->{//手电筒
                val action = slot.Action
                val light = slot.Light
                if (light != null && light == "Torches"){
                    if (action == "On") {
                        return flagFlashlight(true)
                    }
                    if (action == "Off") {
                        return flagFlashlight(false)
                    }
                }


            }


            else -> return AgentResult(false, "暂时不支持相关操作")
        }

        return AgentResult(false)
    }


    /**
     * 设置闹钟
     */
    private fun execAlarm(intentStr: String,slot: SlotsData): AgentResult {
        if (intentStr == AlarmIntent.ALARM_MANAGE.intent){

           val alarmConfig = AlarmParser.analysisAlarmTime(slot)
            if (alarmConfig.hour == -1 || alarmConfig.minute == -1){
                return AgentResult(true, sucMsg ="暂不支持闹钟时间段")
            }
            if (alarmConfig.hour == 0 && alarmConfig.minute == 0){
                return AgentResult(true, sucMsg = "请提供正确的闹钟时间")
            }
            alarmConfig.text = slot.Text?:""
            val alarmScheduler = AlarmScheduler()
            alarmScheduler.schedule(context,alarmConfig)
            return AgentResult(true, sucMsg ="已为你设置好闹钟")
        }
        return AgentResult(false, errMsg = "闹钟设置失败")
    }

    /*
   * 执行电信服务agent动作
   * */
    fun execTelecomServiceAgent (intentStr: IntentData): AgentResult {
         val action = intentStr.slotsSourceMap.Action ?: return AgentResult(false)
        val amount = intentStr.slotsSourceMap.Amount
        val contact = intentStr.slotsSourceMap.Contact
        when (intentStr.intent) {
            TelecomServiceInstruction.TELECOMSERVICE_MOBILEPHONE.intent -> { //充值话费 先固定跳转action  目前不支持流量共享
                when (action) {
                    TelecomServiceAction.ACTION_RECHARGEPHONECREDIT.ation -> {
                        if (amount != null){
                            return openChinaMobileNavigation("充值"+amount+"元话费")
                        }
                        return openChinaMobileNavigation("充值话费")
                    }
                    TelecomServiceAction.ACTION_CHECKBALANCE.ation -> {
                        return openChinaMobileNavigation("查询话费")
                    }
                    TelecomServiceAction.ACTION_CHECKDATAUSAGE.ation -> {
                        return openChinaMobileNavigation("查询流量")
                    }
                }

            }

            TelecomServiceInstruction.MEMBERSHIP_GOTONE.intent -> { //会员权益
                if (action == TelecomServiceAction.ACTION_RIGHTSINQUIRY.ation){
                    return openChinaMobileNavigation("查询全球通权益")
                }

            }
            else -> AgentResult(false, "暂时不支持相关操作")
        }
        return AgentResult(false)
    }

    /*
     * 打开微信
     * */
    fun openWeChat(appData:AppData): AgentResult {
        return if (isWeChatInstalled(context,appData.packageName)){
            val packageManager: PackageManager = context.packageManager
            val intentFormat = packageManager.getLaunchIntentForPackage(appData.packageName)
            context.startActivity(intentFormat)
            AgentResult(true, sucMsg = "已为您打开"+appData.name)
        }else{
            AgentResult(true, sucMsg = "未安装"+appData.name)
        }

    }

    /**
     * 是否安装微信
     */
    private fun isWeChatInstalled(context: Context,appPackage: String): Boolean {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo(appPackage, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }


    /*
     * 打开相册
     * */
    fun openAlbum(): AgentResult {
        val intentFormat = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        context.startActivityForResult(intentFormat, PICK_IMAGE)
        return AgentResult(true, sucMsg = "已为您打开手机相册")
    }

    /*
     * 打开相机
     * */
    fun openDashCam(): AgentResult {
        if (checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            AppPermissionRequestManager.requestCameraPermission( context,CAMERA_PERMISSION)
            return AgentResult(false, sucMsg = "请先开启相机权限")
        }
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(context, takePictureIntent, CAMERA_PERMISSION, null)
        return AgentResult(true, sucMsg = "已为您打开手机相机")
    }


    /*
     * 开启蓝牙
     * */
    fun enableBluetooth(action:String): AgentResult {
        if (checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            AppPermissionRequestManager.requestBluetoothPermission( context,REQUEST_CODE)
            return AgentResult(false,"请先申请权限")
        } else {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null) {
                if (checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return AgentResult(false,"请先申请权限")
                }
                if (action == "Close"){
                    if (bluetoothAdapter.isEnabled){
                        bluetoothAdapter.disable()
                        return AgentResult(true, sucMsg = "已为您关闭手机蓝牙")
                    }
                    return AgentResult(true,sucMsg ="已为您关闭手机蓝牙")
                }

                if (action == "Open"){
                    if (!bluetoothAdapter.isEnabled){
                        bluetoothAdapter.enable()
                        return AgentResult(true,sucMsg ="已为您开启手机蓝牙")
                    }
                    return AgentResult(true,sucMsg ="已为您开启手机蓝牙")
                }

            }
            return AgentResult(false,"暂时不支持相关操作")
        }
    }

    /*
     * 打开/关闭手电筒
     * */
    private fun flagFlashlight(flag: Boolean): AgentResult {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.firstOrNull {
            val characteristics = cameraManager.getCameraCharacteristics(it)
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK &&
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return AgentResult(false,"暂时不支持相关操作")
        cameraManager.setTorchMode(cameraId, flag)
        return AgentResult(true, sucMsg = "已为您${if (flag) "开启" else "关闭"}手电筒")
    }

    /*
     * 调整音量
     * */
    private fun setVolume(type: VolumeType): AgentResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (type == VolumeType.UP) {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            )
            return AgentResult(true, sucMsg = "已为您调整手机音量")
        }
        if (type == VolumeType.DOWN) {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            )
            return AgentResult(true, sucMsg = "已为您调整手机音量")
        }
        if (type == VolumeType.MUTE) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_NOTIFICATION_POLICY) != PackageManager.PERMISSION_GRANTED) {
                return AgentResult(false, sucMsg = "请先申请权限")
            }
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            return AgentResult(true, sucMsg = "已为您调整为静音模式")
        }
        return AgentResult(false, "暂时不支持相关操作")
    }

    /*
     * 调整屏幕亮度
     * */
    private fun setBrightness(brightnessValue: Int): AgentResult {
        if (!Settings.System.canWrite(context)){
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.setData(Uri.parse("package:" + context.packageName))
            context.startActivity(intent)
            return AgentResult(true, sucMsg = "请先允许修改系统设置")
        }
        val currentBrightness = getCurrentBrightness()
        if (Settings.System.canWrite(context)) {
            val contentResolver = context.contentResolver
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                currentBrightness + brightnessValue
            )

            val layoutParams = context.window.attributes
            layoutParams.screenBrightness = (currentBrightness + brightnessValue) / 255.0f
            context.window.attributes = layoutParams
            return AgentResult(true, sucMsg = "已为您调整手机屏幕亮度")
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.setData(Uri.parse("package:" + context.packageName))
            context.startActivity(intent)
            return AgentResult(true, sucMsg = "已为您调整手机屏幕亮度")
        }
    }

    /*
     * 获取当前屏幕亮度
     * */
    private fun getCurrentBrightness(): Int {
        val contentResolver = context.contentResolver
        return try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: SettingNotFoundException) {
            128
        }
    }

    /*
     * 检测SIM卡是否可用
     * */
    @SuppressLint("ServiceCast")
    private fun isSimCardPresent(): Boolean {

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val simState = telephonyManager.simState

        // TelephonyManager.SIM_STATE_ABSENT 表示SIM卡未插入
        // TelephonyManager.SIM_STATE_UNKNOWN 表示未知状态，通常表示设备不支持SIM卡
        // 其他状态（如 TelephonyManager.SIM_STATE_READY）表示SIM卡已插入且可用
        return simState != TelephonyManager.SIM_STATE_ABSENT && simState != TelephonyManager.SIM_STATE_UNKNOWN
    }

    private fun isSimPermission():Int{
        // 权限被拒绝
        val permanentlyDenied = !ActivityCompat.shouldShowRequestPermissionRationale(context,
            Manifest.permission.READ_PHONE_STATE)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ){
            AppPermissionRequestManager.requestReadPhoneStatePermission(context,7)
//            if (permanentlyDenied){
//                return 2
//            }
            return 1
        }
        return 0

    }

    /*
     * 读取通讯录
     * */
    @SuppressLint("Range")
    private fun readContacts( name: String): String {
        val contentResolver = context.contentResolver
        val uri = ContactsContract.Contacts.CONTENT_URI
        val cursor = contentResolver.query(uri, null, null, null, null)
        var result = ""

        if (cursor != null && cursor.count > 0) {
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                val mName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                if (mName == name) {
                    if (cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                        val phoneCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(id),
                            null
                        )

                        while (true) {
                            checkNotNull(phoneCursor)
                            if (!phoneCursor.moveToNext()) break
                            val phoneNumber =
                                    phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            result = phoneNumber
                        }
                        phoneCursor.close()
                    }
                    return result
                }
            }
            cursor.close()
        }

        return result
    }

    /*
     * 电话前条件验证
     * */
    private fun makeCall(name: String): AgentResult {
        var phoneNumber: String
        val permission = isSimPermission()
        if (permission > 0 ){
            return AgentResult(false,"请先申请权限")
        }

        if (!isSimCardPresent()) {
            return AgentResult(false,"通信不可用，请检查SIM是否可用")
        }

        if (checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) != PackageManager.PERMISSION_GRANTED
        ) {
            AppPermissionRequestManager.requestReadContactsPermission(context,READ_CONTACT)
            return AgentResult(false,"请先申请权限")
        } else {
            phoneNumber = readContacts(name)

            if (TextUtils.isEmpty(phoneNumber) && TextUtils.isEmpty(name)) {
                return AgentResult(false,"不在您的通讯录名单中，拨打电话不成功")
            }
            if (TextUtils.isEmpty(phoneNumber)){
                phoneNumber = name;
            }

            if (checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
            ) {
                AppPermissionRequestManager.requestCallContactsPermission(context,CALL_PERMISSION)
                return AgentResult(false,"请先申请权限")
            } else {
                return makePhoneCall(phoneNumber)
            }
        }
    }

    /*
     * 打电话
     * */
    @SuppressLint("QueryPermissionsNeeded")
    private fun makePhoneCall(phoneNumber: String): AgentResult {
        val intent = Intent(Intent.ACTION_CALL)
        intent.setData(Uri.parse("tel:$phoneNumber"))
        if (intent.resolveActivity(context.applicationContext.packageManager) != null) {
            context.startActivity(intent)
            return AgentResult(true, sucMsg = "已为您拨打电话")
        }
        return AgentResult(false,"拨打电话失败")
    }

    /*
     * 发送短信
     * */
    private fun sendSMS(name: String, message: String): AgentResult {

        val permission = isSimPermission()
        if (permission > 0 ){
            return AgentResult(false,"请先申请权限")
        }
        if (!isSimCardPresent()) {
            return AgentResult(false,"通信不可用，请检查SIM是否可用")
        }

        if (checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) != PackageManager.PERMISSION_GRANTED
        ) {
            AppPermissionRequestManager.requestSendSmsPermission(context,SEND_MESSAGE)
            return AgentResult(false,"请先申请权限")
        } else {
            var phoneNumber: String
            if (!isSimCardPresent()) {
                return AgentResult(false,"通信不可用，请检查SIM是否可用")
            }

            if (TextUtils.isEmpty(name)) {
                return AgentResult(false,"联系人不能为空")
            }

            if (checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
            ) {
                AppPermissionRequestManager.requestReadContactsPermission(context,READ_CONTACT)
                return AgentResult(false,"请先申请权限")
            } else {
                phoneNumber = readContacts(name)
                if (TextUtils.isEmpty(phoneNumber) && TextUtils.isEmpty(name) ) {
                    return AgentResult(false,"不在您的通讯录名单中，短信发送不成功")
                }
                if (TextUtils.isEmpty(phoneNumber)){
                    phoneNumber = name
                }

                try {
                    // 获取确切的响应结果需要注册广播接收器，目前暂时这样处理
//                    val smsManager = SmsManager.getDefault()
//                    smsManager.sendTextMessage(phoneNumber, null, message, null, null)

                    val intent = Intent(Intent.ACTION_SENDTO)
                    intent.setData("smsto:$phoneNumber".toUri()) // 注意：必须是 smsto: 而不是 tel:
                    intent.putExtra("sms_body", message)
                    if (intent.resolveActivity(context.getPackageManager()) != null) {
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    }

                    return AgentResult(true, sucMsg = "请您手动确认发送短信")
                } catch (e: Exception) {
                    println(e.message)
                    return AgentResult(false,"短信发送失败")
                }
            }
        }
    }

    /**
     * 跳转中国移动app,通过deeplink 唤醒移动app AI助理
     * 充值话费、查询话费、查询流量
     */
    private fun openChinaMobileNavigation(action:String):AgentResult{
        if (!isChinaMobileInstalled("com.greenpoint://android.mc10086.activity")){
            return AgentResult(false,"当前需要中国移动APP才能继续哦~去应用商店下载后，就能正常使用啦")
        }

        //需要判断是否安装移动、是否移动号码等？
        val url = "com.greenpoint://android.mc10086.activity?url="+PullTerminalUtils.urlEncode("https://chinamobileapp/native/common/qw2305190519?userAsk=$action")+"&codeNumber=99992408301553445_"+PullTerminalUtils.getDeviceSellerId()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        return AgentResult(true, sucMsg = "已为您打开中国移动App")

    }

    private fun isChinaMobileInstalled(appPackage:String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(appPackage))
        return intent.resolveActivity(context.packageManager) != null
    }

    /**
     * 打开移动爱家web
     */
    private fun openHealthWeb(intentStr: String, slot: SlotsData): AgentResult {
        var url = ""
        if (!isChinaMobileInstalled("com.cmri.universalapp://")) {
            if (intentStr == TelecomServiceInstruction.HEALTHCARE_LOVEHOME.intent) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(Constants.HEALTH_URL)
                context.startActivity(intent)
                return AgentResult(true, sucMsg = "已为您打开移动爱家健康服务")
            }
        } else {
            if (slot.Title != null) {
                if (slot.Title == "家庭医生") {
                    url = Constants.HEALTH_FAMILY
                } else if (slot.Title == "三甲") {
                    url = Constants.HEALTH_TERTIARY
                }
                if (!TextUtils.isEmpty(url)) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    return AgentResult(true, sucMsg = "已为您打开移动爱家App")
                }
            }

            if (slot.Action !=null && slot.Action  == TelecomServiceAction.ACTION_CONSULTATION.ation){
                url = Constants.HEALTH_FAMILY
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return AgentResult(true, sucMsg = "已为您打开移动爱家App")
            }
        }
        return AgentResult(false, "暂时不支持相关操作")
    }


    //专属客服
    private fun openCustomerService(intentStr: String, slot: SlotsData):AgentResult{
        if (!isAppInstalled(context,"com.tdtech.doubleosga")) {
            return AgentResult(true, sucMsg = "未安装专属客服App")
        } else {
            val intent = Intent()
            intent.setComponent(ComponentName("com.tdtech.doubleosga",
                "com.tdtech.doubleosga.DialogActivity" // 类名
            ))
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return AgentResult(true, sucMsg = "已为您打开专属客服App")
            } catch (e: ActivityNotFoundException) {
                return AgentResult(false, "暂时不支持相关操作")
            }
        }
    }

    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            return true // 已安装
        } catch (e: PackageManager.NameNotFoundException) {
            return false // 未安装
        }
    }


    /*
    * 获取手机号
    * */
    private fun getCallPhone(name: String): AgentResult {
        val phoneNumber: String
        val permission = isSimPermission()
        if (permission > 0 ){
            return AgentResult(false,"请先申请权限")
        }

        if (checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            AppPermissionRequestManager.requestReadContactsPermission(context,READ_CONTACT)
            return AgentResult(false,"请先申请权限")
        } else {
            phoneNumber = readContacts(name)

            if (TextUtils.isEmpty(phoneNumber) ) {
                return AgentResult(false,"该联系人不在您的通讯录名单中")
            }
            return AgentResult(true, sucMsg = name +"手机号是：$phoneNumber")

        }
    }

}