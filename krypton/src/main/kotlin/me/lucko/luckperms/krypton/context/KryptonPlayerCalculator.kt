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

import me.lucko.luckperms.common.config.ConfigKeys
import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl
import me.lucko.luckperms.krypton.LPKryptonPlugin
import net.luckperms.api.context.Context
import net.luckperms.api.context.ContextCalculator
import net.luckperms.api.context.ContextConsumer
import net.luckperms.api.context.ContextSet
import net.luckperms.api.context.DefaultContextKeys
import org.kryptonmc.api.entity.player.Player
import org.kryptonmc.api.registry.Registries

// TODO: Add event listening for gamemode and world changes and joining worlds to signal context updates
//  when Krypton supports this
class KryptonPlayerCalculator(
    private val plugin: LPKryptonPlugin,
    disabled: Set<String>
) : ContextCalculator<Player> {

    private val gamemode = DefaultContextKeys.GAMEMODE_KEY !in disabled
    private val world = DefaultContextKeys.WORLD_KEY !in disabled
    private val dimensionType = DefaultContextKeys.DIMENSION_TYPE_KEY !in disabled

    override fun calculate(target: Player, consumer: ContextConsumer) {
        if (gamemode) consumer.accept(DefaultContextKeys.GAMEMODE_KEY, target.gameMode.key().value())
        val world = target.world
        val dimension = world.dimensionType
        if (dimensionType) consumer.accept(DefaultContextKeys.DIMENSION_TYPE_KEY, dimension.key().value())
        if (this.world) plugin.configuration[ConfigKeys.WORLD_REWRITES].rewriteAndSubmit(world.name, consumer)
    }

    override fun estimatePotentialContexts(): ContextSet {
        val builder = ImmutableContextSetImpl.BuilderImpl()
        if (gamemode) Registries.GAME_MODES.values.forEach {
            builder.add(DefaultContextKeys.GAMEMODE_KEY, it.key().value())
        }
        if (dimensionType) Registries.DIMENSION_TYPE.values.forEach {
            builder.add(DefaultContextKeys.DIMENSION_TYPE_KEY, it.key().asString().removePrefix("minecraft:"))
        }
        if (world) plugin.bootstrap.server.worldManager.worlds.values.forEach {
            val name = it.name
            if (Context.isValidValue(name)) builder.add(DefaultContextKeys.WORLD_KEY, name)
        }
        return builder.build()
    }
}
