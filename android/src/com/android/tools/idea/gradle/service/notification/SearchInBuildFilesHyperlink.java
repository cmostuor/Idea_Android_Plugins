/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.tools.idea.gradle.service.notification;

import com.android.SdkConstants;
import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindSettings;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Factory;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.*;
import com.intellij.util.AdapterProcessor;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

class SearchInBuildFilesHyperlink extends NotificationHyperlink {
  @NotNull private final Project myProject;
  @NotNull private final String myTextToFind;

  SearchInBuildFilesHyperlink(@NotNull final Project project, @NotNull final String textToFind) {
    super("searchInBuildFiles", "Search in build.gradle files");
    myProject = project;
    myTextToFind = textToFind;
  }

  @Override
  void execute() {
    FindManager findManager = FindManager.getInstance(myProject);
    UsageViewManager usageViewManager = UsageViewManager.getInstance(myProject);

    FindModel findModel = (FindModel)findManager.getFindInProjectModel().clone();
    findModel.setStringToFind(myTextToFind);
    findModel.setReplaceState(false);
    findModel.setOpenInNewTabVisible(true);
    findModel.setOpenInNewTabEnabled(true);
    findModel.setOpenInNewTab(true);
    findModel.setFileFilter(SdkConstants.FN_BUILD_GRADLE);

    findManager.getFindInProjectModel().copyFrom(findModel);
    final FindModel findModelCopy = (FindModel)findModel.clone();

    UsageViewPresentation presentation = FindInProjectUtil.setupViewPresentation(findModel.isOpenInNewTabEnabled(), findModelCopy);
    boolean showPanelIfOnlyOneUsage = !FindSettings.getInstance().isSkipResultsWithOneUsage();
    final FindUsagesProcessPresentation processPresentation =
      FindInProjectUtil.setupProcessPresentation(myProject, showPanelIfOnlyOneUsage, presentation);
    UsageTarget usageTarget = new FindInProjectUtil.StringUsageTarget(findModel.getStringToFind());
    usageViewManager.searchAndShowUsages(new UsageTarget[]{usageTarget}, new Factory<UsageSearcher>() {
      @Override
      public UsageSearcher create() {
        return new UsageSearcher() {
          @Override
          public void generate(@NotNull final Processor<Usage> processor) {
            AdapterProcessor<UsageInfo, Usage> consumer =
              new AdapterProcessor<UsageInfo, Usage>(processor, UsageInfo2UsageAdapter.CONVERTER);
            //noinspection ConstantConditions
            FindInProjectUtil.findUsages(findModelCopy, null, myProject, true, consumer, processPresentation);
          }
        };
      }
    }, processPresentation, presentation, null);
  }
}