package com.example.model

enum class TunnelType(val displayName: String, val shortDesc: String) {
  SSH_DIRECT("SSH Direct", "Direct TCP connection to SSH host"),
  SSH_SSL_TLS("SSH + SSL/TLS (SNI)", "Stunnel with custom SNI bug host"),
  SSH_HTTP_PROXY("SSH + HTTP Proxy", "Inject custom payload through remote proxy"),
  V2RAY_VMESS("V2Ray VMess", "VMess protocol with WebSocket/TLS"),
  V2RAY_VLESS("V2Ray VLess", "Lightweight VLess protocol"),
  TROJAN("Trojan TLS", "Trojan protocol bypassing firewall via TLS")
}
