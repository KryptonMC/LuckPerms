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

import me.lucko.luckperms.common.api.LuckPermsApiProvider
import me.lucko.luckperms.common.calculator.CalculatorFactory
import me.lucko.luckperms.common.command.CommandManager
import me.lucko.luckperms.common.config.ConfigKeys
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter
import me.lucko.luckperms.common.dependencies.Dependency
import me.lucko.luckperms.common.event.AbstractEventBus
import me.lucko.luckperms.common.messaging.MessagingFactory
import me.lucko.luckperms.common.model.User
import me.lucko.luckperms.common.model.manager.group.StandardGroupManager
import me.lucko.luckperms.common.model.manager.track.StandardTrackManager
import me.lucko.luckperms.common.model.manager.user.StandardUserManager
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener
import me.lucko.luckperms.common.sender.Sender
import me.lucko.luckperms.krypton.calculator.KryptonCalculatorFactory
import me.lucko.luckperms.krypton.context.KryptonContextManager
import me.lucko.luckperms.krypton.context.KryptonPlayerCalculator
import me.lucko.luckperms.krypton.listeners.KryptonConnectionListener
import me.lucko.luckperms.krypton.listeners.MonitoringPermissionCheckListener
import me.lucko.luckperms.krypton.messaging.KryptonMessagingFactory
import net.luckperms.api.LuckPerms
import net.luckperms.api.query.QueryOptions
import org.kryptonmc.api.service.register
import java.util.Optional
import java.util.stream.Stream

class LPKryptonPlugin(private val bootstrap: LPKryptonBootstrap) : AbstractLuckPermsPlugin() {

    lateinit var senderFactory: KryptonSenderFactory
    private lateinit var connectionListener: KryptonConnectionListener
    private lateinit var commandManager: KryptonCommandExecutor
    private lateinit var userManager: StandardUserManager
    private lateinit var groupManager: StandardGroupManager
    private lateinit var trackManager: StandardTrackManager
    private lateinit var contextManager: KryptonContextManager

    override fun getBootstrap(): LPKryptonBootstrap = bootstrap

    override fun setupSenderFactory() {
        senderFactory = KryptonSenderFactory(this)
    }

    override fun getGlobalDependencies(): Set<Dependency> = super.getGlobalDependencies().apply {
        remove(Dependency.ADVENTURE) // We shade this, let's not put conflicting versions on to classpath.
    }

    override fun provideConfigurationAdapter(): ConfigurationAdapter = KryptonConfigAdapter(this, resolveConfig("config.conf"))

    override fun registerPlatformListeners() {
        connectionListener = KryptonConnectionListener(this)
        bootstrap.server.eventManager.register(bootstrap, connectionListener)
        bootstrap.server.eventManager.register(bootstrap, MonitoringPermissionCheckListener(this))
    }

    override fun provideMessagingFactory(): MessagingFactory<*> = KryptonMessagingFactory(this)

    override fun registerCommands() {
        commandManager = KryptonCommandExecutor(this)
        commandManager.register()
    }

    override fun setupManagers() {
        userManager = StandardUserManager(this)
        groupManager = StandardGroupManager(this)
        trackManager = StandardTrackManager(this)
    }

    override fun provideCalculatorFactory(): CalculatorFactory = KryptonCalculatorFactory(this)

    override fun setupContextManager() {
        contextManager = KryptonContextManager(this)
        val playerCalculator = KryptonPlayerCalculator(this, configuration[ConfigKeys.DISABLED_CONTEXTS])
        bootstrap.server.eventManager.register(bootstrap, playerCalculator)
        contextManager.registerCalculator(playerCalculator)
    }

    override fun setupPlatformHooks() {
        // got nothing to do here
    }

    override fun provideEventBus(apiProvider: LuckPermsApiProvider): AbstractEventBus<*> = KryptonEventBus(this, apiProvider)

    override fun registerApiOnPlatform(api: LuckPerms) {
        bootstrap.server.servicesManager.register(bootstrap.container, api)
    }

    override fun performFinalSetup() {
        // nothing to do here... yet
    }

    override fun getQueryOptionsForUser(user: User): Optional<QueryOptions> = bootstrap.getPlayer(user.uniqueId).map(contextManager::getQueryOptions)

    override fun getOnlineSenders(): Stream<Sender> = Stream.concat(
        Stream.of(consoleSender),
        bootstrap.server.players.stream().map(senderFactory::wrap)
    )

    override fun getConsoleSender(): Sender = senderFactory.wrap(bootstrap.server.console)

    override fun getConnectionListener(): AbstractConnectionListener = connectionListener

    override fun getCommandManager(): CommandManager = commandManager

    override fun getUserManager(): StandardUserManager = userManager

    override fun getGroupManager(): StandardGroupManager = groupManager

    override fun getTrackManager(): StandardTrackManager = trackManager

    override fun getContextManager(): KryptonContextManager = contextManager
}
