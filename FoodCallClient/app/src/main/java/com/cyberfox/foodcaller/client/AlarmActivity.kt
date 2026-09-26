package com.cyberfox.foodcaller.client
import android.os.Bundle
import android.graphics.Color
import android.view.*
import android.widget.*
import android.content.Intent
class AlarmActivity:android.app.Activity(){override fun onCreate(s:Bundle?){super.onCreate(s);setShowWhenLocked(true);setTurnScreenOn(true);val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(48,72,48,72);setBackgroundColor(Color.rgb(190,35,20))};b.addView(TextView(this).apply{text="ESSEN IST FERTIG";textSize=32f;setTextColor(Color.WHITE);gravity=Gravity.CENTER});b.addView(TextView(this).apply{text="Bitte gib kurz Bescheid.";textSize=18f;setTextColor(Color.WHITE);gravity=Gravity.CENTER;setPadding(0,24,0,32)});listOf("Jetzt" to "now","5 min" to "5","10 min" to "10","Später" to "later").forEach{(t,a)->b.addView(Button(this).apply{text=t;setOnClickListener{sendBroadcast(Intent(this@AlarmActivity,FoodAlertReceiver::class.java).setAction(a));if(a!="now")finish()}},LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,8,0,8)})};setContentView(b)}override fun onBackPressed(){moveTaskToBack(true)}}
