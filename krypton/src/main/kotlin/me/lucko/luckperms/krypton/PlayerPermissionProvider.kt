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

import me.lucko.luckperms.common.context.QueryOptionsSupplier
import me.lucko.luckperms.common.model.User
import org.kryptonmc.api.entity.player.Player
import org.kryptonmc.api.permission.PermissionFunction
import org.kryptonmc.api.permission.PermissionProvider
import org.kryptonmc.api.permission.Subject

class PlayerPermissionProvider(
    private val player: Player,
    private val user: User,
    private val queryOptionsSupplier: QueryOptionsSupplier
) : PermissionProvider, PermissionFunction {

    override fun createFunction(subject: Subject) = apply {
        check(subject === player) { "createFunction called with a different argument!" }
    }

    override fun get(permission: String) = user.cachedData.getPermissionData(queryOptionsSupplier.queryOptions).checkPermission(permission).toTriState()
}
