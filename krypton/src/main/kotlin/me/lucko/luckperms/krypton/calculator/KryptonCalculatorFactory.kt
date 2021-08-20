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

package me.lucko.luckperms.krypton.calculator

import me.lucko.luckperms.common.cacheddata.CacheMetadata
import me.lucko.luckperms.common.calculator.CalculatorFactory
import me.lucko.luckperms.common.calculator.PermissionCalculator
import me.lucko.luckperms.common.calculator.processor.DirectProcessor
import me.lucko.luckperms.common.calculator.processor.PermissionProcessor
import me.lucko.luckperms.common.calculator.processor.RegexProcessor
import me.lucko.luckperms.common.calculator.processor.SpongeWildcardProcessor
import me.lucko.luckperms.common.calculator.processor.WildcardProcessor
import me.lucko.luckperms.common.config.ConfigKeys
import me.lucko.luckperms.krypton.LPKryptonPlugin
import me.lucko.luckperms.krypton.context.KryptonContextManager
import net.luckperms.api.query.QueryOptions

class KryptonCalculatorFactory(private val plugin: LPKryptonPlugin) : CalculatorFactory {

    override fun build(queryOptions: QueryOptions, metadata: CacheMetadata): PermissionCalculator {
        val processors = ArrayList<PermissionProcessor>(4).apply { add(DirectProcessor()) }
        if (plugin.configuration[ConfigKeys.APPLYING_REGEX]) processors.add(RegexProcessor())
        if (plugin.configuration[ConfigKeys.APPLYING_WILDCARDS]) processors.add(WildcardProcessor())
        if (plugin.configuration[ConfigKeys.APPLYING_WILDCARDS_SPONGE]) processors.add(SpongeWildcardProcessor())

        val op = queryOptions.option(KryptonContextManager.OPERATOR_OPTION).orElse(false)
        if (op) processors.add(OperatorProcessor)

        return PermissionCalculator(plugin, metadata, processors)
    }
}
