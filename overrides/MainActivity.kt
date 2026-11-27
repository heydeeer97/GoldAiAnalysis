package com.goldai.analyst

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Bg = Color(0xFF0B0D10)
private val Panel = Color(0xFF15191F)
private val Panel2 = Color(0xFF1C222B)
private val Muted = Color(0xFF9AA4B2)
private val Accent = Color(0xFFE5B94B)
private val White = Color(0xFFF4F6F8)
private val Green = Color(0xFF35C982)
private val Red = Color(0xFFFF6262)

private const val ENGINE_URL = "http://127.0.0.1:5000/analysis"

class MainActivity : ComponentActivity() {
override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
setContent { GoldApp() }
}
}

suspend fun fetchAnalysis(): JSONObject = withContext(Dispatchers.IO) {
val connection = URL(ENGINE_URL).openConnection() as HttpURLConnection
try {
connection.requestMethod = "GET"
connection.connectTimeout = 10000
connection.readTimeout = 15000
connection.connect()

if (connection.responseCode != 200) {  
        throw Exception("HTTP ${connection.responseCode}")  
    }  

    JSONObject(connection.inputStream.bufferedReader().use { it.readText() })  
} finally {  
    connection.disconnect()  
}

}

@Composable
fun GoldApp() {
var selectedTab by remember { mutableStateOf("Обзор") }
var timeframe by remember { mutableStateOf("15") }
var data by remember { mutableStateOf<JSONObject?>(null) }
var error by remember { mutableStateOf("") }
var loading by remember { mutableStateOf(true) }

LaunchedEffect(Unit) {  
    while (true) {  
        try {  
            data = fetchAnalysis()  
            error = ""  
        } catch (e: Exception) {  
            error = e.message ?: "Нет соединения с сервером"  
        }  
        loading = false  
        delay(30000)  
    }  
}  

MaterialTheme(  
    colorScheme = darkColorScheme(  
        background = Bg,  
        surface = Panel,  
        primary = Accent,  
        onBackground = White,  
        onSurface = White  
    )  
) {  
    Scaffold(  
        containerColor = Bg,  
        bottomBar = {  
            NavigationBar(containerColor = Panel) {  
                NavigationBarItem(  
                    selectedTab == "Обзор",  
                    { selectedTab = "Обзор" },  
                    icon = { Icon(Icons.Default.Analytics, null) },  
                    label = { Text("Обзор") }  
                )  
                NavigationBarItem(  
                    selectedTab == "График",  
                    { selectedTab = "График" },  
                    icon = { Icon(Icons.Default.ShowChart, null) },  
                    label = { Text("График") }  
                )  
                NavigationBarItem(  
                    selectedTab == "История",  
                    { selectedTab = "История" },  
                    icon = { Icon(Icons.Default.History, null) },  
                    label = { Text("История") }  
                )  
            }  
        }  
    ) { padding ->  
        Column(  
            Modifier  
                .fillMaxSize()  
                .background(Bg)  
                .padding(padding)  
                .padding(horizontal = 16.dp)  
        ) {  
            Header()  

            when (selectedTab) {  
                "Обзор" -> Overview(  
                    timeframe,  
                    { timeframe = it },  
                    { selectedTab = "График" },  
                    data,  
                    error,  
                    loading  
                )  
                "График" -> ChartScreen(timeframe, { timeframe = it })  
                else -> HistoryScreen()  
            }  
        }  
    }  
}

}

@Composable
fun Header() {
Row(
Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 18.dp),
verticalAlignment = Alignment.CenterVertically
) {
Column(Modifier.weight(1f)) {
Text(
"GOLD AI ANALYST",
color = White,
fontSize = 18.sp,
fontWeight = FontWeight.Bold
)
Text(
"АНАЛИТИКА ЗОЛОТА",
color = Muted,
fontSize = 11.sp,
letterSpacing = 1.4.sp
)
}
Surface(color = Panel2, shape = RoundedCornerShape(50)) {
Text(
"LIVE ENGINE",
color = Green,
modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
fontSize = 10.sp,
fontWeight = FontWeight.Bold
)
}
}
}

