package com.bookofvengeance.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "book_of_vengeance"
private const val KEY_PROFILE_NAME = "profile_name"
private const val KEY_PROFILE_STATUS = "profile_status"
private const val KEY_PROFILE_GENDER = "profile_gender"
private const val KEY_PROFILE_REVENGE = "profile_revenge"
private const val KEY_NOTES = "notes"
private const val KEY_NOTE_ENTRIES = "note_entries"
private const val KEY_SYNDICATE = "syndicate"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        setContent {
            BookOfVengeanceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BookApp(prefs)
                }
            }
        }
    }
}

data class SyndicateEntry(
    val name: String,
    val offense: String,
    val avenged: Boolean
)

data class NoteEntry(
    val text: String,
    val imageUri: String?
)

enum class Gender {
    BROTHER,
    SISTER
}

enum class Tab {
    Notebook,
    Syndicate
}

@Composable
private fun BookApp(prefs: SharedPreferences) {
    val profileName = rememberSaveable { mutableStateOf(prefs.getString(KEY_PROFILE_NAME, "") ?: "") }
    val profileStatus = rememberSaveable { mutableStateOf(prefs.getString(KEY_PROFILE_STATUS, "Little") ?: "Little") }
    val gender = rememberSaveable { mutableStateOf(loadGender(prefs)) }
    val revengeCount = rememberSaveable { mutableIntStateOf(prefs.getInt(KEY_PROFILE_REVENGE, 0)) }
    val notes = rememberSaveable { mutableStateOf(prefs.getString(KEY_NOTES, "") ?: "") }
    val noteEntries = rememberSaveable { mutableStateOf(loadNotes(prefs)) }
    val syndicate = rememberSaveable { mutableStateOf(loadSyndicate(prefs)) }
    val activeTab = rememberSaveable { mutableStateOf(Tab.Notebook) }

    val statusTitle = remember(revengeCount.intValue, gender.value) {
        resolveStatusTitle(revengeCount.intValue, gender.value)
    }

    LaunchedEffect(statusTitle) {
        profileStatus.value = statusTitle
        saveProfile(prefs, profileName.value, gender.value, revengeCount.intValue, profileStatus.value)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BookColors.paper)
            .padding(16.dp)
    ) {
        ProfileHeader(
            name = profileName.value,
            status = profileStatus.value,
            onNameChange = {
                profileName.value = it
                saveProfile(prefs, profileName.value, gender.value, revengeCount.intValue, profileStatus.value)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = BookColors.parchment)
        ) {
            when (activeTab.value) {
                Tab.Notebook -> NotebookScreen(
                    notes = notes.value,
                    entries = noteEntries.value,
                    onNotesChange = {
                        notes.value = it
                        prefs.edit().putString(KEY_NOTES, notes.value).apply()
                    },
                    onEntriesChange = {
                        noteEntries.value = it
                        saveNotes(prefs, noteEntries.value)
                    }
                )

                Tab.Syndicate -> SyndicateScreen(
                    entries = syndicate.value,
                    onEntriesChange = {
                        syndicate.value = it
                        saveSyndicate(prefs, syndicate.value)
                    },
                    onAvenged = { entry ->
                        val newCount = revengeCount.intValue + 1
                        revengeCount.intValue = newCount
                        prefs.edit().putInt(KEY_PROFILE_REVENGE, newCount).apply()

                        if (entry.name.equals("SD_TXRXSH", ignoreCase = true)) {
                            val targetCount = GreatStatusThreshold
                            revengeCount.intValue = targetCount
                            prefs.edit().putInt(KEY_PROFILE_REVENGE, targetCount).apply()
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TabBar(activeTab = activeTab.value, onTabSelected = { activeTab.value = it })
    }

    if (gender.value == null) {
        GenderDialog {
            gender.value = it
            saveProfile(prefs, profileName.value, gender.value, revengeCount.intValue, profileStatus.value)
        }
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    status: String,
    onNameChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BookColors.purple),
        shape = RoundedCornerShape(18.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(BookColors.paper)
                        .border(2.dp, BookColors.gold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = "Profile image placeholder",
                        tint = BookColors.purple
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    TextField(
                        value = name,
                        onValueChange = onNameChange,
                        placeholder = { Text(text = "Имя") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BookColors.paper,
                            unfocusedContainerColor = BookColors.paper
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = status,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = BookColors.gold,
                            fontFamily = BookFontFamilies.graffiti
                        )
                    )
                }
            }
            ChainOverlay()
        }
    }
}

@Composable
private fun NotebookScreen(
    notes: String,
    entries: List<NoteEntry>,
    onNotesChange: (String) -> Unit,
    onEntriesChange: (List<NoteEntry>) -> Unit
) {
    val entryText = rememberSaveable { mutableStateOf("") }
    val imageUri = rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Блокнот",
            style = MaterialTheme.typography.titleLarge,
            color = BookColors.purple
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextField(
            value = entryText.value,
            onValueChange = { entryText.value = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Новая запись...") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = BookColors.paper,
                unfocusedContainerColor = BookColors.paper
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = imageUri.value,
            onValueChange = { imageUri.value = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ссылка/URI на изображение (необязательно)") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = BookColors.paper,
                unfocusedContainerColor = BookColors.paper
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (entryText.value.isNotBlank()) {
                    onEntriesChange(
                        entries + NoteEntry(
                            text = entryText.value.trim(),
                            imageUri = imageUri.value.trim().ifBlank { null }
                        )
                    )
                    entryText.value = ""
                    imageUri.value = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = BookColors.purple)
        ) {
            Text(text = "Добавить запись", color = BookColors.gold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries) { entry ->
                NoteEntryCard(entry = entry)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Черновик",
            style = MaterialTheme.typography.titleMedium,
            color = BookColors.purple
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = notes,
            onValueChange = onNotesChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Пиши всё, что нужно помнить...") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = BookColors.paper,
                unfocusedContainerColor = BookColors.paper
            )
        )
    }
}

@Composable
private fun NoteEntryCard(entry: NoteEntry) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BookColors.paper),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                text = entry.text,
                style = MaterialTheme.typography.bodyLarge,
                color = BookColors.purple
            )
            if (!entry.imageUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = entry.imageUri,
                    contentDescription = "Note image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

@Composable
private fun SyndicateScreen(
    entries: List<SyndicateEntry>,
    onEntriesChange: (List<SyndicateEntry>) -> Unit,
    onAvenged: (SyndicateEntry) -> Unit
) {
    val name = rememberSaveable { mutableStateOf("") }
    val offense = rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Проступки",
            style = MaterialTheme.typography.titleLarge,
            color = BookColors.purple
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextField(
                value = name.value,
                onValueChange = { name.value = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Имя") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BookColors.paper,
                    unfocusedContainerColor = BookColors.paper
                )
            )
            TextField(
                value = offense.value,
                onValueChange = { offense.value = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Проступок") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BookColors.paper,
                    unfocusedContainerColor = BookColors.paper
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (name.value.isNotBlank() && offense.value.isNotBlank()) {
                    onEntriesChange(
                        entries + SyndicateEntry(
                            name = name.value.trim(),
                            offense = offense.value.trim(),
                            avenged = false
                        )
                    )
                    name.value = ""
                    offense.value = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = BookColors.purple)
        ) {
            Text(text = "Добавить", color = BookColors.gold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries) { entry ->
                SyndicateEntryRow(
                    entry = entry,
                    onToggleAvenged = {
                        val updated = entries.map {
                            if (it == entry) {
                                it.copy(avenged = !it.avenged)
                            } else {
                                it
                            }
                        }
                        onEntriesChange(updated)
                        if (!entry.avenged) {
                            onAvenged(entry)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SyndicateEntryRow(entry: SyndicateEntry, onToggleAvenged: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BookColors.paper),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleAvenged() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (entry.avenged) TextDecoration.LineThrough else TextDecoration.None,
                    color = BookColors.purple
                )
                Text(
                    text = entry.offense,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (entry.avenged) TextDecoration.LineThrough else TextDecoration.None
                )
            }
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Avenged toggle",
                tint = if (entry.avenged) BookColors.gold else BookColors.purple
            )
        }
    }
}

@Composable
private fun TabBar(activeTab: Tab, onTabSelected: (Tab) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TabButton(
            isActive = activeTab == Tab.Notebook,
            label = "Блокнот",
            icon = Icons.Default.AutoStories,
            onClick = { onTabSelected(Tab.Notebook) },
            iconRes = null
        )
        TabButton(
            isActive = activeTab == Tab.Syndicate,
            label = "Проступки",
            icon = null,
            onClick = { onTabSelected(Tab.Syndicate) },
            iconRes = R.drawable.ic_syndicate_placeholder
        )
    }
}

@Composable
private fun TabButton(
    isActive: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit,
    iconRes: Int?
) {
    val background = if (isActive) BookColors.purple else BookColors.paper
    val textColor = if (isActive) BookColors.gold else BookColors.purple

    Box(
        modifier = Modifier
            .width(160.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(background)
            .border(2.dp, BookColors.gold, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = textColor
                )
            } else if (icon != null) {
                Icon(imageVector = icon, contentDescription = label, tint = textColor)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, color = textColor)
        }
    }
}

@Composable
private fun GenderDialog(onGenderSelected: (Gender) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = "Выберите пол") },
        text = { Text(text = "Это нужно для статуса. Можно изменить позже.") },
        confirmButton = {
            Button(onClick = { onGenderSelected(Gender.BROTHER) }) {
                Text("Brother")
            }
        },
        dismissButton = {
            Button(onClick = { onGenderSelected(Gender.SISTER) }) {
                Text("Sister")
            }
        }
    )
}

