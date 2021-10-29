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

import me.lucko.luckperms.common.calculator.result.TristateResult
import me.lucko.luckperms.common.query.QueryOptionsImpl
import me.lucko.luckperms.common.verbose.VerboseCheckTarget
import me.lucko.luckperms.common.verbose.event.PermissionCheckEvent
import me.lucko.luckperms.krypton.LPKryptonPlugin
import me.lucko.luckperms.krypton.toTristate
import net.kyori.adventure.util.TriState
import org.kryptonmc.api.entity.player.Player
import org.kryptonmc.api.event.Listener
import org.kryptonmc.api.event.ListenerPriority
import org.kryptonmc.api.event.server.SetupPermissionsEvent
import org.kryptonmc.api.permission.PermissionFunction
import org.kryptonmc.api.permission.PermissionProvider
import org.kryptonmc.api.permission.Subject

class MonitoringPermissionCheckListener(private val plugin: LPKryptonPlugin) {

    @Listener(ListenerPriority.NONE)
    fun onOtherPermissionSetup(event: SetupPermissionsEvent) {
        if (event.subject is Player) return
        event.provider = MonitoredPermissionProvider(event.provider)
    }

    private inner class MonitoredPermissionProvider(private val delegate: PermissionProvider) : PermissionProvider {

        override fun createFunction(subject: Subject) = MonitoredPermissionFunction(subject, delegate.createFunction(subject))
    }

    private inner class MonitoredPermissionFunction(subject: Subject, private val delegate: PermissionFunction) : PermissionFunction {

        private val verboseCheckTarget = VerboseCheckTarget.internal(determineName(subject))

        override fun get(permission: String): TriState {
            val setting = delegate[permission]
            val result = setting.toTristate()
            plugin.verboseHandler.offerPermissionCheckEvent(
                PermissionCheckEvent.Origin.PLATFORM_LOOKUP_CHECK,
                verboseCheckTarget,
                QueryOptionsImpl.DEFAULT_CONTEXTUAL,
                permission,
                TristateResult.of(result)
            )
            plugin.permissionRegistry.offer(permission)
            return setting
        }
    }

    private fun determineName(subject: Subject): String {
        if (subject === plugin.bootstrap.server.console) return "console"
        return subject.javaClass.simpleName
    }
}
