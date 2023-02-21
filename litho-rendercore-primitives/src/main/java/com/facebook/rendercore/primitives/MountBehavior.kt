/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.rendercore.primitives

import android.content.Context
import com.facebook.rendercore.ContentAllocator
import com.facebook.rendercore.MountDelegate
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.Systracer

/**
 * MountBehavior defines how to allocate a [View]/[Drawable] and apply properties to it.
 *
 * @property id Unique id identifying the [RenderUnit] in the tree of Node it is part of.
 * @property contentAllocator Provides a [View]/[Drawable] content.
 * @property mountConfigurationCall A function that allows for applying properties to the content.
 */
class MountBehavior<ContentType : Any>(
    private val id: Long,
    private val contentAllocator: ContentAllocator<ContentType>,
    private val mountConfigurationCall: MountConfigurationScope<ContentType>.() -> Unit
) {
  val renderUnit: PrimitiveRenderUnit<ContentType> by
      lazy(LazyThreadSafetyMode.NONE) {
        val mountConfigurationScope = MountConfigurationScope<ContentType>()
        mountConfigurationScope.mountConfigurationCall()

        object :
            PrimitiveRenderUnit<ContentType>(
                contentAllocator.renderType,
                mountConfigurationScope.fixedBinders,
                mountConfigurationScope.doesMountRenderTreeHosts) {
          override fun getContentAllocator(): ContentAllocator<ContentType> {
            return this@MountBehavior.contentAllocator
          }

          override fun getId(): Long {
            return this@MountBehavior.id
          }
        }
      }
}

abstract class PrimitiveRenderUnit<ContentType>(
    renderType: RenderType,
    fixedMountBinders: List<DelegateBinder<*, ContentType>>,
    private val doesMountRenderTreeHosts: Boolean
) :
    RenderUnit<ContentType>(
        renderType,
        fixedMountBinders,
        emptyList(), // optional binders
        emptyList() // attach binders
        ) {

  override fun doesMountRenderTreeHosts(): Boolean = doesMountRenderTreeHosts

  public override fun mountBinders(
      context: Context?,
      content: ContentType,
      layoutData: Any?,
      tracer: Systracer?
  ) = super.mountBinders(context, content, layoutData, tracer)

  public override fun unmountBinders(
      context: Context?,
      content: ContentType,
      layoutData: Any?,
      tracer: Systracer?
  ) = super.unmountBinders(context, content, layoutData, tracer)

  public override fun attachBinders(
      context: Context?,
      content: ContentType,
      layoutData: Any?,
      tracer: Systracer?
  ) = super.attachBinders(context, content, layoutData, tracer)

  public override fun detachBinders(
      context: Context?,
      content: ContentType,
      layoutData: Any?,
      tracer: Systracer?
  ) = super.detachBinders(context, content, layoutData, tracer)

  public override fun updateBinders(
      context: Context?,
      content: ContentType,
      currentRenderUnit: RenderUnit<ContentType>?,
      currentLayoutData: Any?,
      newLayoutData: Any?,
      mountDelegate: MountDelegate?,
      isAttached: Boolean
  ) =
      super.updateBinders(
          context,
          content,
          currentRenderUnit,
          currentLayoutData,
          newLayoutData,
          mountDelegate,
          isAttached)

  public override fun getOptionalMountBinderTypeToDelegateMap():
      MutableMap<Class<*>, DelegateBinder<*, ContentType>>? =
      super.getOptionalMountBinderTypeToDelegateMap()

  public override fun getOptionalMountBinders(): MutableList<DelegateBinder<*, ContentType>>? =
      super.getOptionalMountBinders()

  public override fun getAttachBinderTypeToDelegateMap():
      MutableMap<Class<*>, DelegateBinder<*, ContentType>>? =
      super.getAttachBinderTypeToDelegateMap()

  public override fun getAttachBinders(): MutableList<DelegateBinder<*, ContentType>>? =
      super.getAttachBinders()
}
