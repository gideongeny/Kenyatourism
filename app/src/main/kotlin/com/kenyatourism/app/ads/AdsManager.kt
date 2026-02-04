package com.kenyatourism.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem

object AdsManager {
    private const val APP_ID = "ca-app-pub-1281448884303417~1527892289"
    private const val BANNER_ID = "ca-app-pub-1281448884303417/5175601050"
    private const val INTERSTITIAL_ID = "ca-app-pub-1281448884303417/7969285357"
    private const val REWARDED_ID = "ca-app-pub-1281448884303417/2477285527"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    fun initialize(context: Context) {
        MobileAds.initialize(context) {}
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

    fun loadRewarded(context: Context) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, REWARDED_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }
            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedAd = null
            }
        })
    }

    fun showRewarded(activity: Activity, onEarned: (reward: RewardItem) -> Unit) {
        rewardedAd?.show(activity) { reward ->
            onEarned(reward)
        }
    }
}
