package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class BugScanResult(
  val host: String,
  val port: Int,
  val responseCode: Int,
  val responseMessage: String,
  val latencyMs: Long,
  val serverHeader: String,
  val headers: Map<String, String>,
  val isSuccess: Boolean,
  val errorMsg: String? = null
)

data class NetworkInfo(
  val localIp: String,
  val connectionType: String,
  val isConnected: Boolean,
  val dnsServers: List<String>
)

object NetworkTools {

  suspend fun scanHost(host: String, port: Int = 443, method: String = "HEAD"): BugScanResult = withContext(Dispatchers.IO) {
    val cleanHost = host.trim().removePrefix("http://").removePrefix("https://")
    val scheme = if (port == 443) "https" else "http"
    val urlStr = "$scheme://$cleanHost:$port/"
    val startTime = System.currentTimeMillis()

    try {
      val url = URL(urlStr)
      val connection = url.openConnection() as HttpURLConnection
      connection.requestMethod = method
      connection.connectTimeout = 4000
      connection.readTimeout = 4000
      connection.instanceFollowRedirects = false
      connection.setRequestProperty("User-Agent", "HN-Tunnel/2.4 (Android)")
      connection.setRequestProperty("Host", cleanHost)

      if (connection is HttpsURLConnection) {
        // SNI is automatically sent by Android HttpsURLConnection for the hostname in the URL
      }

      val code = connection.responseCode
      val msg = connection.responseMessage ?: ""
      val latency = System.currentTimeMillis() - startTime
      val server = connection.getHeaderField("Server") ?: "Unknown"

      val headerMap = mutableMapOf<String, String>()
      for (i in 0..10) {
        val key = connection.getHeaderFieldKey(i)
        val value = connection.getHeaderField(i)
        if (key != null && value != null) {
          headerMap[key] = value
        }
      }

      connection.disconnect()

      BugScanResult(
        host = cleanHost,
        port = port,
        responseCode = code,
        responseMessage = msg,
        latencyMs = latency,
        serverHeader = server,
        headers = headerMap,
        isSuccess = true
      )
    } catch (e: Exception) {
      val latency = System.currentTimeMillis() - startTime
      BugScanResult(
        host = cleanHost,
        port = port,
        responseCode = 0,
        responseMessage = "Connection Failed",
        latencyMs = latency,
        serverHeader = "N/A",
        headers = emptyMap(),
        isSuccess = false,
        errorMsg = e.localizedMessage ?: "Timeout or unreachable"
      )
    }
  }

  fun getNetworkInfo(context: Context): NetworkInfo {
    var localIp = "127.0.0.1"
    var connType = "Offline"
    var isConnected = false
    val dnsList = mutableListOf<String>()

    try {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
      val activeNetwork = cm.activeNetwork
      val caps = cm.getNetworkCapabilities(activeNetwork)

      if (caps != null) {
        isConnected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        connType = when {
          caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
          caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular / Mobile Data"
          caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
          caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN Active"
          else -> "Connected"
        }
      }

      val linkProps = cm.getLinkProperties(activeNetwork)
      linkProps?.dnsServers?.forEach {
        dnsList.add(it.hostAddress ?: "")
      }

      // Find non-loopback IP
      val interfaces = NetworkInterface.getNetworkInterfaces()
      while (interfaces.hasMoreElements()) {
        val intf = interfaces.nextElement()
        val addrs = intf.inetAddresses
        while (addrs.hasMoreElements()) {
          val addr = addrs.nextElement()
          if (!addr.isLoopbackAddress && addr is Inet4Address) {
            localIp = addr.hostAddress ?: "127.0.0.1"
            break
          }
        }
      }
    } catch (e: Exception) {
      // Fallback
    }

    return NetworkInfo(
      localIp = localIp,
      connectionType = connType,
      isConnected = isConnected,
      dnsServers = if (dnsList.isNotEmpty()) dnsList else listOf("8.8.8.8", "8.8.4.4")
    )
  }
}
