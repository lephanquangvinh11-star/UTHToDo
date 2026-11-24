package com.uthtodo.dpgh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.uthtodo.dpgh.data.local.AppPreferences
import com.uthtodo.dpgh.data.local.SessionManager
import com.uthtodo.dpgh.data.remote.FirebaseManager
import com.uthtodo.dpgh.data.repository.TaskRepository
import com.uthtodo.dpgh.ui.navigation.AppNavGraph
import com.uthtodo.dpgh.ui.theme.BaiTap5Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppPreferences.init(this)
        FirebaseManager.init(this)
        SessionManager.init(this)
        TaskRepository.init(this)
        setContent {
            BaiTap5Theme {
                AppNavGraph()
            }
        }
    }
}