@Composable
fun Overview(
timeframe: String,
onTimeframeChange: (String) -> Unit,
onOpenChart: () -> Unit,
data: JSONObject?,
error: String,
loading: Boolean
) {
val indicators = data?.optJSONObject("indicators")
val signal = data?.optString("signal", "NO TRADE") ?: "NO TRADE"

val signalColor = when (signal) {  
    "BUY" -> Green  
    "SELL" -> Red  
    else -> Accent  
}  

Column(  
    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),  
    verticalArrangement = Arrangement.spacedBy(14.dp)  
) {  
    Row(verticalAlignment = Alignment.Bottom) {  
        Column(Modifier.weight(1f)) {  
            Text("Binance · PAXG/USDT", color = Muted, fontSize = 12.sp)  
            Text(  
                "GOLD",  
                color = White,  
                fontSize = 28.sp,  
                fontWeight = FontWeight.Bold  
            )  
        }  
        Text(  
            if (error.isEmpty() && data != null) "● ENGINE ONLINE" else "● OFFLINE",  
            color = if (error.isEmpty() && data != null) Green else Red,  
            fontSize = 10.sp  
        )  
    }  

    CardBlock {  
        Text("ПОСЛЕДНЯЯ ЦЕНА", color = Muted, fontSize = 10.sp)  
        Spacer(Modifier.height(8.dp))  
        Text(  
            data?.optDouble("price")?.takeIf { it > 0 }  
                ?.let { String.format("%.2f", it) } ?: "—",  
            color = White,  
            fontSize = 34.sp,  
            fontWeight = FontWeight.Bold  
        )  
        Text(  
            when {  
                error.isNotEmpty() -> "Ошибка: $error"  
                loading -> "Подключение к аналитическому серверу..."  
                data != null -> "Данные получены с аналитического сервера"  
                else -> "Ожидание данных"  
            },  
            color = if (error.isEmpty()) Muted else Red,  
            fontSize = 11.sp  
        )  
    }  

    Row(verticalAlignment = Alignment.CenterVertically) {  
        TimeButton("15", timeframe == "15") { onTimeframeChange("15") }  
        Spacer(Modifier.width(8.dp))  
        TimeButton("60", timeframe == "60") { onTimeframeChange("60") }  
        Spacer(Modifier.weight(1f))  
        TextButton(onClick = onOpenChart) {  
            Text("Открыть график", color = Accent)  
        }  
    }  

    CardBlock {  
        Text("ТОРГОВЫЙ СИГНАЛ", color = Muted, fontSize = 10.sp)  
        Spacer(Modifier.height(10.dp))  
        Text(  
            signal,  
            color = signalColor,  
            fontSize = 27.sp,  
            fontWeight = FontWeight.Bold  
        )  
        Text(  
            "BUY: ${data?.optJSONObject("scores")?.optInt("buy") ?: 0}   " +  
                "SELL: ${data?.optJSONObject("scores")?.optInt("sell") ?: 0}",  
            color = White,  
            fontSize = 13.sp  
        )  
        Spacer(Modifier.height(8.dp))  
        Text(  
            data?.optJSONArray("reasons")?.let { array ->  
                (0 until array.length()).joinToString("\n") {  
                    "• ${array.optString(it)}"  
                }  
            } ?: "Ожидание аналитических данных",  
            color = Muted,  
            fontSize = 12.sp  
        )  
    }  

    CardBlock {  
        Text("АНАЛИТИЧЕСКИЕ ФАКТОРЫ", color = Muted, fontSize = 10.sp)  
        Spacer(Modifier.height(12.dp))  

        FactorRow("Тренд H1", data?.optString("trend_h1") ?: "Нет данных")  
        FactorRow("Тренд M15", data?.optString("trend_m15") ?: "Нет данных")  
        FactorRow("Структура M15", data?.optString("structure_m15") ?: "Нет данных")  
        FactorRow("Ликвидность", data?.optString("liquidity") ?: "Нет данных")  
        FactorRow("RSI 14", indicators?.optDouble("rsi14")?.let { "%.2f".format(it) } ?: "—")  
        FactorRow("MACD", indicators?.optJSONObject("macd")?.optDouble("histogram")?.let { "%.4f".format(it) } ?: "—")  
        FactorRow("ATR 14", indicators?.optDouble("atr14")?.let { "%.4f".format(it) } ?: "—")  
        FactorRow("Volume ratio", indicators?.optDouble("volume_ratio")?.let { "%.2f".format(it) } ?: "—")  
    }  

    Text(  
        "Источник: Binance PAXG/USDT. Экспериментальная аналитика, не гарантированный торговый сигнал.",  
        color = Muted,  
        fontSize = 10.sp,  
        modifier = Modifier.padding(bottom = 12.dp)  
    )  
}

}

