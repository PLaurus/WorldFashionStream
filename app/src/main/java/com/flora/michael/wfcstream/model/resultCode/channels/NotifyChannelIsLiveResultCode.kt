package com.flora.michael.wfcstream.model.resultCode.channels

import androidx.annotation.StringRes
import com.flora.michael.wfcstream.R
import com.google.gson.annotations.SerializedName

enum class NotifyChannelIsLiveResultCode(
    @StringRes
    val description: Int
){
    @SerializedName("0")
    Success(R.string.notify_channel_is_live_result_code_success),
    @SerializedName("1")
    TokenDoesNotExist(R.string.notify_channel_is_live_result_code_token_does_not_exist),
    @SerializedName("2")
    ChannelIsAlreadyLive(R.string.notify_channel_is_live_result_code_channel_is_already_live),
    @SerializedName("3")
    Unknown(R.string.notify_channel_is_live_result_code_unknown),
    @SerializedName("4")
    DefaultError(R.string.base_result_code_default_error_message);
}