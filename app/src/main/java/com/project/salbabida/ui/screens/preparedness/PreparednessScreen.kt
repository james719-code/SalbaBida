package com.project.salbabida.ui.screens.preparedness

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class FloodPreparednessTip(
    val title: String,
    val description: String
)

private val tipsBefore = listOf(
    FloodPreparednessTip(
        "Makinig sa Ulat-Panahon",
        "Laging makinig ng mga ulat-panahon mula sa PAGASA at NDRRMC upang maging handa sa anumang babala ng bagyo at pagbaha."
    ),
    FloodPreparednessTip(
        "Magtalaga ng Emergency Kit",
        "Maghanda ng Emergency Kit na naglalaman ng pagkain, tubig, gamot, flashlight, baterya, at mahahalagang dokumento."
    ),
    FloodPreparednessTip(
        "Ilipat ang Mahahalagang Bagay",
        "Ilipat ang mahahalagang gamit tulad ng dokumento, electronics, at kasangkapan sa mataas na bahagi ng bahay upang maiwasan ang pagkasira dahil sa baha."
    ),
    FloodPreparednessTip(
        "Suriin ang Katatagan ng Bahay",
        "Siguraduhing matibay ang mga bahagi ng bahay at walang butas o siwang na maaaring pasukan ng tubig-baha."
    ),
    FloodPreparednessTip(
        "Alamin ang Evacuation Center",
        "Alamin ang pinakamalapit na evacuation center at mga ligtas na ruta patungo rito upang mabilis makalikas kung kinakailangan."
    ),
    FloodPreparednessTip(
        "Gamitin ang SALBA-bida App",
        "Para sa karagdagang paghahanda, gamitin ang SALBA-bida app upang makatanggap ng real-time updates at emergency assistance."
    )
)

private val tipsDuring = listOf(
    FloodPreparednessTip(
        "Manatili sa Ligtas na Lugar",
        "Manatili sa ligtas na lugar at iwasang lumusong sa baha lalo na kung hindi alam ang lalim nito upang maiwasan ang panganib."
    ),
    FloodPreparednessTip(
        "Patayin ang Kuryente",
        "Tiyaking nakapatay ang main switch ng kuryente at iwasan ang mga kable o outlet na nakababad sa tubig upang maiwasan ang electrocution."
    ),
    FloodPreparednessTip(
        "Magtutok sa Balita",
        "Patuloy na tumutok sa mga balita para sa mga update at abiso mula sa mga awtoridad upang manatiling ligtas at handa."
    ),
    FloodPreparednessTip(
        "Maging Kalma at Handa",
        "Manatiling nakaantabay sa mga paparating na rescuers at huwag mataranta upang mas madaling mailikas kung kinakailangan."
    ),
    FloodPreparednessTip(
        "Gamitin ang SALBA-bida App",
        "Tumutok sa SALBA-bida app upang maging alerto ang Barangay sa kalagayan ng inyong lugar at makatanggap ng mahahalagang update."
    )
)

private val tipsAfter = listOf(
    FloodPreparednessTip(
        "Suriin ang Kuryente Bago Buksan",
        "Tiyaking walang buhay na kable o outlet na nakababad sa tubig bago buksan ang kuryente upang masiguro ang kaligtasan."
    ),
    FloodPreparednessTip(
        "Ipagbigay-Alam ang mga Nasirang Pasilidad",
        "Ipagbigay-alam sa mga kinauukulan ang mga nasirang pasilidad tulad ng poste ng kuryente, tubo ng tubig, at iba pa upang maiwasan ang karagdagang pinsala o aksidente."
    ),
    FloodPreparednessTip(
        "Iwasan ang Maruming Tubig",
        "Iwasan ang paglusong sa maruruming tubig dulot ng pagbaha upang maiwasan ang sakit at impeksyon."
    ),
    FloodPreparednessTip(
        "Tingnan ang SALBA-bida App para sa Tulong",
        "Bisitahin ang SALBA-bida app para sa mga donasyon mula sa mga Government at Non-government Organizations bilang tulong sa mga nasalanta ng matinding pagbaha."
    )
)

@Composable
fun PreparednessScreen(modifier: Modifier = Modifier) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    
    val tabs = listOf("Before", "During", "After")
    
    val currentTips = when (selectedTabIndex) {
        0 -> tipsBefore
        1 -> tipsDuring
        2 -> tipsAfter
        else -> tipsBefore
    }
    
    val filteredTips = if (searchQuery.isBlank()) {
        currentTips
    } else {
        currentTips.filter { 
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.description.contains(searchQuery, ignoreCase = true) 
        }
    }
    
    Column(modifier = modifier) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search tips...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredTips) { tip ->
                ExpandableTipCard(tip = tip)
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ExpandableTipCard(tip: FloodPreparednessTip) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tip.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = tip.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
