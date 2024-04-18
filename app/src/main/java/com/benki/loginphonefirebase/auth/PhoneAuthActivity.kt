package com.benki.loginphonefirebase.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benki.loginphonefirebase.MainActivity
import com.benki.loginphonefirebase.auth.ui.theme.LoginPhoneFirebaseTheme
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class PhoneAuthActivity : ComponentActivity() {
    private val TAG = "AUTH_PHONE"
    private var otpSent by mutableStateOf(false)
    private var loginSuccess by mutableStateOf(false)
    private var waitingForAutoRetrieval by mutableStateOf(true)
    private var otp by mutableStateOf("")
    private var verificationIdFirebase: String? = null
    private var tokenFirebase: PhoneAuthProvider.ForceResendingToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginPhoneFirebaseTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val modifier = Modifier
                    var phoneNumber by remember {
                        mutableStateOf("1234567890")
                    }

                    val focusManager = LocalFocusManager.current

                    Column(
                        modifier = modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Login With Phone",
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = modifier.height(24.dp))
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            modifier = modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            ),
                            label = { Text(text = "Phone Number") },
                            placeholder = { Text(text = "Enter Phone Number") },
                            maxLines = 1,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            keyboardActions = KeyboardActions(onNext = {
                                focusManager.moveFocus(FocusDirection.Down)
                            }),
                            prefix = { Text(text = "+91") }
                        )
                        AnimatedVisibility(visible = !otpSent) {
                            Column {
                                Spacer(modifier = modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        getOTP(phoneNumber)
                                    },
                                    modifier = modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = phoneNumber.length == 10
                                ) {
                                    Text(text = "Send OTP")
                                }
                            }
                        }
                        Spacer(modifier = modifier.height(24.dp))
                        AnimatedVisibility(visible = otpSent) {
                            Column(
                                modifier = modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "Enter OTP",
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = modifier.height(8.dp))
                                BasicTextField(
                                    value = otp,
                                    onValueChange = { otp = it },
                                    modifier = modifier.fillMaxWidth(),
                                    keyboardActions = KeyboardActions(onDone = {
                                        focusManager.clearFocus()
                                    }),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Done
                                    ),
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = modifier.fillMaxWidth()
                                    ) {
                                        repeat(6) { index ->
                                            val char = when {
                                                index >= otp.length -> ""
                                                else -> otp[index].toString()
                                            }
                                            Text(
                                                text = char,
                                                modifier = modifier
                                                    .border(
                                                        1.dp,
                                                        if (index == otp.length) MaterialTheme.colorScheme.primary else Color.Gray,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .size(50.dp),
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                lineHeight = 50.sp
                                            )
                                        }
                                    }
                                }
                                if (waitingForAutoRetrieval) {
                                    Spacer(modifier = modifier.height(16.dp))
                                    Row(
                                        modifier = modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = modifier.size(30.dp))
                                        Text(text = "Trying to auto fill")
                                    }
                                }
                                Spacer(modifier = modifier.height(16.dp))
                                Button(
                                    onClick = { verifyOTP(otp) },
                                    modifier = modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = otp.length == 6
                                ) {
                                    Text(text = "Verify OTP")
                                }
                            }
                        }
                    }
                }

                LaunchedEffect(key1 = loginSuccess) {
                    if (loginSuccess) {
                        val intent = Intent(applicationContext, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
                        }
                        startActivity(intent)
                        this@PhoneAuthActivity.finish()
                    }
                }
            }
        }
    }

    private fun getOTP(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber("+91$phoneNumber") // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credenial: PhoneAuthCredential) {
                    Log.d(TAG, "Verification Completed")
                    otp = credenial.smsCode ?: ""
                    signInWithPhoneAuthCredential(credenial)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    // This callback is invoked in an invalid request for verification is made,
                    // for instance if the the phone number format is not valid.
                    Log.w(TAG, "onVerificationFailed", e)

                    when (e) {
                        is FirebaseAuthInvalidCredentialsException -> {
                            Toast.makeText(this@PhoneAuthActivity, "Invalid Request", Toast.LENGTH_LONG)
                                .show()
                        }

                        is FirebaseTooManyRequestsException -> {
                            Toast.makeText(this@PhoneAuthActivity, "SMS quota exceeded", Toast.LENGTH_LONG)
                                .show()
                        }

                        is FirebaseAuthMissingActivityForRecaptchaException -> {
                            Toast.makeText(this@PhoneAuthActivity, "reCAPTCHA verification attempted with null Activity", Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    super.onCodeSent(verificationId, token)
                    otpSent = true
                    waitingForAutoRetrieval = true
                    verificationIdFirebase = verificationId
                    tokenFirebase = token
                }

                override fun onCodeAutoRetrievalTimeOut(p0: String) {
                    super.onCodeAutoRetrievalTimeOut(p0)
                    Log.d(TAG, "Timeout")
                    waitingForAutoRetrieval = false
                }
            }) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyOTP(otp: String) {
        if (verificationIdFirebase != null) {
            signInWithPhoneAuthCredential(
                PhoneAuthProvider.getCredential(
                    verificationIdFirebase!!,
                    otp
                )
            )
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        Firebase.auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    loginSuccess = true
                    val user = task.result?.user
                    Log.d(TAG, "User: ${user?.uid}")
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        Toast.makeText(this@PhoneAuthActivity, "Invalid OTP", Toast.LENGTH_LONG)
                            .show()
                    }
                    // Update UI
                }
            }
    }
}