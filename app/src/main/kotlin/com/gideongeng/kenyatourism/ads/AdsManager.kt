package com.gideongeng.kenyatourism.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem

import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd

object AdsManager {
    private const val APP_ID = "ca-app-pub-1281448884303417~5992573219"
    private const val BANNER_ID = "ca-app-pub-1281448884303417/8621163022"
    private const val INTERSTITIAL_ID = "ca-app-pub-1281448884303417/3041533286"
    private const val REWARDED_INTERSTITIAL_ID = "ca-app-pub-1281448884303417/5396233885"
    private const val NATIVE_ID = "ca-app-pub-1281448884303417/9409074044"
    private const val APP_OPEN_ID = "ca-app-pub-1281448884303417/8095992378"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null

    fun initialize(context: Context) {
        MobileAds.initialize(context) {}
        loadAppOpenAd(context)
    }

    fun loadInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, INTERSTITIAL_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialAd = null
            }
        })
    }

    fun showInterstitial(activity: Activity) {
        interstitialAd?.show(activity)
    }

    fun loadRewardedInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedInterstitialAd.load(context, REWARDED_INTERSTITIAL_ID, adRequest, object : RewardedInterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedInterstitialAd) {
                rewardedInterstitialAd = ad
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                rewardedInterstitialAd = null
            }
        })
    }

    fun showRewardedInterstitial(activity: Activity, onEarned: (reward: RewardItem) -> Unit) {
        rewardedInterstitialAd?.show(activity) { reward ->
            onEarned(reward)
        }
    }

    private fun loadAppOpenAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(context, APP_OPEN_ID, adRequest, object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                appOpenAd = ad
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                appOpenAd = null
            }
        })
    }

    fun showAppOpenAd(activity: Activity) {
        appOpenAd?.show(activity)
    }
}
