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

import net.kyori.adventure.util.TriState
import net.luckperms.api.event.EventBus
import net.luckperms.api.event.EventSubscription
import net.luckperms.api.event.LuckPermsEvent
import net.luckperms.api.event.context.ContextUpdateEvent
import net.luckperms.api.util.Tristate
import java.util.Optional

fun TriState.toTristate() = when (this) {
    TriState.TRUE -> Tristate.TRUE
    TriState.FALSE -> Tristate.FALSE
    TriState.NOT_SET -> Tristate.UNDEFINED
}

fun Tristate.toTriState() = when (this) {
    Tristate.TRUE -> TriState.TRUE
    Tristate.FALSE -> TriState.FALSE
    Tristate.UNDEFINED -> TriState.NOT_SET
}

inline fun <reified T : LuckPermsEvent> EventBus.subscribe(noinline handler: (T) -> Unit): EventSubscription<T> = subscribe(T::class.java, handler)

inline fun <reified T> ContextUpdateEvent.subject(): Optional<T> = getSubject(T::class.java)
