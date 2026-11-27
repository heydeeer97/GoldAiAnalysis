package com.goldai.analyst

import android.annotation.SuppressLint
import android.graphics.Color as AColor
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

private const val ENGINE = "http://127.0.0.1:5000"
private val BG = Color(0xFF080D16)
private val PANEL = Color(0xFF111A28)
private val MUTED = Color(0xFF91A0B5)
private val GREEN = Color(0xFF22C58B)
private val RED = Color(0xFFFF647C)
private val BLUE = Color(0xFF55A6FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = AColor.rgb(8, 13, 22)
        window.navigationBarColor = AColor.rgb(8, 13, 22)
        setContent { GoldApp() }
    }
}

@Composable
private fun GoldApp() {
    var tab by remember { mutableStateOf("Обзор") }
    var timeframe by remember { mutableStateOf("15m") }
    var data by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            try {
                data = getJson("$ENGINE/analysis")
                error = ""
            } catch (_: Exception) {
                error = "Нет связи с Python Engine. Запусти main.py в Termux."
            }
            delay(30_000)
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = BLUE, background = BG, surface = PANEL)) {
        Column(Modifier.fillMaxSize().background(BG).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(18.dp))
            Text("GOLD AI ANALYST", color = BLUE, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("PAXG / USDT", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("Binance • Tokenized Gold • Signals only", color = MUTED, fontSize = 11.sp)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Обзор", "График", "История").forEach { name ->
                    Box(
                        Modifier.weight(1f)
                            .background(if (tab == name) BLUE.copy(alpha = .18f) else PANEL, RoundedCornerShape(12.dp))
                            .clickable { tab = name }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(name, color = if (tab == name) BLUE else MUTED, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            when (tab) {
                "Обзор" -> Overview(data, error)
                "График" -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("15m", "1h").forEach { tf ->
                            FilterChip(selected = timeframe == tf, onClick = { timeframe = tf }, label = { Text(tf.uppercase()) })
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    CandleChart(timeframe)
                }
                else -> History(data)
            }
        }
    }
}

@Composable
private fun Overview(data: JSONObject?, error: String) {
    val signal = data?.optString("signal", "NO TRADE") ?: "NO TRADE"
    val price = data?.optDouble("price", 0.0) ?: 0.0
    val buy = data?.optInt("buy_score", 0) ?: 0
    val sell = data?.optInt("sell_score", 0) ?: 0
    val color = when (signal.uppercase()) { "BUY" -> GREEN; "SELL" -> RED; else -> BLUE }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 12.dp)) {
        if (error.isNotEmpty()) PanelCard { Text(error, color = RED) }
        PanelCard {
            Text("ТЕКУЩАЯ ЦЕНА", color = MUTED, fontSize = 11.sp)
            Text(if (price > 0) "$" + "%.2f".format(price) else "—", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Источник: Binance PAXGUSDT", color = MUTED, fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))
        PanelCard {
            Text("АНАЛИТИЧЕСКИЙ СИГНАЛ", color = MUTED, fontSize = 11.sp)
            Text(signal, color = color, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("BUY  $buy", color = GREEN, fontWeight = FontWeight.Bold)
                Text("SELL  $sell", color = RED, fontWeight = FontWeight.Bold)
            }
            Text("Сигнал не является торговой рекомендацией.", color = MUTED, fontSize = 10.sp)
        }
        Spacer(Modifier.height(10.dp))
        PanelCard {
            Text("ТРЕНД И СТРУКТУРА", color = MUTED, fontSize = 11.sp)
            InfoRow("H1", data?.optString("h1_trend", "—") ?: "—")
            InfoRow("M15", data?.optString("m15_trend", "—") ?: "—")
            InfoRow("Структура", data?.optString("structure", "—") ?: "—")
            InfoRow("Ликвидность", data?.optString("liquidity", "—") ?: "—")
        }
        Spacer(Modifier.height(10.dp))
        PanelCard {
            Text("ИНДИКАТОРЫ", color = MUTED, fontSize = 11.sp)
            InfoRow("RSI 14", "%.2f".format(data?.optDouble("rsi", 0.0) ?: 0.0))
            InfoRow("MACD histogram", "%.4f".format(data?.optDouble("macd_histogram", 0.0) ?: 0.0))
            InfoRow("ATR 14", "%.4f".format(data?.optDouble("atr", 0.0) ?: 0.0))
            InfoRow("Volume ratio", "%.2f".format(data?.optDouble("volume_ratio", 0.0) ?: 0.0))
        }
        val reasons = data?.optJSONArray("reasons")
        if (reasons != null && reasons.length() > 0) {
            Spacer(Modifier.height(10.dp))
            PanelCard {
                Text("ОБОСНОВАНИЕ", color = MUTED, fontSize = 11.sp)
                for (i in 0 until reasons.length()) Text("• ${reasons.optString(i)}", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun History(data: JSONObject?) {
    Column(Modifier.fillMaxSize()) {
        PanelCard {
            Text("ИСТОРИЯ СИГНАЛОВ", color = MUTED, fontSize = 11.sp)
            Text("История сделок пока не сохраняется.", color = Color.White)
            Text("Текущий статус: ${data?.optString("signal", "нет данных") ?: "нет данных"}", color = BLUE)
        }
    }
}

@Composable
private fun PanelCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PANEL)
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MUTED, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun CandleChart(timeframe: String) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(520.dp).background(PANEL, RoundedCornerShape(14.dp)),
        factory = {
            WebView(context).apply {
                setBackgroundColor(AColor.rgb(17, 26, 40))
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                loadDataWithBaseURL("$ENGINE/", chartPage(timeframe), "text/html", "UTF-8", null)
            }
        },
        update = { it.evaluateJavascript("if(window.changeInterval){window.changeInterval('$timeframe');}", null) }
    )
}

private fun chartPage(startInterval: String): String = """
<!doctype html><html><head>
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
<style>
html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#111a28;color:#91a0b5;font-family:Arial}
#status{height:32px;box-sizing:border-box;padding:9px 10px;font-size:11px}
#chart{width:100%;height:calc(100% - 32px)}
</style>
<script src="https://unpkg.com/lightweight-charts@4.2.0/dist/lightweight-charts.standalone.production.js"></script>
</head><body><div id="status">Загрузка Binance…</div><div id="chart"></div>
<script>
(function(){
let interval='$startInterval',chart,candleSeries,ema20Series,ema50Series,volumeSeries;
const status=document.getElementById('status');
function ema(rows,n){let k=2/(n+1),p=null;return rows.map(x=>{p=p===null?x.close:x.close*k+p*(1-k);return {time:x.time,value:p};});}
function init(){
 if(typeof LightweightCharts==='undefined'){status.textContent='Нет библиотеки графика. Проверь интернет.';return;}
 const el=document.getElementById('chart');
 chart=LightweightCharts.createChart(el,{
  width:el.clientWidth,height:el.clientHeight,
  layout:{background:{color:'#111a28'},textColor:'#91a0b5'},
  grid:{vertLines:{color:'#1d2939'},horzLines:{color:'#1d2939'}},
  rightPriceScale:{borderColor:'#344256'},
  timeScale:{borderColor:'#344256',timeVisible:true,secondsVisible:false}
 });
 candleSeries=chart.addCandlestickSeries({upColor:'#22c58b',downColor:'#ff647c',borderUpColor:'#22c58b',borderDownColor:'#ff647c',wickUpColor:'#22c58b',wickDownColor:'#ff647c'});
 ema20Series=chart.addLineSeries({color:'#55a6ff',lineWidth:2,priceLineVisible:false});
 ema50Series=chart.addLineSeries({color:'#f5b942',lineWidth:2,priceLineVisible:false});
 volumeSeries=chart.addHistogramSeries({priceFormat:{type:'volume'},priceScaleId:'volume',scaleMargins:{top:.82,bottom:0}});
 chart.priceScale('volume').applyOptions({scaleMargins:{top:.82,bottom:0}});
 new ResizeObserver(()=>chart.applyOptions({width:el.clientWidth,height:el.clientHeight})).observe(el);
 loadCandles(true);setInterval(()=>loadCandles(false),30000);
}
async function loadCandles(first){
 try{
  status.textContent='Обновление '+interval.toUpperCase()+'…';
  const response=await fetch('/candles?interval='+encodeURIComponent(interval)+'&_='+Date.now());
  if(!response.ok)throw new Error('HTTP '+response.status);
  const json=await response.json();
  if(!json.candles||!json.candles.length)throw new Error('Нет свечей в ответе API');
  const rows=json.candles.map(x=>({time:Math.floor(new Date(x.time).getTime()/1000),open:+x.open,high:+x.high,low:+x.low,close:+x.close,volume:+x.volume}));
  const range=first?null:chart.timeScale().getVisibleLogicalRange();
  candleSeries.setData(rows);
  ema20Series.setData(ema(rows,20));ema50Series.setData(ema(rows,50));
  volumeSeries.setData(rows.map(x=>({time:x.time,value:x.volume,color:x.close>=x.open?'#22c58b66':'#ff647c66'})));
  if(range)chart.timeScale().setVisibleLogicalRange(range);else chart.timeScale().fitContent();
  status.textContent='BINANCE PAXGUSDT • '+interval.toUpperCase()+' • '+rows.length+' свечей';
 }catch(e){status.textContent='Ошибка графика: '+e.message;}
}
window.changeInterval=function(next){if(next!==interval){interval=next;loadCandles(true);}};
window.addEventListener('load',init);
})();
</script></body></html>
""".trimIndent()

private suspend fun getJson(address: String): JSONObject = withContext(Dispatchers.IO) {
    val connection = URL(address).openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 8000
    connection.readTimeout = 8000
    try {
        JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
    } finally {
        connection.disconnect()
    }
}
