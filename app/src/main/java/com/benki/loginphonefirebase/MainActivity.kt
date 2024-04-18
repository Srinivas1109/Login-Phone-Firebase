package com.benki.loginphonefirebase

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.benki.loginphonefirebase.ui.theme.LoginPhoneFirebaseTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    private val TAG = "FIREBASE_TASK"

    private var allTasks = mutableStateListOf<Pair<String, String>>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginPhoneFirebaseTheme {
                val coroutine = rememberCoroutineScope()
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val modifier = Modifier
                    var title by remember {
                        mutableStateOf("")
                    }
                    var description by remember {
                        mutableStateOf("")
                    }
                    Column(
                        modifier = modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Total Tasks ${allTasks.size}")
                        allTasks.forEach {
                            Text(text = it.first)
                        }
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            modifier = modifier.fillMaxWidth()
                        )
                        Spacer(modifier = modifier.height(24.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            modifier = modifier.fillMaxWidth()
                        )
                        Spacer(modifier = modifier.height(24.dp))
                        Button(
                            onClick = {
                                coroutine.launch {
                                    insertTask(title, description)
                                }
                            },
                            modifier = modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Add")
                        }
                        Spacer(modifier = modifier.height(24.dp))
                        Button(
                            onClick = {
                                coroutine.launch {
                                    getTasks()
                                }
                            },
                            modifier = modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Get Tasks")
                        }

                        Spacer(modifier = modifier.height(24.dp))
                        Button(
                            onClick = {
                                signOut()
                            },
                            modifier = modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Logout")
                        }
                    }
                }
            }
        }
    }

    private suspend fun insertTask(title: String, description: String) {
        val db = Firebase.firestore
        val task = hashMapOf("title" to title, "description" to description)
        try {
            db.collection("tasks").add(task).await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed", e)
        }
    }

    private suspend fun getTasks() {
        val db = Firebase.firestore
        try {
            val tasks = db.collection("tasks").get().await().map {
                it.get("title").toString() to it.get("description").toString()
            }
            allTasks.addAll(tasks)
        } catch (e: Exception) {
            Log.e(TAG, "Failed", e)
        }
    }

    private fun signOut(){
        Firebase.auth.signOut()
    }
}