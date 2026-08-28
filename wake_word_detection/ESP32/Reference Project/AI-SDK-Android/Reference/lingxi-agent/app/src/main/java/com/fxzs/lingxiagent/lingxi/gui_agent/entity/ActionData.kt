package com.fxzs.lingxiagent.lingxi.gui_agent.entity


enum class ActionType (val alias: String) {
    CLICK("click"),
    INPUT("input"),
    LONG_PRESS("long_press"),
    PRESS("press"),
    OPEN_APP("open_app"),
    SCROLL("scroll"),
    DRAG("drag"),
    CLARIFICATION("clarification"),
    FINISHED("finished");
}

enum class ClarificationType(val alias: String){
    PAYMENT("payment"),
    LOGIN("login"),
    SECURITY_VERIFICATION("security_verification"),
    PERMISSION_CONSENT("permission_consent"),
    ITEM_SELECTION("item_selection"),
    CLARIFICATION_MISMATCH("clarification_mismatch"),
    SPECIFICATION_SELECTION("specification_selection"),
}

enum class OperatorType(val keyEvent: String) {
    HOME("HOME"),
    BACK("BACK"),
    MENU("MENU"),
    ENTER("ENTER"),
    APPSELECT("APPSELECT"),
    CLEAR("CLEAR"),
    POWER("power"),
    VOLUME_UP("volume_up"),
    VOLUME_DOWN("volume_down"),
    VOLUME_MUTE("volume_mute");
}

enum class ScrollDirection(val alias: String) {
    UP("up"),
    DOWN("down"),
    LEFT("left"),
    RIGHT("right");
}