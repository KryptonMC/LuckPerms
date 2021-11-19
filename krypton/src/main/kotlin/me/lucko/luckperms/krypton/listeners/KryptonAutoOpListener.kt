package me.lucko.luckperms.krypton.listeners

import me.lucko.luckperms.common.api.implementation.ApiUser
import me.lucko.luckperms.common.event.LuckPermsEventListener
import me.lucko.luckperms.krypton.LPKryptonPlugin
import net.luckperms.api.event.EventBus
import net.luckperms.api.event.context.ContextUpdateEvent
import net.luckperms.api.event.user.UserDataRecalculateEvent
import org.kryptonmc.api.entity.player.Player

/**
 * Implements the LuckPerms auto op feature.
 */
class KryptonAutoOpListener(private val plugin: LPKryptonPlugin) : LuckPermsEventListener {

    override fun bind(bus: EventBus) {
        bus.subscribe(UserDataRecalculateEvent::class.java, ::onUserDataRecalculate)
        bus.subscribe(ContextUpdateEvent::class.java, ::onContextUpdate)
    }

    private fun onUserDataRecalculate(event: UserDataRecalculateEvent) {
        val user = ApiUser.cast(event.user)
        plugin.bootstrap.getPlayer(user.uniqueId).ifPresent(::refreshAutoOp)
    }

    private fun onContextUpdate(event: ContextUpdateEvent) {
        event.getSubject(Player::class.java).ifPresent(::refreshAutoOp)
    }

    private fun refreshAutoOp(player: Player) {
        val user = plugin.userManager.getIfLoaded(player.uuid)
        val value = if (user != null) {
            val queryOptions = plugin.contextManager.getQueryOptions(player)
            val permissionData = user.cachedData.getPermissionData(queryOptions).permissionMap
            permissionData.getOrDefault(NODE, false)
        } else {
            false
        }
        player.isOperator = value
    }

    companion object {

        private const val NODE = "luckperms.autoop"
    }
}