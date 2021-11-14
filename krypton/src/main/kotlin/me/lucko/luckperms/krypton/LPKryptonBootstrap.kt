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

import com.google.inject.Inject
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap
import me.lucko.luckperms.common.plugin.logging.Log4jPluginLogger
import net.luckperms.api.platform.Platform
import org.apache.logging.log4j.Logger
import org.kryptonmc.api.Server
import org.kryptonmc.api.event.Listener
import org.kryptonmc.api.event.ListenerPriority
import org.kryptonmc.api.event.server.ServerStartEvent
import org.kryptonmc.api.event.server.ServerStopEvent
import org.kryptonmc.api.plugin.PluginDescription
import org.kryptonmc.api.plugin.annotation.DataFolder
import org.kryptonmc.api.plugin.annotation.Plugin
import java.nio.file.Path
import java.time.Instant
import java.util.Optional
import java.util.UUID
import java.util.concurrent.CountDownLatch

/**
 * Bootstrap plugin for LuckPerms running on Krypton.
 */
@Plugin("luckperms", "LuckPerms", "@version@", "A permissions plugin", ["Luck"])
class LPKryptonBootstrap @Inject constructor(
    val server: Server,
    logger: Logger,
    @DataFolder folder: Path,
    private val description: PluginDescription,
) : LuckPermsBootstrap {

    private val folder = folder.toAbsolutePath()
    private val plugin = LPKryptonPlugin(this)
    private val logger = Log4jPluginLogger(logger)
    private val schedulerAdapter = KryptonSchedulerAdapter(this, server.scheduler)
    private val classPathAppender = KryptonClassPathAppender(this)

    private val loadLatch = CountDownLatch(1)
    private val enableLatch = CountDownLatch(1)

    private lateinit var startTime: Instant

    @Listener(ListenerPriority.MAXIMUM)
    fun onStart(event: ServerStartEvent) {
        startTime = Instant.now()
        try {
            plugin.load()
        } finally {
            loadLatch.countDown()
        }

        try {
            plugin.enable()
        } finally {
            enableLatch.countDown()
        }
    }

    @Listener(ListenerPriority.NONE)
    fun onStop(event: ServerStopEvent) {
        plugin.disable()
    }

    override fun getPluginLogger() = logger

    override fun getScheduler() = schedulerAdapter

    override fun getClassPathAppender() = classPathAppender

    override fun getLoadLatch() = loadLatch

    override fun getEnableLatch() = enableLatch

    override fun getVersion() = description.version

    override fun getStartupTime() = startTime

    override fun getType() = Platform.Type.KRYPTON

    override fun getServerBrand() = server.platform.name

    override fun getServerVersion() = "${server.platform.version} (for Minecraft ${server.platform.minecraftVersion})"

    override fun getDataDirectory(): Path = folder

    override fun getPlayer(uniqueId: UUID) = Optional.ofNullable(server.player(uniqueId))

    override fun lookupUniqueId(username: String) = Optional.ofNullable(server.player(username)?.uuid)

    override fun lookupUsername(uniqueId: UUID) = Optional.ofNullable(server.player(uniqueId)?.profile?.name)

    override fun getPlayerCount() = server.players.size

    override fun getPlayerList() = server.players.map { it.profile.name }

    override fun getOnlinePlayers() = server.players.map { it.uuid }

    override fun isPlayerOnline(uniqueId: UUID) = server.player(uniqueId) != null
}
