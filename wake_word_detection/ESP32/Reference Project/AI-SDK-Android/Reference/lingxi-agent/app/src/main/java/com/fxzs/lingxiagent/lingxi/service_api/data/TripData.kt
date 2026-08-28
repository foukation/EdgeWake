package com.example.service_api.data

import java.io.Serializable

data class TripContentRes(
    val code: Int,
    val msg: String,
    val data: TripListData,
): Serializable

data class TripListData(
    val count: Int,
    val pageIndex: Int,
    val pageSize: Int,
    val list: ArrayList<TripItemContent>?
): Serializable

data class TripItemContent(
    val id: Int,
    val title: String,
    val description: String,
    val command: String,
    val status: Int,
    val sub_status1: Int,
    val sub_status2: Int,
    val createdAt: String,
    val result: TripResultContent?,
    val all_result: TripAllResultContent?,
): Serializable

data class TripResultContent(
    val file_url: String,
): Serializable

data class  TripAllResultContent(
    val poi_search_result: PoiSearchResultContent,
    val status_summary_info: String,
): Serializable

data class PoiSearchResultContent(
    val top1_poi_recommend_reason: String?,
    val title: String,
    val sub_title: String,
    val poi_list: ArrayList<PoiItem>
): Serializable

data class PoiItem(
    val name: String,
    val address: String,
    val opentime_week: String,
    val rating: String,
    val poi_photos: ArrayList<String>,
    val type: String,
    val recommend_reason: String,
    val location:String,
): Serializable

data class TripDelRes(
    val code: Int,
    val msg: String,
): Serializable

data class TripCreateParams(
    val title: String,
    val description: String,
    val command: String,
): Serializable

data class TripCreateRes(
    val code: Int,
    val msg: String,
    val data: TripCreateData,
): Serializable

data class TripCreateData(
    val taskId: Int,
): Serializable

