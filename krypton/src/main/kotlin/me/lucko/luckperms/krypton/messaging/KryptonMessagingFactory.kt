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

package me.lucko.luckperms.krypton.messaging

import me.lucko.luckperms.common.messaging.InternalMessagingService
import me.lucko.luckperms.common.messaging.LuckPermsMessagingService
import me.lucko.luckperms.common.messaging.MessagingFactory
import me.lucko.luckperms.krypton.LPKryptonPlugin
import net.luckperms.api.messenger.IncomingMessageConsumer
import net.luckperms.api.messenger.Messenger
import net.luckperms.api.messenger.MessengerProvider

class KryptonMessagingFactory(plugin: LPKryptonPlugin) : MessagingFactory<LPKryptonPlugin>(plugin) {

    override fun getServiceFor(messagingType: String?): InternalMessagingService {
        if (messagingType == "pluginmsg" || messagingType == "bungee" || messagingType == "velocity") {
            try {
                return LuckPermsMessagingService(plugin, PluginMessageMessengerProvider())
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
        return super.getServiceFor(messagingType)
    }

    private inner class PluginMessageMessengerProvider : MessengerProvider {

        override fun getName(): String = "PluginMessage"

        override fun obtain(consumer: IncomingMessageConsumer): Messenger = PluginMessageMessenger(plugin, consumer)
    }
}
