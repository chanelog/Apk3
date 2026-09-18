package com.example.util

object PayloadGenerator {

  enum class InjectionMethod(val label: String) {
    NORMAL("Normal"),
    FRONT_INJECT("Front Inject"),
    BACK_INJECT("Back Inject")
  }

  enum class QueryMethod(val label: String) {
    NONE("None"),
    FRONT_QUERY("Front Query"),
    BACK_QUERY("Back Query"),
    REVERSE_PROXY("Reverse Proxy")
  }

  enum class SplitMethod(val label: String) {
    NONE("None"),
    INSTANT_SPLIT("Instant Split ([split])"),
    DELAY_SPLIT("Delay Split ([delay_split])")
  }

  data class GeneratorParams(
    val bugHost: String = "m.facebook.com",
    val requestMethod: String = "CONNECT",
    val injectionMethod: InjectionMethod = InjectionMethod.NORMAL,
    val queryMethod: QueryMethod = QueryMethod.NONE,
    val splitMethod: SplitMethod = SplitMethod.NONE,
    val keepAlive: Boolean = true,
    val onlineHost: Boolean = true,
    val forwardHost: Boolean = false,
    val reverseProxyHeader: Boolean = false,
    val userAgent: Boolean = true,
    val referer: Boolean = false,
    val dualConnect: Boolean = false
  )

  fun generate(params: GeneratorParams): String {
    val host = if (params.bugHost.isBlank()) "[host]" else params.bugHost.trim()
    val method = params.requestMethod.trim()

    val targetUri = when (params.queryMethod) {
      QueryMethod.FRONT_QUERY -> "$host@[host_port]"
      QueryMethod.BACK_QUERY -> "[host_port]@$host"
      else -> "[host_port]"
    }

    val headers = StringBuilder()
    headers.append("Host: ").append(host).append("[crlf]")

    if (params.onlineHost) {
      headers.append("X-Online-Host: ").append(host).append("[crlf]")
    }
    if (params.forwardHost) {
      headers.append("X-Forward-Host: ").append(host).append("[crlf]")
    }
    if (params.reverseProxyHeader || params.queryMethod == QueryMethod.REVERSE_PROXY) {
      headers.append("X-Reverse-Proxy: ").append(host).append("[crlf]")
    }
    if (params.userAgent) {
      headers.append("User-Agent: [ua][crlf]")
    }
    if (params.referer) {
      headers.append("Referer: https://").append(host).append("/[crlf]")
    }
    if (params.keepAlive) {
      headers.append("Connection: Keep-Alive[crlf]")
      headers.append("Proxy-Connection: Keep-Alive[crlf]")
    }

    val splitTag = when (params.splitMethod) {
      SplitMethod.INSTANT_SPLIT -> "[split]"
      SplitMethod.DELAY_SPLIT -> "[delay_split]"
      SplitMethod.NONE -> ""
    }

    val sb = StringBuilder()
    when (params.injectionMethod) {
      InjectionMethod.NORMAL -> {
        if (params.dualConnect) sb.append("[raw]")
        sb.append(method).append(" ").append(targetUri).append(" [protocol][crlf]")
        sb.append(headers.toString())
        if (splitTag.isNotEmpty()) sb.append(splitTag)
        sb.append("[crlf]")
      }

      InjectionMethod.FRONT_INJECT -> {
        sb.append("GET http://").append(host).append("/ HTTP/1.1[crlf]")
        sb.append("Host: ").append(host).append("[crlf]")
        sb.append("Connection: Keep-Alive[crlf][crlf]")
        sb.append(if (splitTag.isNotEmpty()) splitTag else "[split]")
        sb.append(method).append(" ").append(targetUri).append(" [protocol][crlf]")
        sb.append(headers.toString())
        sb.append("[crlf]")
      }

      InjectionMethod.BACK_INJECT -> {
        sb.append(method).append(" ").append(targetUri).append(" [protocol][crlf]")
        sb.append(headers.toString())
        sb.append(if (splitTag.isNotEmpty()) splitTag else "[split]")
        sb.append("GET http://").append(host).append("/ HTTP/1.1[crlf]")
        sb.append("Host: ").append(host).append("[crlf]")
        sb.append("Connection: Keep-Alive[crlf][crlf]")
      }
    }

    return sb.toString()
  }
}
