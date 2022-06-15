package common

import java.text.SimpleDateFormat

typealias Message = String
typealias HostName = String

class Utils {
    companion object {
        var basePath = "/Users/mukkhakimov/Documents/itmo/thesis/results"
        var keysPath = "/Users/mukkhakimov/Documents/itmo/thesis"
        var timestampFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS")

        fun padLeftZeros(inputString: String, length: Int): String {
            if (inputString.length >= length) {
                return inputString
            }
            val sb = StringBuilder()
            while (sb.length < length - inputString.length) {
                sb.append('0')
            }
            sb.append(inputString)
            return sb.toString()
        }
    }
}
