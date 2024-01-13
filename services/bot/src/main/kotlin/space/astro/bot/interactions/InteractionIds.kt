package space.astro.bot.interactions

import com.aventrix.jnanoid.jnanoid.NanoIdUtils

object InteractionIds {

    object Menu {
        const val VC_NAME = "menu/vc/name"
        const val VC_LIMIT = "menu/vc/limit"
        const val VC_BITRATE = "menu/vc/bitrate"
        const val VC_REGION = "menu/vc/region"
        const val VC_TEMPLATE = "menu/vc/template"

        const val VC_UNLOCK = "menu/vc/unlock"
        const val VC_LOCK = "menu/vc/lock"
        const val VC_HIDE = "menu/vc/hide"
        const val VC_UNHIDE = "menu/vc/unhide"
        const val VC_BAN = "menu/vc/ban"
        const val VC_PERMIT = "menu/vc/permit"
        const val VC_INVITE = "menu/vc/invite"
        const val VC_RESET = "menu/vc/reset"

        const val VC_CLAIM = "menu/vc/claim"
        const val VC_TRANSFER = "menu/vc/transfer"

        const val VC_CHAT = "menu/vc/chat"
        const val VC_LOGS = "menu/vc/logs"

        const val VC_WAITING_ROOM = "menu/vc/waiting-room"
    }

    object Button {
        const val VC_NAME = "button/vc/name"
        const val VC_LIMIT = "button/vc/limit"
        const val VC_BITRATE = "button/vc/bitrate"
        const val VC_REGION = "button/vc/region"
        const val VC_TEMPLATE = "button/vc/template"

        const val VC_UNLOCK = "button/vc/unlock"
        const val VC_LOCK = "button/vc/lock"
        const val VC_HIDE = "button/vc/hide"
        const val VC_UNHIDE = "button/vc/unhide"
        const val VC_BAN = "button/vc/ban"
        const val VC_PERMIT = "button/vc/permit"
        const val VC_INVITE = "button/vc/invite"
        const val VC_RESET = "button/vc/reset"

        const val VC_CLAIM = "button/vc/claim"
        const val VC_TRANSFER = "button/vc/transfer"

        const val VC_CHAT = "button/vc/chat"
        const val VC_LOGS = "button/vc/logs"

        const val VC_WAITING_ROOM = "button/vc/waiting-room"
    }

    object Modal {
        const val VC_NAME = "modal/vc/name"
        const val VC_BITRATE = "modal/vc/bitrate"
        const val VC_LIMIT = "modal/vc/limit"
    }

    fun getRandom(): String = NanoIdUtils.randomNanoId()
}