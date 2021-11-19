package me.lucko.luckperms.krypton.listeners

import me.lucko.luckperms.common.config.ConfigKeys
import me.lucko.luckperms.common.locale.Message
import me.lucko.luckperms.krypton.LPKryptonPlugin
import org.kryptonmc.api.event.Listener
import org.kryptonmc.api.event.command.CommandExecuteEvent
import org.kryptonmc.api.event.command.CommandResult

class KryptonPlatformListener(private val plugin: LPKryptonPlugin) {

    @Listener
    fun onCommand(event: CommandExecuteEvent) {
        if (event.command.isEmpty()) return
        if (plugin.configuration[ConfigKeys.OPS_ENABLED]) return
        if (OP_COMMAND_REGEX.matches(event.command)) {
            event.result = CommandResult.denied()
            Message.OP_DISABLED.send(plugin.senderFactory.wrap(event.sender))
        }
    }

    companion object {

        private val OP_COMMAND_REGEX = "^/?(\\w+:)?(deop|op)( .*)?$".toRegex()
    }
}