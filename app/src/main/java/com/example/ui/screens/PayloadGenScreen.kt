package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldNeon
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.PayloadGenerator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PayloadGenScreen(
  currentPayload: String,
  onApplyPayload: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  var bugHost by remember { mutableStateOf("m.facebook.com") }
  var selectedMethod by remember { mutableStateOf("CONNECT") }
  var injectionMethod by remember { mutableStateOf(PayloadGenerator.InjectionMethod.NORMAL) }
  var queryMethod by remember { mutableStateOf(PayloadGenerator.QueryMethod.NONE) }
  var splitMethod by remember { mutableStateOf(PayloadGenerator.SplitMethod.NONE) }

  // Extra Header toggles
  var keepAlive by remember { mutableStateOf(true) }
  var onlineHost by remember { mutableStateOf(true) }
  var forwardHost by remember { mutableStateOf(false) }
  var reverseProxy by remember { mutableStateOf(false) }
  var userAgent by remember { mutableStateOf(true) }
  var referer by remember { mutableStateOf(false) }
  var dualConnect by remember { mutableStateOf(false) }

  var generatedPayload by remember { mutableStateOf(currentPayload) }

  fun updatePayload() {
    val params = PayloadGenerator.GeneratorParams(
      bugHost = bugHost,
      requestMethod = selectedMethod,
      injectionMethod = injectionMethod,
      queryMethod = queryMethod,
      splitMethod = splitMethod,
      keepAlive = keepAlive,
      onlineHost = onlineHost,
      forwardHost = forwardHost,
      reverseProxyHeader = reverseProxy,
      userAgent = userAgent,
      referer = referer,
      dualConnect = dualConnect
    )
    generatedPayload = PayloadGenerator.generate(params)
  }

  LaunchedEffect(
    bugHost, selectedMethod, injectionMethod, queryMethod, splitMethod,
    keepAlive, onlineHost, forwardHost, reverseProxy, userAgent, referer, dualConnect
  ) {
    updatePayload()
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianDark)
      .verticalScroll(scrollState)
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .padding(bottom = 36.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(22.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Payload Generator",
        color = TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
      )
    }

    Text(
      text = "Craft custom HTTP injection headers like HTTP Custom",
      color = TextSecondary,
      fontSize = 12.sp
    )

    Spacer(modifier = Modifier.height(14.dp))

    // 1. URL / Host Bug Input
    OutlinedTextField(
      value = bugHost,
      onValueChange = { bugHost = it },
      label = { Text("URL / Host Bug") },
      placeholder = { Text("e.g. m.facebook.com or api.zoom.us") },
      modifier = Modifier.fillMaxWidth(),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = EmeraldNeon,
        unfocusedBorderColor = ObsidianBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedLabelColor = EmeraldNeon
      )
    )

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Request Method
    Text(
      text = "REQUEST METHOD",
      color = TextSecondary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp
    )

    Spacer(modifier = Modifier.height(6.dp))

    val methods = listOf("CONNECT", "GET", "POST", "HEAD", "PUT", "OPTIONS", "TRACE", "PATCH")
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      methods.forEach { method ->
        val isSelected = selectedMethod == method
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) EmeraldNeon else ObsidianCard)
            .border(1.dp, if (isSelected) EmeraldNeon else ObsidianBorder, RoundedCornerShape(8.dp))
            .clickable { selectedMethod = method }
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Text(
            text = method,
            color = if (isSelected) ObsidianDark else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Injection Method
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = ObsidianCard),
      border = androidx.compose.foundation.BorderStroke(0.8.dp, ObsidianBorder)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = "INJECTION METHOD",
          color = CyanNeon,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        PayloadGenerator.InjectionMethod.values().forEach { method ->
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { injectionMethod = method }
              .padding(vertical = 3.dp)
          ) {
            RadioButton(
              selected = injectionMethod == method,
              onClick = { injectionMethod = method },
              colors = RadioButtonDefaults.colors(selectedColor = CyanNeon, unselectedColor = TextSecondary)
            )
            Text(
              text = method.label,
              color = if (injectionMethod == method) TextPrimary else TextSecondary,
              fontSize = 13.sp,
              fontWeight = if (injectionMethod == method) FontWeight.Bold else FontWeight.Normal
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 4. Query & Split Methods
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = ObsidianCard),
      border = androidx.compose.foundation.BorderStroke(0.8.dp, ObsidianBorder)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = "QUERY / PROXY METHOD",
          color = EmeraldNeon,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          PayloadGenerator.QueryMethod.values().forEach { qm ->
            val sel = queryMethod == qm
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (sel) EmeraldNeon.copy(alpha = 0.2f) else ObsidianSurface)
                .border(0.8.dp, if (sel) EmeraldNeon else ObsidianBorder, RoundedCornerShape(8.dp))
                .clickable { queryMethod = qm }
                .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text(
                text = qm.label,
                color = if (sel) EmeraldNeon else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "SPLIT DELAY",
          color = EmeraldNeon,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          PayloadGenerator.SplitMethod.values().forEach { sm ->
            val sel = splitMethod == sm
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (sel) EmeraldNeon.copy(alpha = 0.2f) else ObsidianSurface)
                .border(0.8.dp, if (sel) EmeraldNeon else ObsidianBorder, RoundedCornerShape(8.dp))
                .clickable { splitMethod = sm }
                .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text(
                text = sm.label,
                color = if (sel) EmeraldNeon else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 5. Extra Headers Checklist
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = ObsidianCard),
      border = androidx.compose.foundation.BorderStroke(0.8.dp, ObsidianBorder)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = "EXTRA HEADERS",
          color = TextSecondary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          HeaderCheckItem("Online Host", onlineHost) { onlineHost = it }
          HeaderCheckItem("Keep-Alive", keepAlive) { keepAlive = it }
          HeaderCheckItem("User-Agent", userAgent) { userAgent = it }
          HeaderCheckItem("Forward Host", forwardHost) { forwardHost = it }
          HeaderCheckItem("Reverse Proxy", reverseProxy) { reverseProxy = it }
          HeaderCheckItem("Referer", referer) { referer = it }
          HeaderCheckItem("Dual Connect", dualConnect) { dualConnect = it }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 6. Live Payload Preview
    Text(
      text = "GENERATED PAYLOAD PREVIEW",
      color = TextSecondary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp
    )

    Spacer(modifier = Modifier.height(6.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF070B12))
        .border(1.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        .padding(12.dp)
    ) {
      Text(
        text = generatedPayload,
        color = CyanNeon,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 17.sp
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Action Buttons: Copy & Apply
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = {
          val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          cm.setPrimaryClip(ClipData.newPlainText("HN Tunnel Payload", generatedPayload))
          Toast.makeText(context, "Payload copied to clipboard!", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ObsidianCard, contentColor = TextPrimary),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
      ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Copy", fontWeight = FontWeight.Bold)
      }

      Button(
        onClick = {
          onApplyPayload(generatedPayload)
          Toast.makeText(context, "Payload applied to active tunnel!", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.weight(1.4f),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = ObsidianDark)
      ) {
        Icon(Icons.Default.ElectricBolt, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Apply to Tunnel", fontWeight = FontWeight.Black)
      }
    }
  }
}

@Composable
private fun HeaderCheckItem(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable { onCheckedChange(!checked) }
      .padding(end = 6.dp)
  ) {
    Checkbox(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = CheckboxDefaults.colors(
        checkedColor = EmeraldNeon,
        checkmarkColor = ObsidianDark,
        uncheckedColor = TextSecondary
      ),
      modifier = Modifier.size(32.dp)
    )
    Text(
      text = label,
      color = if (checked) TextPrimary else TextSecondary,
      fontSize = 12.sp,
      fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal
    )
  }
}
