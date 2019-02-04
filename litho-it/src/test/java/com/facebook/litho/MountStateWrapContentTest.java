/*
 * Copyright 2018-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.os.Build;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(ComponentsTestRunner.class)
public class MountStateWrapContentTest {
  private ComponentContext mContext;
  private Component mNoDLComponent;
  private Component mDLComponent;
  private Component mViewComponent;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mNoDLComponent =
        TestDrawableComponent.create(mContext, 0, 0, true, true, true, false, false).build();
    mDLComponent =
        TestDrawableComponent.create(mContext, 0, 0, true, true, true, false, true).build();
    mViewComponent = TestViewComponent.create(mContext).build();
  }

  @Test
  public void testWrapContentPreLollipop() {
    ComponentsConfiguration.useDisplayListForAllDrawables = true;

    final Context androidContext = mContext.getAndroidContext();
    Object content = mDLComponent.onCreateMountContent(androidContext);
    assertThat(MountState.wrapContentIfNeeded(androidContext, mDLComponent, content, false))
        .isNull();

    content = mNoDLComponent.onCreateMountContent(mContext.getAndroidContext());
    assertThat(MountState.wrapContentIfNeeded(androidContext, mNoDLComponent, content, false))
        .isNull();

    content = mViewComponent.onCreateMountContent(mContext.getAndroidContext());
    assertThat(MountState.wrapContentIfNeeded(androidContext, mViewComponent, content, false))
        .isNull();
  }

  @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
  @Test
  public void testWrapContentWithDLEnabled() {
    ComponentsConfiguration.useDisplayListForAllDrawables = false;

    final Context androidContext = mContext.getAndroidContext();
    Object content = mDLComponent.onCreateMountContent(androidContext);
    assertThat(MountState.wrapContentIfNeeded(androidContext, mDLComponent, content, false))
        .isInstanceOf(DisplayListDrawable.class);

    content = mDLComponent.onCreateMountContent(androidContext);
    assertThat(MountState.wrapContentIfNeeded(androidContext, mDLComponent, content, true))
        .isNull();

    content = mNoDLComponent.onCreateMountContent(androidContext);
    assertThat(MountState.wrapContentIfNeeded(androidContext, mNoDLComponent, content, false))
        .isNull();

    content = mViewComponent.onCreateMountContent(androidContext);
    assertThat(MountState.wrapContentIfNeeded(androidContext, mViewComponent, content, false))
        .isNull();
  }

  @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
  @Test
  public void testWrapContentWithWrappingAllDrawables() {
    ComponentsConfiguration.useDisplayListForAllDrawables = true;

    final Context androidContext = mContext.getAndroidContext();
    Object content = mDLComponent.onCreateMountContent(androidContext);
    assertThat(MountState.wrapContentIfNeeded(androidContext, mDLComponent, content, false))
        .isInstanceOf(DisplayListDrawable.class);

    content = mDLComponent.onCreateMountContent(androidContext);
    assertThat(MountState.wrapContentIfNeeded(androidContext, mDLComponent, content, true))
        .isNull();

    content = mNoDLComponent.onCreateMountContent(androidContext);
    assertThat(MountState.wrapContentIfNeeded(androidContext, mNoDLComponent, content, false))
        .isInstanceOf(DisplayListDrawable.class);

    content = mNoDLComponent.onCreateMountContent(androidContext);
    assertThat(MountState.wrapContentIfNeeded(androidContext, mNoDLComponent, content, true))
        .isNull();

    content = mViewComponent.onCreateMountContent(androidContext);
    assertThat(MountState.wrapContentIfNeeded(androidContext, mViewComponent, content, false))
        .isNull();
  }
}
