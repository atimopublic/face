package com.tcc.face.feature.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.navigation.fragment.NavHostFragment
import com.tcc.face.R
import com.tcc.face.base.websocket.WebSocketCallback
import com.tcc.face.base.websocket.WebSocketManager
import com.tcc.face.base.websocket.WebSocketMessage

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    //val signupViewModel: SignUpViewModel by viewModels()
    private lateinit var webSocketManager: WebSocketManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //initialize navigation
        setupNavigation()

    }

    fun setupNavigation()
    {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController
      //  NavigationUI.setupActionBarWithNavController(this, navController)
      //  appBarConfiguration = AppBarConfiguration(navController.graph)
    }



}