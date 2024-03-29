package com.d101.presentation.splash

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.d101.presentation.BuildConfig
import com.d101.presentation.R
import com.d101.presentation.databinding.DialogUpdateBinding
import com.d101.presentation.main.MainActivity
import com.d101.presentation.music.BackgroundMusicService
import com.d101.presentation.music.BackgroundMusicService.Companion.MUSIC_NAME
import com.d101.presentation.music.BackgroundMusicService.Companion.PLAY
import com.d101.presentation.welcome.WelcomeActivity
import dagger.hilt.android.AndroidEntryPoint
import utils.CustomToast
import utils.repeatOnStarted

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    val viewModel: SplashViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        collectEvent()
    }

    private fun collectEvent() {
        repeatOnStarted {
            viewModel.eventFlow.collect { event ->
                when (event) {
                    SplashViewEvent.ShowSplash -> viewModel.showSplash()
                    SplashViewEvent.AutoSignInFailure -> navigateToSignIn()
                    SplashViewEvent.AutoSignInSuccess -> navigateToMain()
                    is SplashViewEvent.CheckAppStatus -> checkAppStatus(event)
                    is SplashViewEvent.SetBackGroundMusic -> startMusicService(event.musicName)
                    is SplashViewEvent.OnServerMaintaining -> blockApp(event.message)
                    is SplashViewEvent.OnShowToast -> blockApp(event.message)
                }
            }
        }
    }

    private fun blockApp(message: String) {
        showToast(message)
        ActivityCompat.finishAffinity(this)
    }

    private fun checkAppStatus(event: SplashViewEvent.CheckAppStatus) {
        if (needToUpdate(event.minVersion)) {
            showUpdateDialog(event)
            return
        }

        viewModel.checkSignInStatus()
    }

    private fun showUpdateDialog(event: SplashViewEvent.CheckAppStatus) {
        val dialog = createFullScreenDialog()
        val dialogBinding = DialogUpdateBinding.inflate(layoutInflater)
        dialogBinding.updateButtonTextView.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.storeUrl))
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://play.google.com/store/apps/details?id=$packageName",
                    ),
                )
                startActivity(intent)
            }
        }

        dialog.setOnDismissListener {
            ActivityCompat.finishAffinity(this)
        }
        dialog.setContentView(dialogBinding.root)
        dialog.show()
    }

    private fun createFullScreenDialog(): Dialog {
        return Dialog(this, R.style.Base_FTR_FullScreenDialog).apply {
            window?.setBackgroundDrawableResource(R.drawable.btn_white_green_36dp)
        }
    }

    private fun needToUpdate(minVersion: String): Boolean {
        val (appMajor, appMinor, appPatch) = BuildConfig.APP_VERSION_NAME.split(".")
            .map { it.toInt() }

        val (minMajor, minMinor, minPatch) = minVersion.split(".")
            .map { it.toInt() }

        if (appMajor < minMajor) return true
        if (appMajor == minMajor && appMinor < minMinor) return true
        if (appMajor == minMajor && appMinor == minMinor && appPatch < minPatch) return true
        return false
    }

    private fun showToast(message: String) {
        CustomToast.createAndShow(this, message)
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToSignIn() {
        val intent = Intent(this, WelcomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startMusicService(musicName: String) {
        startService(
            Intent(this, BackgroundMusicService::class.java).apply {
                action = PLAY
                putExtra(MUSIC_NAME, musicName)
            },
        )
    }
}
