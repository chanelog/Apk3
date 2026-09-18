package com.example.model

enum class TunnelState(val label: String) {
  DISCONNECTED("Disconnected"),
  CONNECTING("Connecting..."),
  AUTHENTICATING("Authenticating..."),
  HANDSHAKING("SSL/TLS Handshake..."),
  CONNECTED("Connected"),
  STOPPING("Stopping...")
}
