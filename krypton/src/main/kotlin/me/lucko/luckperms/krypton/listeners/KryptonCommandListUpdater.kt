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

import com.github.benmanes.caffeine.cache.LoadingCache
import me.lucko.luckperms.common.cache.BufferedRequest
import me.lucko.luckperms.common.event.LuckPermsEventListener
import me.lucko.luckperms.common.util.CaffeineFactory
import me.lucko.luckperms.krypton.LPKryptonBootstrap
import net.luckperms.api.event.EventBus
import net.luckperms.api.event.context.ContextUpdateEvent
import net.luckperms.api.event.user.UserDataRecalculateEvent
import org.kryptonmc.api.entity.player.Player
import java.util.UUID
import java.util.concurrent.TimeUnit

class KryptonCommandListUpdater(private val bootstrap: LPKryptonBootstrap) : LuckPermsEventListener {

    private val sendingBuffers: LoadingCache<UUID, SendBuffer> = CaffeineFactory.newBuilder()
        .expireAfterAccess(10, TimeUnit.SECONDS)
        .build(::SendBuffer)

    override fun bind(bus: EventBus) {
        bus.subscribe(UserDataRecalculateEvent::class.java) { requestUpdate(it.user.uniqueId) }
        bus.subscribe(ContextUpdateEvent::class.java) { event -> event.getSubject(Player::class.java).ifPresent { requestUpdate(it.uuid) } }
    }

    private fun requestUpdate(uuid: UUID) {
        if (!bootstrap.isPlayerOnline(uuid)) return

        // Buffer the request to send a commands update.
        sendingBuffers.get(uuid)!!.request()
    }

    private fun sendUpdate(uuid: UUID) {
        bootstrap.scheduler.executeAsync { bootstrap.getPlayer(uuid).ifPresent { bootstrap.server.commandManager.updateCommands(it) } }
    }

    private inner class SendBuffer(private val uuid: UUID) : BufferedRequest<Unit>(500, TimeUnit.MILLISECONDS, bootstrap.scheduler) {

        override fun perform() {
            sendUpdate(uuid)
        }
    }
}
