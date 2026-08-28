package com.fxzs.lingxiagent.lingxi.service_api.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WeatherContent(
    val description: String,
    val weatherForecast: ArrayList<WeatherForecast>,
): Parcelable

@Parcelize
data class WeatherIcon(
    val src: String,
): Parcelable

@Parcelize
data class WeatherIndexes(
    val level: String,
    val suggestion: String,
    val type: String
): Parcelable

@Parcelize
data class WeatherForecast(
    val airQuality: String,
    val currentAirQuality: String,
    val currentPM25: Number,
    val currentTemperature: String,
    val date: String,
    val day: String,
    val highTemperature: String,
    val lowTemperature: String,
    val pm25: Number,
    val weatherCondition: String,
    val weatherIcon: WeatherIcon,
    val windCondition: String,
    val indexes: ArrayList<WeatherIndexes>,
): Parcelable
