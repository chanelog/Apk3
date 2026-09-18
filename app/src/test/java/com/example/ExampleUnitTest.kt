package com.example

import com.example.model.TunnelConfig
import com.example.model.TunnelType
import com.example.util.ConfigManager
import com.example.util.PayloadGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

  @Test
  fun payloadGenerator_createsValidPayload() {
    val params = PayloadGenerator.GeneratorParams(
      bugHost = "m.facebook.com",
      requestMethod = "CONNECT",
      injectionMethod = PayloadGenerator.InjectionMethod.NORMAL,
      keepAlive = true,
      onlineHost = true
    )
    val payload = PayloadGenerator.generate(params)
    assertTrue(payload.contains("CONNECT [host_port] [protocol]"))
    assertTrue(payload.contains("Host: m.facebook.com"))
    assertTrue(payload.contains("X-Online-Host: m.facebook.com"))
    assertTrue(payload.contains("Connection: Keep-Alive"))
  }

  @Test
  fun configManager_exportAndImportHnt() {
    val original = TunnelConfig(
      name = "Test SSH Direct",
      type = TunnelType.SSH_DIRECT,
      sshHost = "ssh.test.org",
      sshPort = 22,
      sshUsername = "user123",
      sshPassword = "password123",
      noteMessage = "Unit test config"
    )

    val exported = ConfigManager.exportToHnt(original)
    assertTrue(exported.startsWith("HNTUNNEL#"))

    val imported = ConfigManager.importConfig(exported)
    assertEquals(original.name, imported.name)
    assertEquals(original.type, imported.type)
    assertEquals(original.sshHost, imported.sshHost)
    assertEquals(original.sshPort, imported.sshPort)
    assertEquals(original.sshUsername, imported.sshUsername)
  }
}
