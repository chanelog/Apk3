package com.example.tunnel

import com.example.model.TunnelConfig
import com.example.model.TunnelType
import org.json.JSONArray
import org.json.JSONObject

/** Builds an Xray-core JSON configuration from the app profile. */
object XrayConfigBuilder {

  fun build(config: TunnelConfig, socksInboundPort: Int): String {
    val root = JSONObject()
    root.put("log", JSONObject().apply { put("loglevel", "warning") })

    // Keep DNS inside Xray. The VPN TUN itself advertises these DNS servers,
    // while Xray resolves proxy destinations through HTTPS DNS resolvers.
    root.put("dns", JSONObject().apply {
      put("servers", JSONArray()
        .put("https://1.1.1.1/dns-query")
        .put("https://8.8.8.8/dns-query"))
    })

    // This inbound is useful for diagnostics and for library variants that
    // expose a local SOCKS listener. The startLoop(tunFd) path still handles
    // VPN traffic directly through the supplied TUN descriptor.
    root.put("inbounds", JSONArray().put(
      JSONObject().apply {
        put("tag", "socks-in")
        put("port", socksInboundPort)
        put("listen", "127.0.0.1")
        put("protocol", "socks")
        put("settings", JSONObject().apply {
          put("auth", "noauth")
          put("udp", false)
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

    // The proxy outbound is the default for both TCP and UDP. UDP support still
    // depends on the selected server/transport supporting XUDP.
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

  private fun streamSettings(config: TunnelConfig): JSONObject = JSONObject().apply {
    put("network", config.v2rayNetwork.ifBlank { "tcp" }.lowercase())
    if (config.v2rayTls) {
      put("security", "tls")
      put("tlsSettings", JSONObject().apply {
        put("serverName", config.v2raySni.ifBlank { config.v2rayAddress })
        put("allowInsecure", config.v2rayAllowInsecure)
      })
    }
    if (config.v2rayNetwork.equals("ws", ignoreCase = true)) {
      put("wsSettings", JSONObject().apply {
        put("path", config.v2rayWsPath.ifBlank { "/" })
        put("headers", JSONObject().apply {
          // Host is the HTTP WebSocket Host, not necessarily the TLS SNI.
          val host = config.v2rayRequestHost.ifBlank { config.v2rayAddress }
          if (host.isNotBlank()) put("Host", host)
        })
      })
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
          put("alterId", config.v2rayAlterId)
          put("security", config.v2raySecurity.ifBlank { "auto" })
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
          put("encryption", config.v2rayEncryption.ifBlank { "none" })
          if (config.v2rayFlow.isNotBlank() && config.v2rayFlow != "none") put("flow", config.v2rayFlow)
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
        put("password", config.v2rayUuid)
      }))
    })
    put("streamSettings", streamSettings(config).apply { put("security", "tls") })
  }
}
