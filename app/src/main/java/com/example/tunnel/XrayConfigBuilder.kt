package com.example.tunnel

import com.example.model.TunnelConfig
import com.example.model.TunnelType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Membentuk config JSON Xray-core dari TunnelConfig. Skema JSON ini adalah
 * skema resmi Xray-core (stabil, terdokumentasi di xtls/xray-core), BUKAN
 * bagian yang rawan berubah — beda dengan lapisan JNI/gomobile-nya.
 *
 * Catatan penting: field "allowInsecure" SENGAJA tidak pernah ditulis di sini
 * (lihat histori project: versi Xray-core baru menolak/menghapus field ini).
 */
object XrayConfigBuilder {

  fun build(config: TunnelConfig, socksInboundPort: Int): String {
    val root = JSONObject()
    root.put("log", JSONObject().apply { put("loglevel", "warning") })

    // WAJIB: tanpa ini, Xray sering "connected" tapi nggak bisa resolve domain
    // sama sekali (gejala persis "connect tapi internet nggak jalan").
    val dnsServer1 = config.customDns1.ifBlank { "8.8.8.8" }
    val dnsServer2 = config.customDns2.ifBlank { "1.1.1.1" }
    root.put("dns", JSONObject().apply {
      put("servers", JSONArray().put(dnsServer1).put(dnsServer2))
    })

    root.put("inbounds", JSONArray().put(
      JSONObject().apply {
        put("tag", "socks-in")
        put("port", socksInboundPort)
        put("listen", "127.0.0.1")
        put("protocol", "socks")
        put("settings", JSONObject().apply {
          put("auth", "noauth")
          put("udp", true)
        })
      }
    ))

    val outbound = when (config.type) {
      TunnelType.V2RAY_VMESS -> buildVmessOutbound(config)
      TunnelType.V2RAY_VLESS -> buildVlessOutbound(config)
      TunnelType.TROJAN -> buildTrojanOutbound(config)
      else -> throw IllegalArgumentException("XrayConfigBuilder dipanggil untuk tipe non-Xray: ${config.type}")
    }
    root.put("outbounds", JSONArray().put(outbound).put(
      JSONObject().apply { put("protocol", "freedom"); put("tag", "direct") }
    ))

    // Rute eksplisit: semua trafik lewat "proxy" secara default. Tanpa blok
    // ini Xray SEHARUSNYA tetap pakai outbound pertama secara default, tapi
    // ditulis eksplisit supaya tidak ambigu.
    root.put("routing", JSONObject().apply {
      put("domainStrategy", "IPIfNonMatch")
      put("rules", JSONArray().put(
        JSONObject().apply {
          put("type", "field")
          put("network", "tcp,udp")
          put("outboundTag", "proxy")
        }
      ))
    })

    return root.toString()
  }

  private fun streamSettings(config: TunnelConfig): JSONObject {
    return JSONObject().apply {
      put("network", config.v2rayNetwork.ifBlank { "tcp" })
      if (config.v2rayTls) {
        put("security", "tls")
        put("tlsSettings", JSONObject().apply {
          put("serverName", config.v2raySni.ifBlank { config.v2rayAddress })
          // allowInsecure sengaja TIDAK ditulis (lihat catatan di atas)
        })
      }
      if (config.v2rayNetwork.equals("ws", ignoreCase = true)) {
        put("wsSettings", JSONObject().apply {
          put("path", config.v2rayWsPath.ifBlank { "/" })
          put("headers", JSONObject().apply {
            if (config.v2raySni.isNotBlank()) put("Host", config.v2raySni)
          })
        })
      }
    }
  }

  private fun buildVmessOutbound(config: TunnelConfig): JSONObject = JSONObject().apply {
    put("tag", "proxy")
    put("protocol", "vmess")
    put("settings", JSONObject().apply {
      put("vnext", JSONArray().put(JSONObject().apply {
        put("address", config.v2rayAddress)
        put("port", config.v2rayPort)
        put("users", JSONArray().put(JSONObject().apply {
          put("id", config.v2rayUuid)
          put("alterId", 0)
          put("security", "auto")
        }))
      }))
    })
    put("streamSettings", streamSettings(config))
  }

  private fun buildVlessOutbound(config: TunnelConfig): JSONObject = JSONObject().apply {
    put("tag", "proxy")
    put("protocol", "vless")
    put("settings", JSONObject().apply {
      put("vnext", JSONArray().put(JSONObject().apply {
        put("address", config.v2rayAddress)
        put("port", config.v2rayPort)
        put("users", JSONArray().put(JSONObject().apply {
          put("id", config.v2rayUuid)
          put("encryption", "none")
        }))
      }))
    })
    put("streamSettings", streamSettings(config))
  }

  private fun buildTrojanOutbound(config: TunnelConfig): JSONObject = JSONObject().apply {
    put("tag", "proxy")
    put("protocol", "trojan")
    put("settings", JSONObject().apply {
      put("servers", JSONArray().put(JSONObject().apply {
        put("address", config.v2rayAddress)
        put("port", config.v2rayPort)
        put("password", config.v2rayUuid) // field password Trojan dipakai dari slot uuid di TunnelConfig
      }))
    })
    put("streamSettings", streamSettings(config).apply { put("security", "tls") })
  }
}
