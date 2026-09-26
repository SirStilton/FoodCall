package com.cyberfox.foodcaller.client
import android.content.Context
object AlarmStore { private const val P="foodcall"; fun active(c:Context)=c.getSharedPreferences(P,0).getBoolean("active",false); fun id(c:Context)=c.getSharedPreferences(P,0).getString("id","") ?: ""; fun start(c:Context,id:String)=c.getSharedPreferences(P,0).edit().putBoolean("active",true).putString("id",id).apply(); fun clear(c:Context)=c.getSharedPreferences(P,0).edit().clear().apply() }
