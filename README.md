   Language : Kotlin
   Jetpack components used (Room DB, Data Binding)


BLECodingChallenge
    Connectivity with bluetooth device, and send the data to one from another, and saved the received data into Local Memory by using ROOM DB,
    and on network sending the saved data to firebase server, once data updated in server, one push notification show on device.

  Steps:

  1) Install the same application in two different mobile. (Build SDK must be less than ANDROID 12)
  2) Open Application in both the device
  3) Click in one the bluetooth icon, to make it searchable, 
  4) Click menu on another mobile, to get the paired devices list, select the 1st mobile in that list, now you will see the devices is connected.
  5) Now write the course details,and press the send butoon, same time you may see the same data isreflecting second device, and same time the data would
  be saved in local database here using Room DB for this.
  6) Click on sync button, the data will save into firebase server, 
  7) Once updation of data be done in server, you will receive a push notification in your device.
