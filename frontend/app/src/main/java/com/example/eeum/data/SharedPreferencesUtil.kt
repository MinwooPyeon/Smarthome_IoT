package com.example.eeum.data

import android.content.Context
import android.content.SharedPreferences
//import com.example.eeum.data.model.dto.User
//import com.google.gson.Gson

class SharedPreferencesUtil (context: Context) {
    val SHARED_PREFERENCES_NAME = "no1_preference"
    val COOKIES_KEY_NAME = "cookies"

    private val KEY_ACCESS_TOKEN = "access_token"
    private val KEY_REFRESH_TOKEN = "refresh_token"

    var preferences: SharedPreferences =
        context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    fun setAccessToken(token: String) {
        preferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getAccessToken(): String? {
        return preferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun setRefreshToken(token: String) {
        preferences.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? {
        return preferences.getString(KEY_REFRESH_TOKEN, null)
    }
    //사용자 정보 저장
//    fun addUser(user: User){
//        val editor = preferences.edit()
//        editor.putString("id", user.id)
//        editor.putString("name", user.name)
//        editor.apply()
//    }
//
//    fun getUser(): User {
//        val id = preferences.getString("id", "")
//        if (id != ""){
//            val name = preferences.getString("name", "")
//            return User(id!!, name!!, "")
//        }else{
//            return User()
//        }
//    }

    //전체 초기화
    fun deleteUser(){
        //preference 지우기
        val editor = preferences.edit()
        editor.clear()
        editor.apply()
    }

    fun clearAuthSession() {
        preferences.edit()
            .remove("access_token")
            .remove("refresh_token")
            .remove("id")
            .remove("name")
            .apply()
        deleteUserCookie() // 쿠키 쓰면 필요할 때만 호출
    }

    fun addUserCookie(cookies: HashSet<String>) {
        val editor = preferences.edit()
        editor.putStringSet(COOKIES_KEY_NAME, cookies)
        editor.apply()
    }

    fun getUserCookie(): MutableSet<String>? {
        return preferences.getStringSet(COOKIES_KEY_NAME, HashSet())
    }

    fun deleteUserCookie() {
        preferences.edit().remove(COOKIES_KEY_NAME).apply()
    }

//    fun addNotice(info: String) {
//        val list = getNotice()
//
//        list.add(info)
//        val json = Gson().toJson(list)
//
//        preferences.edit().let {
//            it.putString("notice", json)
//            it.apply()
//        }
//    }
//
//    fun setNotice(list: MutableList<String>) {
//        preferences.edit().let {
//            it.putString("notice", Gson().toJson(list))
//            it.apply()
//        }
//    }

//    fun getNotice() : MutableList<String> {
//        val str = preferences.getString("notice", "")!!
//        val list = if(str.isEmpty()) mutableListOf<String>() else Gson().fromJson(str, MutableList::class.java) as MutableList<String>
//
//        return list
//    }


    //온보딩을 할지말지
    private val prefs = context.getSharedPreferences("no1_preference", Context.MODE_PRIVATE)
    private val KEY_ONBOARDING_DONE = "onboarding_done"
    fun setOnboardingDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply()
    }
    fun isOnboardingDone(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false)
    }

}