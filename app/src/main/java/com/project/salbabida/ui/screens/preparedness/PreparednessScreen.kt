package com.project.salbabida.ui.screens.preparedness

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.project.salbabida.R

data class FloodPreparednessTip(
    val title: String,
    val description: String
)

@Composable
fun PreparednessScreen(modifier: Modifier = Modifier) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val tabs = listOf(
        stringResource(R.string.prep_tab_before),
        stringResource(R.string.prep_tab_during),
        stringResource(R.string.prep_tab_after)
    )

    val tipsBefore = remember {
        listOf(
            R.string.prep_before_1_title to R.string.prep_before_1_desc,
            R.string.prep_before_2_title to R.string.prep_before_2_desc,
            R.string.prep_before_3_title to R.string.prep_before_3_desc,
            R.string.prep_before_4_title to R.string.prep_before_4_desc,
            R.string.prep_before_5_title to R.string.prep_before_5_desc,
            R.string.prep_before_6_title to R.string.prep_before_6_desc,
        )
    }

    val tipsDuring = remember {
        listOf(
            R.string.prep_during_1_title to R.string.prep_during_1_desc,
            R.string.prep_during_2_title to R.string.prep_during_2_desc,
            R.string.prep_during_3_title to R.string.prep_during_3_desc,
            R.string.prep_during_4_title to R.string.prep_during_4_desc,
            R.string.prep_during_5_title to R.string.prep_during_5_desc,
        )
    }

    val tipsAfter = remember {
        listOf(
            R.string.prep_after_1_title to R.string.prep_after_1_desc,
            R.string.prep_after_2_title to R.string.prep_after_2_desc,
            R.string.prep_after_3_title to R.string.prep_after_3_desc,
            R.string.prep_after_4_title to R.string.prep_after_4_desc,
        )
    }

    val currentResIds = when (selectedTabIndex) {
        0 -> tipsBefore
        1 -> tipsDuring
        2 -> tipsAfter
        else -> tipsBefore
    }

    val currentTips = currentResIds.map { (titleRes, descRes) ->
        FloodPreparednessTip(
            title = stringResource(titleRes),
            description = stringResource(descRes)
        )
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
            placeholder = { Text(stringResource(R.string.prep_search_hint), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
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
                    contentDescription = if (expanded) stringResource(R.string.prep_collapse) else stringResource(R.string.prep_expand)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Text(
                    text = tip.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
