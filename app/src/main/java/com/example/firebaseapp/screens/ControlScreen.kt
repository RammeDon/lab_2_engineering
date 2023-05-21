package com.example.firebaseapp.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


fun rememberFirestoreCollection(
): SnapshotStateList<DocumentSnapshot> {
    val documents = mutableStateListOf<DocumentSnapshot>()  //mutableStateOf(MutableList<T>())
    val collectionRef = FirebaseFirestore.getInstance().collection("devices")

    collectionRef.addSnapshotListener { snapshot, error ->
        if (error != null) {
            // Handle the error
            return@addSnapshotListener
        }
        if (snapshot != null) {
            documents.clear()
            snapshot.documents.forEach { item ->
                documents.add(item)
            }
        }
    }
    return documents
}

@Composable
fun ControlScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    FirebaseApp.initializeApp(context)
    val devices = rememberFirestoreCollection()
    Box(modifier = modifier){
        Column {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .padding(top = 10.dp)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(items = devices, key = { item -> item.id }) { item ->
                    //Log.d("LOOK", item.get("name") as String)
                    DeviceItem(item = item)
                }
            }
        }
    }
}

@Composable
fun DeviceItem(item: DocumentSnapshot) {
    val context = LocalContext.current
    val image: Painter = when (item.get("name") as String) {
        "light" -> {
            if (item.get("state") as String == "on") painterResource(id = com.example.firebaseapp.R.drawable.light_bulb_on) else painterResource(
                id = com.example.firebaseapp.R.drawable.light_bulb_off
            )
        }
        "door" -> {
            if (item.get("state") as String == "open") painterResource(id = com.example.firebaseapp.R.drawable.door_open) else painterResource(
                id = com.example.firebaseapp.R.drawable.door_close
            )
        }
        "window" -> {
            if (item.get("state") as String == "open") painterResource(id = com.example.firebaseapp.R.drawable.window_open) else painterResource(
                id = com.example.firebaseapp.R.drawable.window_closed
            )
        }

        else -> {
            painterResource(id = com.example.firebaseapp.R.drawable.light_bulb_on)
        } // change to nuffin
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.get("name") as String,
            modifier = Modifier.weight(2f),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(text = item.get("state") as String)
        Spacer(modifier = Modifier.weight(1f))
        Image(painter = image, contentDescription = "")
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = { toggle(context, item) }) {
            Text(text = "toggle")
        }
    }

}

fun toggle(context: Context, item: DocumentSnapshot){
    updateFirestoreDocumentField(
        context = context,
        documentId = item.id,
        fieldName = "state",
        currentState = item.get("state") as String,
        itemType = item.get("type") as String
    )
}

@OptIn(DelicateCoroutinesApi::class)
fun updateFirestoreDocumentField(
    context: Context,
    documentId: String,
    fieldName: String,
    itemType: String,
    currentState: String,
) {
    val db = FirebaseFirestore.getInstance()
    val documentRef = db.collection("devices").document(documentId)

    val newValue: String = when (itemType) {
        "openClose" -> if (currentState == "open") "close" else "open"
        "toggle" -> if (currentState == "on") "off" else "on"
        else -> {
            "no State"
        }
    }

    val fieldMap = hashMapOf<String, String>(fieldName to newValue)

    GlobalScope.launch(Dispatchers.IO) {
        try {
            documentRef.update(fieldMap as Map<String, Any>).await()
            Log.d("LOOOOOOK", "success")
        } catch (e: Exception) {
            Log.d("LOOOOOOK", "fail: ${e.message}")
        }
    }
}
