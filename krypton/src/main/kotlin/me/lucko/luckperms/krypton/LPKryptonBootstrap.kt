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
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender
import me.lucko.luckperms.common.plugin.logging.Log4jPluginLogger
import me.lucko.luckperms.common.plugin.logging.PluginLogger
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter
import net.luckperms.api.platform.Platform
import org.apache.logging.log4j.Logger
import org.kryptonmc.api.Server
import org.kryptonmc.api.entity.player.Player
import org.kryptonmc.api.event.Event
import org.kryptonmc.api.event.EventNode
import org.kryptonmc.api.event.Listener
import org.kryptonmc.api.event.server.ServerStartEvent
import org.kryptonmc.api.event.server.ServerStopEvent
import org.kryptonmc.api.plugin.PluginContainer
import org.kryptonmc.api.plugin.annotation.DataFolder
import java.nio.file.Path
import java.time.Instant
import java.util.Optional
import java.util.UUID
import java.util.concurrent.CountDownLatch

/**
 * Bootstrap plugin for LuckPerms running on Krypton.
 */
class LPKryptonBootstrap @Inject constructor(
    val server: Server,
    logger: Logger,
    val eventNode: EventNode<Event>,
    @DataFolder folder: Path,
    val container: PluginContainer,
) : LuckPermsBootstrap {

    private val folder = folder.toAbsolutePath()
    private val plugin = LPKryptonPlugin(this)
    private val logger = Log4jPluginLogger(logger)
    private val schedulerAdapter = KryptonSchedulerAdapter(this, server.scheduler)
    private val classPathAppender = KryptonClassPathAppender(this)

    private val loadLatch = CountDownLatch(1)
    private val enableLatch = CountDownLatch(1)

    private lateinit var startTime: Instant

    @Listener
    fun onStart(event: ServerStartEvent) {
        try {
            plugin.load()
        } finally {
            loadLatch.countDown()
        }

        startTime = Instant.now()
        try {
            plugin.enable()
        } finally {
            enableLatch.countDown()
        }
    }

    @Listener
    fun onStop(event: ServerStopEvent) {
        plugin.disable()
    }

    override fun getPluginLogger(): PluginLogger = logger

    override fun getScheduler(): SchedulerAdapter = schedulerAdapter

    override fun getClassPathAppender(): ClassPathAppender = classPathAppender

    override fun getLoadLatch(): CountDownLatch = loadLatch

    override fun getEnableLatch(): CountDownLatch = enableLatch

    override fun getVersion(): String = container.description.version

    override fun getStartupTime(): Instant = startTime

    override fun getType(): Platform.Type = Platform.Type.KRYPTON

    override fun getServerBrand(): String = server.platform.name

    override fun getServerVersion(): String = "${server.platform.version} (for Minecraft ${server.platform.minecraftVersion})"

    override fun getDataDirectory(): Path = folder

    override fun getPlayer(uniqueId: UUID): Optional<Player> = Optional.ofNullable(server.getPlayer(uniqueId))

    override fun lookupUniqueId(username: String): Optional<UUID> = Optional.ofNullable(server.getPlayer(username)?.uuid)

    override fun lookupUsername(uniqueId: UUID): Optional<String> = Optional.ofNullable(server.getPlayer(uniqueId)?.profile?.name)

    override fun getPlayerCount(): Int = server.players.size

    override fun getPlayerList(): Collection<String> = server.players.map { it.profile.name }

    override fun getOnlinePlayers(): Collection<UUID> = server.players.map { it.uuid }

    override fun isPlayerOnline(uniqueId: UUID): Boolean = server.getPlayer(uniqueId) != null
}