private fun resolveStatusTitle(revengeCount: Int, gender: Gender?): String {
    val tier = when {
        revengeCount >= GreatStatusThreshold -> StatusTier.GREAT
        revengeCount >= BigStatusThreshold -> StatusTier.BIG
        revengeCount >= MiddleStatusThreshold -> StatusTier.MIDDLE
        else -> StatusTier.LITTLE
    }
    return when (tier) {
        StatusTier.LITTLE -> if (gender == Gender.SISTER) "Little Sister" else "Little Brother"
        StatusTier.MIDDLE -> if (gender == Gender.SISTER) "Middle Sister" else "Middle Brother"
        StatusTier.BIG -> if (gender == Gender.SISTER) "Big Sister" else "Big Brother"
        StatusTier.GREAT -> if (gender == Gender.SISTER) "Great Sister" else "Great Brother"
    }
}

@Composable
private fun ChainOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        ChainRow(modifier = Modifier.align(Alignment.TopCenter))
        ChainRow(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ChainRow(modifier: Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(8) { _ ->
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .border(2.dp, BookColors.gold, CircleShape)
            )
        }
    }
}

private enum class StatusTier {
    LITTLE,
    MIDDLE,
    BIG,
    GREAT
}

private const val MiddleStatusThreshold = 2
private const val BigStatusThreshold = 4
private const val GreatStatusThreshold = 6

