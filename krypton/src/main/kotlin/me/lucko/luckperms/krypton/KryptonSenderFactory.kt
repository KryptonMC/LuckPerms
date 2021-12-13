/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.krypton

import me.lucko.luckperms.common.locale.TranslationManager
import me.lucko.luckperms.common.sender.Sender as LPSender
import me.lucko.luckperms.common.sender.SenderFactory
import net.kyori.adventure.text.Component
import net.luckperms.api.util.Tristate
import org.kryptonmc.api.command.ConsoleSender
import org.kryptonmc.api.command.Sender
import org.kryptonmc.api.entity.player.Player
import java.util.UUID

class KryptonSenderFactory(private val plugin: LPKryptonPlugin) : SenderFactory<LPKryptonPlugin, Sender>(plugin) {

    override fun getName(sender: Sender): String {
        if (sender is Player) return sender.profile.name
        return LPSender.CONSOLE_NAME
    }

    override fun getUniqueId(sender: Sender): UUID {
        if (sender is Player) return sender.uuid
        return LPSender.CONSOLE_UUID
    }

    override fun sendMessage(sender: Sender, message: Component) {
        val locale = if (sender is Player) sender.locale else null
        sender.sendMessage(TranslationManager.render(message, locale))
    }

    override fun getPermissionValue(sender: Sender, node: String): Tristate = sender.getPermissionValue(node).toTristate()

    override fun hasPermission(sender: Sender, node: String): Boolean = sender.hasPermission(node)

    override fun performCommand(sender: Sender, command: String) {
        plugin.bootstrap.server.commandManager.dispatch(sender, command)
    }

    override fun isConsole(sender: Sender): Boolean = sender is ConsoleSender
}
