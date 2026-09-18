package com.example.util

import android.content.Context
import android.util.Base64
import com.example.model.TunnelConfig
import com.example.model.TunnelType
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object ConfigManager {

  private const val PREFS_NAME = "hn_tunnel_prefs"
  private const val KEY_ACTIVE_CONFIG = "active_config"
  private const val KEY_SAVED_PROFILES = "saved_profiles"
  private const val HNT_PREFIX = "HNTUNNEL#"

  val DEFAULT_PROFILES = listOf(
    TunnelConfig(
      id = "preset-1",
      name = "SG-01 High-Speed SSH Direct",
      type = TunnelType.SSH_DIRECT,
      sshHost = "sg-ssh.hnhost.net",
      sshPort = 22,
      sshUsername = "hntunnel_vip",
      sshPassword = "vip_password",
      noteMessage = "Fast direct SSH tunnel for low-latency gaming & browsing.",
      author = "HN Tunnel Team"
    ),
    TunnelConfig(
      id = "preset-2",
      name = "ID-02 Cloudflare SSL/SNI Bug",
      type = TunnelType.SSH_SSL_TLS,
      sshHost = "id-ssl.hnhost.net",
      sshPort = 443,
      sshUsername = "hntunnel_ssl",
      sshPassword = "ssl_password",
      sniHost = "cdn.cloudflare.net",
      payload = "CONNECT [host_port] HTTP/1.1[crlf]Host: [host][crlf]Connection: Keep-Alive[crlf][crlf]",
      noteMessage = "Optimized for SSL/TLS SNI bypass with Cloudflare CDN.",
      author = "HN Tunnel Team"
    ),
    TunnelConfig(
      id = "preset-3",
      name = "US-03 V2Ray VMess Secure CDN",
      type = TunnelType.V2RAY_VMESS,
      v2rayAddress = "us-v2ray.hnhost.net",
      v2rayPort = 443,
      v2rayUuid = "b831381d-6324-4d53-ad4f-8cda48b30811",
      v2raySecurity = "auto",
      v2rayNetwork = "ws",
      v2rayWsPath = "/v2ray-ws",
      v2raySni = "speed.cloudflare.com",
      v2rayTls = true,
      noteMessage = "VMess over WebSocket + TLS for censorship resistance.",
      author = "HN Tunnel Team"
    ),
    TunnelConfig(
      id = "preset-4",
      name = "HK-04 Trojan TLS High-Speed",
      type = TunnelType.TROJAN,
      v2rayAddress = "hk-trojan.hnhost.net",
      v2rayPort = 443,
      v2rayUuid = "trojan_secret_key_8899",
      v2raySni = "hk.fastcdn.me",
      v2rayTls = true,
      noteMessage = "Trojan protocol mimicking standard HTTPS traffic.",
      author = "HN Tunnel Team"
    ),
    TunnelConfig(
      id = "preset-5",
      name = "arsyad-vless-tls",
      type = TunnelType.V2RAY_VLESS,
      v2rayAddress = "id.dontol.ccwu.cc",
      v2rayPort = 443,
      v2rayUuid = "03567a4b-2056-4fd8-8826-0bca7d051760",
      v2rayFlow = "none",
      v2rayEncryption = "none",
      v2rayNetwork = "ws",
      v2rayHeaderType = "---",
      v2rayRequestHost = "id.dontol.ccwu.cc",
      v2rayWsPath = "/v2ray-ws",
      v2raySni = "listen.noice.id",
      v2rayTls = true,
      v2rayAllowInsecure = true,
      v2rayFingerprint = "chrome",
      noteMessage = "VLESS over WebSocket + TLS with SNI listen.noice.id",
      author = "Arsyad"
    )
  )

  fun getActiveConfig(context: Context): TunnelConfig {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val jsonStr = prefs.getString(KEY_ACTIVE_CONFIG, null)
    if (!jsonStr.isNullOrBlank()) {
      try {
        return TunnelConfig.fromJson(JSONObject(jsonStr))
      } catch (e: Exception) {
        // Fallback to default
      }
    }
    return DEFAULT_PROFILES[1] // Default to Cloudflare SSL/SNI
  }

  fun saveActiveConfig(context: Context, config: TunnelConfig) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_ACTIVE_CONFIG, config.toJson().toString()).apply()
  }

  fun getSavedProfiles(context: Context): List<TunnelConfig> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val jsonStr = prefs.getString(KEY_SAVED_PROFILES, null)
    if (!jsonStr.isNullOrBlank()) {
      try {
        val array = JSONArray(jsonStr)
        val list = mutableListOf<TunnelConfig>()
        for (i in 0 until array.length()) {
          list.add(TunnelConfig.fromJson(array.getJSONObject(i)))
        }
        if (list.isNotEmpty()) return list
      } catch (e: Exception) {
        // Fallback to presets
      }
    }
    return DEFAULT_PROFILES
  }

  fun saveProfiles(context: Context, profiles: List<TunnelConfig>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val array = JSONArray()
    profiles.forEach { array.put(it.toJson()) }
    prefs.edit().putString(KEY_SAVED_PROFILES, array.toString()).apply()
  }

  private fun encodeBase64(bytes: ByteArray): String {
    return try {
      Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (e: Throwable) {
      java.util.Base64.getEncoder().encodeToString(bytes)
    }
  }

  private fun decodeBase64(str: String): ByteArray {
    return try {
      Base64.decode(str, Base64.DEFAULT)
    } catch (e: Throwable) {
      java.util.Base64.getDecoder().decode(str.trim())
    }
  }

  /**
   * Export config to .hnt encoded string
   */
  fun exportToHnt(config: TunnelConfig): String {
    val json = config.toJson()
    val rawBytes = json.toString().toByteArray(StandardCharsets.UTF_8)
    val b64 = encodeBase64(rawBytes)
    return "$HNT_PREFIX$b64"
  }

  /**
   * Import config from raw input: .hnt, JSON, or vmess:// / vless:// / trojan://
   */
  fun importConfig(input: String): TunnelConfig {
    val trimmed = input.trim()

    // 1. Check HN Tunnel .hnt format
    if (trimmed.startsWith(HNT_PREFIX)) {
      val b64 = trimmed.substring(HNT_PREFIX.length)
      val decoded = String(decodeBase64(b64), StandardCharsets.UTF_8)
      return TunnelConfig.fromJson(JSONObject(decoded))
    }

    // 2. Check VMess link: vmess://base64
    if (trimmed.startsWith("vmess://", ignoreCase = true)) {
      val b64 = trimmed.substring("vmess://".length)
      val decoded = String(decodeBase64(b64), StandardCharsets.UTF_8)
      val json = JSONObject(decoded)
      return TunnelConfig(
        name = json.optString("ps", "VMess Import"),
        type = TunnelType.V2RAY_VMESS,
        v2rayAddress = json.optString("add", "127.0.0.1"),
        v2rayPort = json.optInt("port", 443),
        v2rayUuid = json.optString("id", ""),
        v2rayAlterId = json.optInt("aid", 0),
        v2rayNetwork = json.optString("net", "ws"),
        v2rayHeaderType = json.optString("type", "---"),
        v2rayRequestHost = json.optString("host", ""),
        v2rayWsPath = json.optString("path", "/"),
        v2raySni = json.optString("sni", json.optString("host", "")),
        v2rayTls = json.optString("tls", "") == "tls",
        v2rayFingerprint = json.optString("fp", "chrome")
      )
    }

    // 3. Check VLess link: vless://uuid@host:port?...#name
    if (trimmed.startsWith("vless://", ignoreCase = true)) {
      val uri = URI(trimmed)
      val userInfo = uri.userInfo ?: ""
      val host = uri.host ?: ""
      val port = if (uri.port > 0) uri.port else 443
      val fragment = uri.fragment ?: "VLess Import"

      val queryParams = parseQueryParams(uri.rawQuery ?: "")
      return TunnelConfig(
        name = URLDecoder.decode(fragment, "UTF-8"),
        type = TunnelType.V2RAY_VLESS,
        v2rayAddress = host,
        v2rayPort = port,
        v2rayUuid = userInfo,
        v2rayFlow = queryParams["flow"] ?: "none",
        v2rayEncryption = queryParams["encryption"] ?: "none",
        v2rayNetwork = queryParams["type"] ?: "ws",
        v2rayHeaderType = queryParams["headerType"] ?: "---",
        v2rayRequestHost = queryParams["host"] ?: host,
        v2rayWsPath = queryParams["path"] ?: "/",
        v2raySni = queryParams["sni"] ?: queryParams["host"] ?: host,
        v2rayTls = (queryParams["security"] == "tls" || queryParams["security"] == "reality"),
        v2rayAllowInsecure = queryParams["allowInsecure"] != "0",
        v2rayFingerprint = queryParams["fp"] ?: "chrome"
      )
    }

    // 4. Check Trojan link: trojan://password@host:port?...#name
    if (trimmed.startsWith("trojan://", ignoreCase = true)) {
      val uri = URI(trimmed)
      val pass = uri.userInfo ?: ""
      val host = uri.host ?: ""
      val port = if (uri.port > 0) uri.port else 443
      val fragment = uri.fragment ?: "Trojan Import"

      val queryParams = parseQueryParams(uri.rawQuery ?: "")
      return TunnelConfig(
        name = URLDecoder.decode(fragment, "UTF-8"),
        type = TunnelType.TROJAN,
        v2rayAddress = host,
        v2rayPort = port,
        v2rayUuid = pass,
        v2rayNetwork = queryParams["type"] ?: "tcp",
        v2rayHeaderType = queryParams["headerType"] ?: "---",
        v2rayRequestHost = queryParams["host"] ?: host,
        v2rayWsPath = queryParams["path"] ?: "/",
        v2raySni = queryParams["sni"] ?: host,
        v2rayTls = true,
        v2rayAllowInsecure = queryParams["allowInsecure"] != "0",
        v2rayFingerprint = queryParams["fp"] ?: "chrome"
      )
    }

    // 5. Try plain JSON
    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
      return TunnelConfig.fromJson(JSONObject(trimmed))
    }

    throw IllegalArgumentException("Unrecognized config format. Expected .hnt, vmess://, vless://, trojan://, or JSON.")
  }

  private fun parseQueryParams(query: String): Map<String, String> {
    val map = mutableMapOf<String, String>()
    if (query.isBlank()) return map
    val pairs = query.split("&")
    for (pair in pairs) {
      val idx = pair.indexOf("=")
      if (idx > 0) {
        val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
        val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
        map[key] = value
      }
    }
    return map
  }
}
