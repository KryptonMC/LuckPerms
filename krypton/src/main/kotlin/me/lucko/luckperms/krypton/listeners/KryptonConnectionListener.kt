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

package me.lucko.luckperms.krypton.listeners

import me.lucko.luckperms.common.config.ConfigKeys
import me.lucko.luckperms.common.locale.Message
import me.lucko.luckperms.common.locale.TranslationManager
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener
import me.lucko.luckperms.krypton.LPKryptonPlugin
import me.lucko.luckperms.krypton.PlayerPermissionProvider
import org.kryptonmc.api.entity.player.Player
import org.kryptonmc.api.event.Listener
import org.kryptonmc.api.event.ListenerPriority
import org.kryptonmc.api.event.player.JoinEvent
import org.kryptonmc.api.event.player.JoinResult
import org.kryptonmc.api.event.player.QuitEvent
import org.kryptonmc.api.event.server.SetupPermissionsEvent
import java.util.Collections
import java.util.UUID
import java.util.concurrent.TimeUnit

class KryptonConnectionListener(private val plugin: LPKryptonPlugin) : AbstractConnectionListener(plugin) {

    private val deniedLogin = Collections.synchronizedSet(HashSet<UUID>())

    @Listener
    fun onPlayerPermissionsSetup(event: SetupPermissionsEvent) {
        if (event.subject !is Player) return
        val player = event.subject as Player
        if (plugin.configuration[ConfigKeys.DEBUG_LOGINS]) {
            plugin.logger.info("Processing pre-login for ${player.uuid} - ${player.profile.name}")
        }

        try {
            val user = loadUser(player.uuid, player.profile.name)
            recordConnection(player.uuid)
            event.provider = PlayerPermissionProvider(player, user, plugin.contextManager.getCacheFor(player))
            plugin.eventDispatcher.dispatchPlayerLoginProcess(player.uuid, player.profile.name, user)
        } catch (exception: Exception) {
            plugin.logger.severe("Exception occurred whilst loading data for ${player.uuid} - ${player.profile.name}", exception)
            if (plugin.configuration[ConfigKeys.CANCEL_FAILED_LOGINS]) deniedLogin.add(player.uuid)
            plugin.eventDispatcher.dispatchPlayerLoginProcess(player.uuid, player.profile.name, null)
        }
    }

    @Listener(ListenerPriority.MAXIMUM)
    fun onJoin(event: JoinEvent) {
        if (!deniedLogin.remove(event.player.uuid)) return
        event.result = JoinResult.denied(TranslationManager.render(Message.LOADING_DATABASE_ERROR.build()))
    }

    @Listener
    fun onNormalJoin(event: JoinEvent) {
        val player = event.player
        val user = plugin.userManager.getIfLoaded(player.uuid)
        if (plugin.configuration.get(ConfigKeys.DEBUG_LOGINS)) plugin.logger.info("Processing join for ${player.uuid} - ${player.name}")
        if (!event.result.isAllowed) return

        if (user == null) {
            if (player.uuid !in uniqueConnections) {
                plugin.logger.warn("User ${player.uuid} - ${player.name} doesn't have any data pre-loaded, they have never been processed " +
                    "during login in this session. Denying login.")
            } else {
                plugin.logger.warn("User ${player.uuid} - ${player.name} doesn't currently have any data pre-loaded, but they have been " +
                    "processed before in this session. Denying login.")
            }

            if (plugin.configuration[ConfigKeys.CANCEL_FAILED_LOGINS]) {
                event.result = JoinResult.denied(TranslationManager.render(Message.LOADING_STATE_ERROR.build()))
            } else {
                plugin.bootstrap.scheduler.asyncLater({
                    Message.LOADING_STATE_ERROR.send(plugin.senderFactory.wrap(player))
                }, 1, TimeUnit.SECONDS)
            }
        }
    }

    // Sit at the back of the queue so other plugins can still perform permission checks on this event
    @Listener(ListenerPriority.NONE)
    fun onQuit(event: QuitEvent) {
        handleDisconnect(event.player.uuid)
    }
}
