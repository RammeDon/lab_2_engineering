package com.example.firebaseapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import com.example.firebaseapp.screens.ControlScreen
import com.example.firebaseapp.screens.toggle
import com.example.firebaseapp.ui.theme.FirebaseAppTheme
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FirebaseAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 0)
                    RunAPP(onMicClicked = { getSpeechInput() })
                }
            }
        }
    }

    // mic feedback handling
    val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult
        ()) { speechResult ->
        if (speechResult.resultCode == Activity.RESULT_OK) {
            val data: ArrayList<String> =
                speechResult.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?: return@registerForActivityResult
            val voiceInput = data[0].lowercase()  // returns the speech input command by the user
            FirebaseFirestore.getInstance().collection("devices").get()
                .addOnSuccessListener { it ->
                    it.documents.forEach {
                        if (voiceInput.contains((it.get("name") as String).lowercase())) {
                            val actions = voiceInput.split("and")
                            val it: DocumentSnapshot = it
                            val context = this.baseContext
                            actions.forEach { str ->
                                if (str.lowercase()
                                        .contains((it.get("name") as String).lowercase())
                                ) {
                                    toggle(context, it)
                                }
                            }
                        }
                    }
                }
        } else { /* do nothing */
        }
    }
    // mic handling
    private fun getSpeechInput() {
        val context = this.baseContext
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Speech not Available", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
            )
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak Something")
            // at last we are calling start activity for result to start our activity.

            launcher.launch(intent)
        }
    }
}

@Composable
fun RunAPP(onMicClicked: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {},
        content  = {
            ControlScreen(modifier = Modifier.padding(it))
        },
        bottomBar = {},
        floatingActionButton = {
            FloatingActionButton(onClick = { onMicClicked() }, modifier = Modifier.zIndex(2f)) {
                Icon(tint = Color.Black, imageVector = Icons.Rounded.Mic, contentDescription =
                "Microphone", modifier = Modifier.scale(1.3f))
            }
        }, isFloatingActionButtonDocked = true, floatingActionButtonPosition = FabPosition.End
    )

}
