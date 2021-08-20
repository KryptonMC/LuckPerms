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

package me.lucko.luckperms.krypton.context

import com.github.benmanes.caffeine.cache.LoadingCache
import me.lucko.luckperms.common.config.ConfigKeys
import me.lucko.luckperms.common.context.ContextManager
import me.lucko.luckperms.common.context.QueryOptionsCache
import me.lucko.luckperms.common.context.QueryOptionsSupplier
import me.lucko.luckperms.common.util.CaffeineFactory
import me.lucko.luckperms.krypton.LPKryptonPlugin
import net.luckperms.api.context.ImmutableContextSet
import net.luckperms.api.query.OptionKey
import net.luckperms.api.query.QueryOptions
import org.kryptonmc.api.entity.player.Player
import java.util.concurrent.TimeUnit

class KryptonContextManager(plugin: LPKryptonPlugin) : ContextManager<Player, Player>(plugin, Player::class.java, Player::class.java) {

    private val subjectCaches: LoadingCache<Player, QueryOptionsCache<Player>> = CaffeineFactory.newBuilder()
        .expireAfterAccess(1, TimeUnit.MINUTES)
        .build { QueryOptionsCache(it, this) }

    override fun getUniqueId(player: Player) = player.uuid

    override fun getCacheFor(subject: Player) = subjectCaches[subject]!!

    override fun invalidateCache(subject: Player) {
        subjectCaches.getIfPresent(subject)?.invalidate()
    }

    override fun formQueryOptions(subject: Player, contextSet: ImmutableContextSet): QueryOptions {
        val options = plugin.configuration[ConfigKeys.GLOBAL_QUERY_OPTIONS].toBuilder()
        if (subject.permissionLevel > 0) options.option(OPERATOR_OPTION, true)
        return options.context(contextSet).build()
    }

    companion object {

        val OPERATOR_OPTION = OptionKey.of("op", Boolean::class.java)
    }
}