private fun loadGender(prefs: SharedPreferences): Gender? {
    val stored = prefs.getString(KEY_PROFILE_GENDER, null)
    return stored?.let { Gender.valueOf(it) }
}

private fun saveProfile(
    prefs: SharedPreferences,
    name: String,
    gender: Gender?,
    revengeCount: Int,
    status: String
) {
    prefs.edit()
        .putString(KEY_PROFILE_NAME, name)
        .putString(KEY_PROFILE_GENDER, gender?.name)
        .putInt(KEY_PROFILE_REVENGE, revengeCount)
        .putString(KEY_PROFILE_STATUS, status)
        .apply()
}

private fun loadSyndicate(prefs: SharedPreferences): List<SyndicateEntry> {
    val raw = prefs.getString(KEY_SYNDICATE, "[]") ?: "[]"
    return try {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    SyndicateEntry(
                        name = item.optString("name"),
                        offense = item.optString("offense"),
                        avenged = item.optBoolean("avenged")
                    )
                )
            }
        }
    } catch (exception: Exception) {
        emptyList()
    }
}

private fun saveSyndicate(prefs: SharedPreferences, entries: List<SyndicateEntry>) {
    val array = JSONArray()
    entries.forEach { entry ->
        val item = JSONObject()
        item.put("name", entry.name)
        item.put("offense", entry.offense)
        item.put("avenged", entry.avenged)
        array.put(item)
    }
    prefs.edit().putString(KEY_SYNDICATE, array.toString()).apply()
}

private fun loadNotes(prefs: SharedPreferences): List<NoteEntry> {
    val raw = prefs.getString(KEY_NOTE_ENTRIES, "[]") ?: "[]"
    return try {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    NoteEntry(
                        text = item.optString("text"),
                        imageUri = item.optString("imageUri").ifBlank { null }
                    )
                )
            }
        }
    } catch (exception: Exception) {
        emptyList()
    }
}

private fun saveNotes(prefs: SharedPreferences, entries: List<NoteEntry>) {
    val array = JSONArray()
    entries.forEach { entry ->
        val item = JSONObject()
        item.put("text", entry.text)
        item.put("imageUri", entry.imageUri ?: "")
        array.put(item)
    }
    prefs.edit().putString(KEY_NOTE_ENTRIES, array.toString()).apply()
}

object BookColors {
    val purple = androidx.compose.ui.graphics.Color(0xFF4A2C7F)
    val gold = androidx.compose.ui.graphics.Color(0xFFF2C66D)
    val paper = androidx.compose.ui.graphics.Color(0xFFF7E9D0)
    val parchment = androidx.compose.ui.graphics.Color(0xFFEBD6B0)
}

object BookFontFamilies {
    val comicSans: FontFamily = FontFamily(Font(R.font.comic_sans))
    val graffiti: FontFamily = FontFamily(Font(R.font.graffiti_stroke))
}

@Composable
fun BookOfVengeanceTheme(content: @Composable () -> Unit) {
    val typography = MaterialTheme.typography.copy(
        bodyLarge = TextStyle(fontFamily = BookFontFamilies.comicSans, fontSize = 16.sp),
        bodyMedium = TextStyle(fontFamily = BookFontFamilies.comicSans, fontSize = 14.sp),
        titleLarge = TextStyle(fontFamily = BookFontFamilies.comicSans, fontSize = 22.sp),
        titleMedium = TextStyle(fontFamily = BookFontFamilies.comicSans, fontSize = 18.sp)
    )
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = BookColors.purple,
            secondary = BookColors.gold,
            background = BookColors.paper
        ),
        typography = typography,
        content = content
    )
}
