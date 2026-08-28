package com.fxzs.lingxiagent.lingxi.service_api.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue


@Parcelize
data class FoodList(
    val list: @RawValue ArrayList<FoodItem>,
    var moreLink: ItemLink,
): Parcelable

data class FoodItem(
    val img: String,
    var title: String,
    var score: String,
    var tag: ArrayList<String>,
    var address: String,
    var subTag: ArrayList<String>,
    var itemLink: ItemLink,
    var buttonLink: ButtonLink,
    var location: String
)

@Parcelize
data class ItemLink(
    val web: WebContent,
): Parcelable

@Parcelize
data class WebContent(
    val url: String,
): Parcelable

@Parcelize
data class ButtonLink(
    val nativeApp: NativeAppData,
    val text: String,
): Parcelable

@Parcelize
data class NativeAppData(
    var url: String,
    var appName: String,
    var pkgName: String,
): Parcelable