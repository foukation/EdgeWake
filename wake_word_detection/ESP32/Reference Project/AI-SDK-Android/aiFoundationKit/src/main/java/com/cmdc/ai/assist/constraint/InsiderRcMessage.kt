package com.cmdc.ai.assist.constraint

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class InsiderRcMessage(
    var role: String, var content: String, @field:SerializedName(
        "Nlu"
    ) var nlu: String, var source: String, var mediaInfo: Any
) :
    Serializable
