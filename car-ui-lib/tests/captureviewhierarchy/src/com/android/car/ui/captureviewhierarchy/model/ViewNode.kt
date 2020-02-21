/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.car.ui.captureviewhierarchy.model

import androidx.constraintlayout.solver.widgets.Rectangle
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import java.util.Collections
import java.util.Enumeration
import java.util.LinkedList

/**
 * Represents an Android View object. Holds properties and a previewBox that contains the display
 * area of the object on screen. Created by parsing view dumps using
 * [com.android.car.ui.captureviewhierarchy.parser.ViewNodeParser].
 */
data class ViewNode internal constructor(private val parent: ViewNode?, val name: String, val hash: String){
    // If the force state is set, the preview tries to render/hide the view
    // (depending on the parent's state)
    enum class ForcedState {
        NONE,
        VISIBLE,
        INVISIBLE
    }

    val groupedProperties: MutableMap<String, MutableList<ViewProperty>> = Maps.newHashMap()
    val namedProperties: MutableMap<String, ViewProperty> = Maps.newHashMap()
    val properties: MutableList<ViewProperty> = Lists.newArrayList()
    val children: MutableList<ViewNode> = Lists.newArrayList()
    val previewBox: Rectangle = Rectangle()

    // default in case properties are not available
    var index: Int = 0
    var id: String? = null

    lateinit var displayInfo: DisplayInfo

    var isParentVisible: Boolean = false
        private set
    var isDrawn: Boolean = false
        private set
    var forcedState: ForcedState = ForcedState.NONE

    fun addPropertyToGroup(property: ViewProperty, groupKey: String) {
        val propertiesList = groupedProperties.getOrDefault(
          groupKey,
            LinkedList()
        )
        propertiesList.add(property)
        groupedProperties[groupKey] = propertiesList
    }

    fun getProperty(name: String, vararg altNames: String): ViewProperty? {
        var property: ViewProperty? = namedProperties[name]
        var i = 0
        while (property == null && i < altNames.size) {
            property = namedProperties[altNames[i]]
            i++
        }
        return property
    }

    /** Recursively updates all the visibility parameter of the nodes.  */
    fun updateNodeDrawn() {
        updateNodeDrawn(isParentVisible)
    }

    fun updateNodeDrawn(parentVisible: Boolean) {
        var parentVisible = parentVisible
        isParentVisible = parentVisible
        if (forcedState == ForcedState.NONE) {
            isDrawn = !displayInfo.willNotDraw && parentVisible && displayInfo.isVisible
            parentVisible = parentVisible && displayInfo.isVisible
        } else {
            isDrawn = forcedState == ForcedState.VISIBLE && parentVisible
            parentVisible = isDrawn
        }
        for (child in children) {
            child.updateNodeDrawn(parentVisible)
            isDrawn = isDrawn or (child.isDrawn && child.displayInfo.isVisible)
        }
    }

    override fun toString(): String {
        return "$name@$hash"
    }

     fun getChildAt(childIndex: Int): ViewNode {
        return children[childIndex]
    }

     fun getChildCount(): Int {
        return children.size
    }

     fun getParent(): ViewNode? {
        return parent
    }

     fun getIndex(node: ViewNode): Int {
        return children.indexOf(node as ViewNode)
    }

     fun getAllowsChildren(): Boolean {
        return true
    }

     fun isLeaf(): Boolean {
        return getChildCount() == 0
    }

     fun children(): Enumeration<*> {
        return Collections.enumeration(children)
    }
}