@Composable
fun ChartScreen(timeframe: String, onTimeframeChange: (String) -> Unit) {
Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
Text("ГРАФИК XAU/USD", color = Muted, fontSize = 11.sp)
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
TimeButton("15", timeframe == "15") { onTimeframeChange("15") }
TimeButton("60", timeframe == "60") { onTimeframeChange("60") }
}
TradingViewChart(timeframe, Modifier.fillMaxWidth().weight(1f))
Text(
"График TradingView. Котировки графика могут отличаться от Binance PAXG/USDT.",
color = Muted,
fontSize = 10.sp
)
}
}

@Composable
fun TradingViewChart(timeframe: String, modifier: Modifier = Modifier) {
val html = remember(timeframe) {
"""
<!DOCTYPE html><html><head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
html,body{margin:0;padding:0;background:#0B0D10;height:100%}
.tradingview-widget-container{height:100%;width:100%}
.tradingview-widget-container__widget{height:100%;width:100%}
</style></head><body>
<div class="tradingview-widget-container">
<div class="tradingview-widget-container__widget"></div>
<script type="text/javascript" src="https://s3.tradingview.com/external-embedding/embed-widget-advanced-chart.js" async>
{
"autosize": true,
"symbol": "OANDA:XAUUSD",
"interval": "$timeframe",
"timezone": "Europe/Warsaw",
"theme": "dark",
"style": "1",
"locale": "ru",
"allow_symbol_change": true,
"calendar": false,
"support_host": "https://www.tradingview.com"
}
</script>
</div></body></html>
""".trimIndent()
}

AndroidView(  
    modifier = modifier,  
    factory = { context ->  
        WebView(context).apply {  
            settings.javaScriptEnabled = true  
            settings.domStorageEnabled = true  
            webChromeClient = WebChromeClient()  
            webViewClient = WebViewClient()  
            setBackgroundColor(android.graphics.Color.rgb(11, 13, 16))  
            loadDataWithBaseURL(  
                "https://www.tradingview.com",  
                html, "text/html", "UTF-8", null  
            )  
        }  
    },  
    update = {  
        it.loadDataWithBaseURL(  
            "https://www.tradingview.com",  
            html, "text/html", "UTF-8", null  
        )  
    }  
)

}

@Composable
fun HistoryScreen() {
Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
Text("ИСТОРИЯ СИГНАЛОВ", color = Muted, fontSize = 11.sp)
CardBlock {
Text("Пока нет сохранённых сигналов", color = White)
Text(
"Историю сделаем следующим этапом.",
color = Muted,
fontSize = 12.sp
)
}
}
}

@Composable
fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
Card(
colors = CardDefaults.cardColors(containerColor = Panel),
shape = RoundedCornerShape(18.dp),
modifier = Modifier.fillMaxWidth()
) {
Column(Modifier.padding(16.dp), content = content)
}
}

@Composable
fun TimeButton(label: String, selected: Boolean, onClick: () -> Unit) {
val title = if (label == "15") "M15" else "H1"
Surface(
onClick = onClick,
color = if (selected) Accent else Panel2,
shape = RoundedCornerShape(10.dp)
) {
Text(
title,
color = if (selected) Bg else White,
modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
fontWeight = FontWeight.Bold,
fontSize = 12.sp
)
}
}

@Composable
fun FactorRow(name: String, value: String) {
Row(
Modifier.fillMaxWidth().padding(vertical = 7.dp),
verticalAlignment = Alignment.CenterVertically
) {
Text(name, color = White, fontSize = 12.sp, modifier = Modifier.weight(1f))
Text(value, color = Muted, fontSize = 11.sp)
}
}
