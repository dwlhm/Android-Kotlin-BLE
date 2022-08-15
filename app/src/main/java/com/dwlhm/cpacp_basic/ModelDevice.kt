package com.dwlhm.cpacp_basic

class ModelDevice(mac: String, date: String) {

        private var mac: String
        private var date: String

        init {
            this.mac = mac
            this.date = date
        }

    fun getMac(): String {
        return mac
    }

    fun getDate(): String {
        return date
    }

}