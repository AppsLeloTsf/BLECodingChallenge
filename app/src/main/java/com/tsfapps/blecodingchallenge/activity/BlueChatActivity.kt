package com.tsfapps.blecodingchallenge.activity

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.tsfapps.blecodingchallenge.R
import com.tsfapps.blecodingchallenge.databinding.ActivityBlueChatBinding
import com.tsfapps.blecodingchallenge.room.CourseModal
import com.tsfapps.blecodingchallenge.utils.Constants
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID


open class BlueChatActivity : AppCompatActivity() {
    private val database = Firebase.database
    private lateinit var myRef: DatabaseReference
    private lateinit var courseModal: CourseModal
    private lateinit var binding: ActivityBlueChatBinding
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var btArray: Array<BluetoothDevice?>
    private var sendReceive: SendReceive? = null
    private var REQUEST_ENABLE_BLUETOOTH = 1
    private  var courseName = ""
    private  var courseDesc = ""
    private  var courseDuration = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlueChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        myRef = database.getReference("CourseModal")

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!bluetoothAdapter.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH)
        }
        implementListeners()
        binding.btnSync.setOnClickListener {
            addDataToFirebase()

        }

    }

    @SuppressLint("SetTextI18n")
    private fun implementListeners() {
        binding.btnListDevices.setOnClickListener {
            val bt = bluetoothAdapter.bondedDevices
            val strings = arrayOfNulls<String>(bt.size)
            btArray = arrayOfNulls(bt.size)
            var index = 0
            if (bt.size > 0) {
                for (device in bt) {
                    btArray[index] = device
                    strings[index] = device.name
                    index++
                }
                val arrayAdapter = ArrayAdapter(
                    applicationContext,
                    android.R.layout.simple_list_item_1,
                    strings
                )
                binding.lvDeviceList.adapter = arrayAdapter
            }
        }
        binding.btnDiscoverable.setOnClickListener {
            if (!bluetoothAdapter.isEnabled) {
                val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH)
            }else{
                val serverClass = ServerClass()
                serverClass.start()
            }

        }
        binding.lvDeviceList.onItemClickListener =
            OnItemClickListener { adapterView, view, i, l ->
                val clientClass = ClientClass(btArray[i])
                clientClass.start()
                binding.tvStatus.text = "Connecting..."
            }
        binding.btnSendMessage.setOnClickListener {
             courseName = binding.idEdtCourseName.text.toString()
             courseDesc = binding.idEdtCourseDescription.text.toString()
             courseDuration = binding.idEdtCourseDuration.text.toString()
            val courseDetails = "$courseName\n$courseDesc\n$courseDuration"
            sendReceive?.write(courseDetails.toByteArray())
            startActivityForResult(intent, ADD_COURSE_REQUEST)
        }
    }

    @SuppressLint("SetTextI18n")
    var handler = Handler { msg ->
        when (msg.what) {
            STATE_LISTENING -> binding.tvStatus.text = "Listening"
            STATE_CONNECTING -> binding.tvStatus.text = "Connecting..."
            STATE_CONNECTED -> binding.tvStatus.text = "Connected"
            STATE_CONNECTION_FAILED -> binding.tvStatus.text = "Connection Failed"
            STATE_MESSAGE_RECEIVED -> {
                val readBuff = msg.obj as ByteArray
                val courseDetails = String(readBuff, 0, msg.arg1)
                binding.tvShowMessage.text = courseDetails
                saveCourse(courseDetails, courseDetails, courseDetails)

            }
        }
        true
    }

    private inner class ServerClass : Thread() {
        private var serverSocket: BluetoothServerSocket? = null

        init {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                    Companion.APP_NAME,
                    Companion.MY_UUID
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        override fun run() {
            var socket: BluetoothSocket? = null
            while (socket == null) {
                try {
                    val message = Message.obtain()
                    message.what = Companion.STATE_CONNECTING
                    handler.sendMessage(message)
                    socket = serverSocket!!.accept()
                } catch (e: IOException) {
                    e.printStackTrace()
                    val message = Message.obtain()
                    message.what = Companion.STATE_CONNECTION_FAILED
                    handler.sendMessage(message)
                }
                if (socket != null) {
                    val message = Message.obtain()
                    message.what = Companion.STATE_CONNECTED
                    handler.sendMessage(message)
                    sendReceive = SendReceive(socket)
                    sendReceive!!.start()
                    break
                }
            }
        }
    }

    private inner class ClientClass(device: BluetoothDevice?) : Thread() {
        private var socket: BluetoothSocket? = null

        init {
            try {
                socket = device?.createRfcommSocketToServiceRecord(Companion.MY_UUID)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        override fun run() {
            try {
                socket!!.connect()
                val message = Message.obtain()
                message.what = Companion.STATE_CONNECTED
                handler.sendMessage(message)
                sendReceive = SendReceive(socket!!)
                sendReceive!!.start()
            } catch (e: IOException) {
                e.printStackTrace()
                val message = Message.obtain()
                message.what = Companion.STATE_CONNECTION_FAILED
                handler.sendMessage(message)
            }
        }
    }

    inner class SendReceive(bluetoothSocket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream?
        private val outputStream: OutputStream?

        init {
            var tempIn: InputStream? = null
            var tempOut: OutputStream? = null
            try {
                tempIn = bluetoothSocket.inputStream
                tempOut = bluetoothSocket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
            inputStream = tempIn
            outputStream = tempOut
        }

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {
                    bytes = inputStream!!.read(buffer)
                    handler.obtainMessage(
                        Companion.STATE_MESSAGE_RECEIVED,
                        bytes,
                        -1,
                        buffer
                    ).sendToTarget()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        fun write(bytes: ByteArray?) {
            try {
                outputStream!!.write(bytes)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val ADD_COURSE_REQUEST = 1
        private const val EDIT_COURSE_REQUEST = 2
        const val STATE_LISTENING = 1
        const val STATE_CONNECTING = 2
        const val STATE_CONNECTED = 3
        const val STATE_CONNECTION_FAILED = 4
        const val STATE_MESSAGE_RECEIVED = 5
        private const val APP_NAME = "BTChat"
        private val MY_UUID = UUID.fromString("8ce255c0-223a-11e0-ac64-0803450c9a66")
    }


    private fun saveCourse(courseName: String, courseDescription: String, courseDuration: String) {
        val data = Intent()
        setResult(RESULT_OK, data)
        saveIntoFile(courseName, courseDescription, courseDuration)
        Toast.makeText(this, "Course has been saved to Room Database. ",
            Toast.LENGTH_SHORT).show()
    }
    private fun jsonConverter(courseName: String, courseDescription: String, courseDuration: String):
            JSONObject {
        val jsonObject =  JSONObject();
        jsonObject.put("courseName", courseName);
        jsonObject.put("courseDescription", courseDescription);
        jsonObject.put("courseDuration", courseDuration)
        return jsonObject
    }
    private fun saveIntoFile(courseName: String, courseDescription: String, courseDuration: String){
        val userString: String = jsonConverter(courseName, courseDescription, courseDuration).toString()
        val file = File(this.filesDir, "TSF_FILE")
        val fileWriter = FileWriter(file)
        val bufferedWriter = BufferedWriter(fileWriter)
        bufferedWriter.write(userString)
        bufferedWriter.close()
    }
    private fun addDataToFirebase(){
        myRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                myRef.setValue(courseModal)
                Toast.makeText(this@BlueChatActivity, "Course saved in server", Toast.LENGTH_SHORT).show()
                sendNotification()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w(Constants.TAG, "Failed to read value.", error.toException())
            }

        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_COURSE_REQUEST && resultCode == RESULT_OK) {
            courseModal = CourseModal(
                courseName, courseDesc, courseDuration
            )
            Toast.makeText(this, "Course saved", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Course not saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendNotification(){
        val channel = NotificationChannel("n", "n", NotificationManager.IMPORTANCE_DEFAULT)
        val manager: NotificationManager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, "n")
            .setContentText("TSF APPS")
            .setSmallIcon(R.drawable.ic_bluetooth_searching)
            .setAutoCancel(true)
            .setContentText("New Course Added")
        val managerCompact: NotificationManagerCompat = NotificationManagerCompat.from(this)
        managerCompact.notify(999, builder.build())
    }

}
