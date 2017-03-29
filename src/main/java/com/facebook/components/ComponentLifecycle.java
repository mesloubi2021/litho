/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.yoga.YogaAlign;

import com.facebook.yoga.YogaFlexDirection;

import java.util.concurrent.atomic.AtomicInteger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.StyleRes;
import android.support.v4.util.Pools;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.ExploreByTouchHelper;
import android.view.View;

import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.yoga.YogaBaselineFunction;
import com.facebook.yoga.YogaMeasureMode;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaNodeAPI;
import com.facebook.yoga.YogaMeasureOutput;

/**
 * {@link ComponentLifecycle} is a stateless singleton object that defines how {@link Component}
 * instances calculate their layout bounds and mount elements, among other things. This is the
 * base class from which all new component types inherit.
 *
 * In most cases, the {@link ComponentLifecycle} class will be automatically generated by the
 * annotation processor at build-time based on your spec class and you won't have to deal with
 * it directly when implementing new component types.
 */
public abstract class ComponentLifecycle implements EventDispatcher {
  private static final AtomicInteger sComponentId = new AtomicInteger();
  private static final int DEFAULT_MAX_PREALLOCATION = 15;

  private boolean mPreallocationDone;

  public enum MountType {
    NONE,
    DRAWABLE,
    VIEW,
  }

  public interface StateContainer {}

  private static final YogaBaselineFunction sBaselineFunction = new YogaBaselineFunction() {
    public float baseline(YogaNodeAPI cssNode, float width, float height) {
      final InternalNode node = (InternalNode) cssNode.getData();
      return node.getComponent()
          .getLifecycle()
          .onMeasureBaseline(node.getContext(), (int) width, (int) height);
    }
  };

  private static final YogaMeasureFunction sMeasureFunction = new YogaMeasureFunction() {

    private final Pools.SynchronizedPool<Size> mSizePool =
        new Pools.SynchronizedPool<>(2);

    private Size acquireSize(int initialValue) {
      Size size = mSizePool.acquire();
      if (size == null) {
        size = new Size();
      }

      size.width = initialValue;
      size.height = initialValue;
      return size;
    }

    private void releaseSize(Size size) {
      mSizePool.release(size);
    }

    @Override
    @SuppressLint("WrongCall")
    @SuppressWarnings("unchecked")
    public long measure(
        YogaNodeAPI cssNode,
        float width,
        YogaMeasureMode widthMode,
        float height,
        YogaMeasureMode heightMode) {
      final InternalNode node = (InternalNode) cssNode.getData();
      final DiffNode diffNode = node.areCachedMeasuresValid() ? node.getDiffNode() : null;
      final Component<?> component = node.getComponent();
      final int widthSpec;
      final int heightSpec;

      ComponentsSystrace.beginSection("measure:" + component.getSimpleName());
      widthSpec = SizeSpec.makeSizeSpecFromCssSpec(width, widthMode);
      heightSpec = SizeSpec.makeSizeSpecFromCssSpec(height, heightMode);

      node.setLastWidthSpec(widthSpec);
      node.setLastHeightSpec(heightSpec);

      int outputWidth = 0;
      int outputHeight = 0;

      if (Component.isNestedTree(component)) {
        final InternalNode nestedTree = LayoutState.resolveNestedTree(node, widthSpec, heightSpec);

        outputWidth = nestedTree.getWidth();
        outputHeight = nestedTree.getHeight();
      } else if (diffNode != null
          && diffNode.getLastWidthSpec() == widthSpec
          && diffNode.getLastHeightSpec() == heightSpec) {
        outputWidth = (int) diffNode.getLastMeasuredWidth();
        outputHeight = (int) diffNode.getLastMeasuredHeight();
      } else {
        final Size size = acquireSize(Integer.MIN_VALUE /* initialValue */);
