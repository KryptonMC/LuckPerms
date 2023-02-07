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
import org.kryptonmc.api.event.player.JoinEvent
import org.kryptonmc.api.event.player.QuitEvent
import org.kryptonmc.api.event.server.SetupPermissionsEvent
import java.util.Collections
import java.util.UUID
import java.util.concurrent.TimeUnit

class KryptonConnectionListener(private val plugin: LPKryptonPlugin) : AbstractConnectionListener(plugin) {

    private val deniedLogin = Collections.synchronizedSet(HashSet<UUID>())

    @Listener
    fun onPlayerPermissionsSetup(event: SetupPermissionsEvent) {
        // Only accept players in here, as we only override checks for players. Everything else will be handled by
        // the MonitoringPermissionCheckListener.
        if (event.subject !is Player) return
        val player = event.subject as Player

        if (plugin.configuration.get(ConfigKeys.DEBUG_LOGINS)) {
            plugin.logger.info("Processing pre-login for ${player.uuid} - ${player.profile.name}")
        }

        /*
          Actually process the login for the connection.
          We do this here to delay the login until the data is ready.
          If the login gets cancelled later on, then this will be cleaned up.

          This includes:
          - loading uuid data
          - loading permissions
          - creating a user instance in the UserManager for this connection.
          - setting up cached data.
         */
        try {
            val user = loadUser(player.uuid, player.profile.name)
            recordConnection(player.uuid)
            event.provider = PlayerPermissionProvider(player, user, plugin.contextManager.getCacheFor(player))
            plugin.eventDispatcher.dispatchPlayerLoginProcess(player.uuid, player.profile.name, user)
        } catch (exception: Exception) {
            plugin.logger.severe("Exception occurred whilst loading data for ${player.uuid} - ${player.profile.name}", exception)

            // there was sme error loading
            if (plugin.configuration.get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                // cancel the login attempt
                deniedLogin.add(player.uuid)
            }
            plugin.eventDispatcher.dispatchPlayerLoginProcess(player.uuid, player.profile.name, null)
        }
    }

    @Listener
    fun onJoin(event: JoinEvent) {
        val player = event.player
        if (!deniedLogin.remove(player.uuid)) return
        event.denyWithResult(JoinEvent.Result(TranslationManager.render(Message.LOADING_DATABASE_ERROR.build(), player.settings.locale), false))
    }

    @Listener
    fun onNormalJoin(event: JoinEvent) {
        val player = event.player
        val user = plugin.userManager.getIfLoaded(player.uuid)

        val formattedInfo = "${player.uuid} - ${player.profile.name}"
        if (plugin.configuration.get(ConfigKeys.DEBUG_LOGINS)) {
            plugin.logger.info("Processing join for $formattedInfo")
        }
        if (!event.isAllowed()) return

        if (user == null) {
            if (!uniqueConnections.contains(player.uuid)) {
                plugin.logger.warn("User $formattedInfo doesn't have any data pre-loaded, they have never been processed during login in this " +
                    "session. Denying login.")
            } else {
                plugin.logger.warn("User $formattedInfo doesn't currently have any data pre-loaded, but they have been processed before in this " +
                    "session. Denying login.")
            }

            if (plugin.configuration.get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                // disconnect the user
                event.denyWithResult(JoinEvent.Result(TranslationManager.render(Message.LOADING_STATE_ERROR.build(), player.settings.locale), false))
            } else {
                // just send a message
                plugin.bootstrap.scheduler.asyncLater({ Message.LOADING_STATE_ERROR.send(plugin.senderFactory.wrap(player)) }, 1, TimeUnit.SECONDS)
            }
        }
    }

    // Sit at the back of the queue so other plugins can still perform permission checks on this event
    @Listener
    fun onQuit(event: QuitEvent) {
        handleDisconnect(event.player.uuid)
    }
}
