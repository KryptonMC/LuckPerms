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

import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter
import me.lucko.luckperms.common.plugin.LuckPermsPlugin
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.kotlin.extensions.getList
import java.nio.file.Path

class KryptonConfigAdapter(
    private val plugin: LPKryptonPlugin,
    private val path: Path
) : ConfigurationAdapter {

    private var root = load()

    override fun getPlugin(): LuckPermsPlugin = plugin

    override fun reload() {
        root = load()
    }

    override fun getString(path: String, def: String?): String? {
        val resolved = path.resolve()
        if (def != null) return resolved.getString(def)
        return resolved.string
    }

    override fun getInteger(path: String, def: Int): Int = path.resolve().getInt(def)

    override fun getBoolean(path: String, def: Boolean): Boolean = path.resolve().getBoolean(def)

    override fun getStringList(path: String, def: List<String>): List<String> {
        val node = path.resolve()
        if (node.virtual() || !node.isList) return def
        return node.getList(String::class, def)
    }

    override fun getStringMap(path: String, def: MutableMap<String, String>): MutableMap<String, String> {
        val node = path.resolve()
        if (node.virtual()) return def
        return node.get(def)
    }

    private fun String.resolve(): ConfigurationNode = root.node(split('.'))

    private fun load(): CommentedConfigurationNode {
        return try {
            HoconConfigurationLoader.builder().path(path).build().load()
        } catch (exception: Exception) {
            throw RuntimeException(exception)
        }
    }
}
