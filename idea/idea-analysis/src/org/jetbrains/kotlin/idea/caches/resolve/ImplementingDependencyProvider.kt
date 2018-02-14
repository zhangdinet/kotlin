/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.psi.util.CachedValueProvider
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.resolve.TargetPlatform
import java.util.*

class ImplementingDependencyProvider(
    private val commonInfo: ModuleInfo,
    private val platform: TargetPlatform
) : CachedValueProvider<ModuleInfo> {

    internal var baseInfo: ModuleInfo? = null

    override fun compute(): CachedValueProvider.Result<ModuleInfo>? {
        val baseInfo = baseInfo ?: return null
        val queue: Queue<ModuleInfo> = LinkedList()
        val visited = mutableSetOf<ModuleInfo>()
        queue += baseInfo
        while (queue.isNotEmpty()) {
            val current = queue.poll()
            if (commonInfo == current.expectedBy) return CachedValueProvider.Result(current, commonInfo, baseInfo)
            for (dependency in current.dependencies()) {
                if (dependency in visited || dependency.platform != platform) continue
                visited += dependency
                queue += dependency
            }
        }
        return null
    }
}