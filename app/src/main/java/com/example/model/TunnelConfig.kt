package com.example.model

import org.json.JSONObject
import java.util.UUID

data class TunnelConfig(
  val id: String = UUID.randomUUID().toString(),
  val name: String = "Default HN Profile",
  val type: TunnelType = TunnelType.SSH_SSL_TLS,

  // SSH Fields
  val sshHost: String = "sg1.hnhost.net",
  val sshPort: Int = 443,
  val sshUsername: String = "hntunnel_free",
  val sshPassword: String = "123456",

  // Payload & Proxy & SNI
  val payload: String = "CONNECT [host_port] HTTP/1.1[crlf]Host: [host][crlf]X-Online-Host: [host][crlf]Connection: Keep-Alive[crlf][crlf]",
  val sniHost: String = "cdn.cloudflare.net",
  val remoteProxy: String = "",

  // V2Ray / Trojan Fields
  val v2rayAddress: String = "sg-v2ray.hnhost.net",
  val v2rayPort: Int = 443,
  val v2rayUuid: String = "b831381d-6324-4d53-ad4f-8cda48b30811",
  val v2rayAlterId: Int = 0,
  val v2raySecurity: String = "auto",
  val v2rayNetwork: String = "ws",
  val v2rayWsPath: String = "/v2ray-ws",
  val v2raySni: String = "cdn.cloudflare.net",
  val v2rayTls: Boolean = true,
  val v2rayFlow: String = "none",
  val v2rayEncryption: String = "none",
  val v2rayHeaderType: String = "---",
  val v2rayRequestHost: String = "",
  val v2rayAllowInsecure: Boolean = true,
  val v2rayFingerprint: String = "chrome",

  // DNS & Network Options
  val dnsProvider: String = "Google DNS",
  val customDns1: String = "8.8.8.8",
  val customDns2: String = "8.8.4.4",
  val enableUdp: Boolean = true,
  val udpgwPort: Int = 7300,
  val tlsVersion: String = "Auto", // "Auto", "TLSv1.2", "TLSv1.3"
  val sendBuffer: Int = 16384,
  val receiveBuffer: Int = 32768,
  val enableDnsForwarding: Boolean = true,
  val autoReconnect: Boolean = true,
  val autoPingEnabled: Boolean = true,
  val autoPingHost: String = "8.8.8.8",
  val autoPingIntervalSec: Int = 3,
  val routingMode: String = "DIRECT_TUNNEL", // "DIRECT_TUNNEL" (Internet Normal) or "GLOBAL_TUN"
  val bypassVpnApps: String = "", // Comma separated packages to bypass

  // Export / Lock Protection
  val isLocked: Boolean = false,
  val lockSsh: Boolean = false,
  val lockPayload: Boolean = false,
  val noteMessage: String = "Free high-speed configuration provided for HN Tunnel.",
  val expiryDate: String = "",
  val author: String = "HN Dev"
) {
  fun toJson(): JSONObject {
    val json = JSONObject()
    json.put("id", id)
    json.put("name", name)
    json.put("type", type.name)
    json.put("sshHost", sshHost)
    json.put("sshPort", sshPort)
    json.put("sshUsername", if (lockSsh) "" else sshUsername)
    json.put("sshPassword", if (lockSsh) "" else sshPassword)
    json.put("payload", if (lockPayload) "" else payload)
    json.put("sniHost", sniHost)
    json.put("remoteProxy", remoteProxy)
    json.put("v2rayAddress", v2rayAddress)
    json.put("v2rayPort", v2rayPort)
    json.put("v2rayUuid", if (lockSsh) "" else v2rayUuid)
    json.put("v2rayAlterId", v2rayAlterId)
    json.put("v2raySecurity", v2raySecurity)
    json.put("v2rayNetwork", v2rayNetwork)
    json.put("v2rayWsPath", v2rayWsPath)
    json.put("v2raySni", v2raySni)
    json.put("v2rayTls", v2rayTls)
    json.put("v2rayFlow", v2rayFlow)
    json.put("v2rayEncryption", v2rayEncryption)
    json.put("v2rayHeaderType", v2rayHeaderType)
    json.put("v2rayRequestHost", v2rayRequestHost)
    json.put("v2rayAllowInsecure", v2rayAllowInsecure)
    json.put("v2rayFingerprint", v2rayFingerprint)
    json.put("dnsProvider", dnsProvider)
    json.put("customDns1", customDns1)
    json.put("customDns2", customDns2)
    json.put("enableUdp", enableUdp)
    json.put("udpgwPort", udpgwPort)
    json.put("tlsVersion", tlsVersion)
    json.put("sendBuffer", sendBuffer)
    json.put("receiveBuffer", receiveBuffer)
    json.put("enableDnsForwarding", enableDnsForwarding)
    json.put("autoReconnect", autoReconnect)
    json.put("autoPingEnabled", autoPingEnabled)
    json.put("autoPingHost", autoPingHost)
    json.put("autoPingIntervalSec", autoPingIntervalSec)
    json.put("routingMode", routingMode)
    json.put("bypassVpnApps", bypassVpnApps)
    json.put("isLocked", isLocked)
    json.put("lockSsh", lockSsh)
    json.put("lockPayload", lockPayload)
    json.put("noteMessage", noteMessage)
    json.put("expiryDate", expiryDate)
    json.put("author", author)
    return json
  }

  companion object {
    fun fromJson(json: JSONObject): TunnelConfig {
      val typeStr = json.optString("type", TunnelType.SSH_SSL_TLS.name)
      val parsedType = try {
        TunnelType.valueOf(typeStr)
      } catch (e: Exception) {
        TunnelType.SSH_SSL_TLS
      }

      return TunnelConfig(
        id = json.optString("id", UUID.randomUUID().toString()),
        name = json.optString("name", "Imported Config"),
        type = parsedType,
        sshHost = json.optString("sshHost", "sg1.hnhost.net"),
        sshPort = json.optInt("sshPort", 443),
        sshUsername = json.optString("sshUsername", "user"),
        sshPassword = json.optString("sshPassword", "pass"),
        payload = json.optString("payload", "CONNECT [host_port] HTTP/1.1[crlf]Host: [host][crlf][crlf]"),
        sniHost = json.optString("sniHost", "cdn.cloudflare.net"),
        remoteProxy = json.optString("remoteProxy", ""),
        v2rayAddress = json.optString("v2rayAddress", "sg-v2ray.hnhost.net"),
        v2rayPort = json.optInt("v2rayPort", 443),
        v2rayUuid = json.optString("v2rayUuid", "b831381d-6324-4d53-ad4f-8cda48b30811"),
        v2rayAlterId = json.optInt("v2rayAlterId", 0),
        v2raySecurity = json.optString("v2raySecurity", "auto"),
        v2rayNetwork = json.optString("v2rayNetwork", "ws"),
        v2rayWsPath = json.optString("v2rayWsPath", "/v2ray-ws"),
        v2raySni = json.optString("v2raySni", "cdn.cloudflare.net"),
        v2rayTls = json.optBoolean("v2rayTls", true),
        v2rayFlow = json.optString("v2rayFlow", "none"),
        v2rayEncryption = json.optString("v2rayEncryption", "none"),
        v2rayHeaderType = json.optString("v2rayHeaderType", "---"),
        v2rayRequestHost = json.optString("v2rayRequestHost", ""),
        v2rayAllowInsecure = json.optBoolean("v2rayAllowInsecure", true),
        v2rayFingerprint = json.optString("v2rayFingerprint", "chrome"),
        dnsProvider = json.optString("dnsProvider", "Google DNS"),
        customDns1 = json.optString("customDns1", "8.8.8.8"),
        customDns2 = json.optString("customDns2", "8.8.4.4"),
        enableUdp = json.optBoolean("enableUdp", true),
        udpgwPort = json.optInt("udpgwPort", 7300),
        tlsVersion = json.optString("tlsVersion", "Auto"),
        sendBuffer = json.optInt("sendBuffer", 16384),
        receiveBuffer = json.optInt("receiveBuffer", 32768),
        enableDnsForwarding = json.optBoolean("enableDnsForwarding", true),
        autoReconnect = json.optBoolean("autoReconnect", true),
        autoPingEnabled = json.optBoolean("autoPingEnabled", true),
        autoPingHost = json.optString("autoPingHost", "8.8.8.8"),
        autoPingIntervalSec = json.optInt("autoPingIntervalSec", 3),
        routingMode = json.optString("routingMode", "DIRECT_TUNNEL"),
        bypassVpnApps = json.optString("bypassVpnApps", ""),
        isLocked = json.optBoolean("isLocked", false),
        lockSsh = json.optBoolean("lockSsh", false),
        lockPayload = json.optBoolean("lockPayload", false),
        noteMessage = json.optString("noteMessage", ""),
        expiryDate = json.optString("expiryDate", ""),
        author = json.optString("author", "")
      )
    }
  }
}
