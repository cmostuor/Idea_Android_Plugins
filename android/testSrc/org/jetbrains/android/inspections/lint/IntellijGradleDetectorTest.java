/*
 * Copyright (C) 2014 The Android Open Source Project
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
package org.jetbrains.android.inspections.lint;

import com.android.tools.lint.checks.GradleDetector;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.android.AndroidTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.jetbrains.android.inspections.lint.AndroidLintInspectionToolProvider.*;

public class IntellijGradleDetectorTest extends AndroidTestCase {
  private static final String BASE_PATH = "gradle/";

  @Override
  public void setUp() throws Exception {
    super.setUp();
    AndroidLintInspectionBase.invalidateInspectionShortName2IssueMap();
  }

  public void testDependencies() throws Exception {
    AndroidLintGradleDependencyInspection inspection = new AndroidLintGradleDependencyInspection();
    doTest(inspection, null);
  }

  public void testPaths() throws Exception {
    AndroidLintGradlePathInspection inspection = new AndroidLintGradlePathInspection();
    doTest(inspection, null);
  }

  public void testIdeSupport() throws Exception {
    AndroidLintGradleIdeErrorInspection inspection = new AndroidLintGradleIdeErrorInspection();
    doTest(inspection, null);
  }

  public void testSetter() throws Exception {
    AndroidLintGradleGetterInspection inspection = new AndroidLintGradleGetterInspection();
    doTest(inspection, null);
  }

  public void testCompatibility() throws Exception {
    AndroidLintGradleCompatibleInspection inspection = new AndroidLintGradleCompatibleInspection();
    doTest(inspection, null);
  }

  public void testPlus() throws Exception {
    GradleDetector.PLUS.setEnabledByDefault(true);
    AndroidLintGradleDynamicVersionInspection inspection = new AndroidLintGradleDynamicVersionInspection();
    doTest(inspection, null);
  }

  public void testPathSuppress() throws Exception {
    AndroidLintGradlePathInspection inspection = new AndroidLintGradlePathInspection();
    doTest(inspection, "Suppress: Add //noinspection GradlePath");
  }

  public void testPathSuppressJoin() throws Exception {
    AndroidLintGradlePathInspection inspection = new AndroidLintGradlePathInspection();
    doTest(inspection, "Suppress: Add //noinspection GradlePath");
  }

  private void doTest(@NotNull final AndroidLintInspectionBase inspection, @Nullable String quickFixName) throws Exception {
    createManifest();
    myFixture.enableInspections(inspection);
    VirtualFile file = myFixture.copyFileToProject(BASE_PATH + getTestName(false) + ".gradle", "build.gradle");
    myFixture.configureFromExistingVirtualFile(file);
    myFixture.checkHighlighting(true, false, false);

    if (quickFixName != null) {
      final IntentionAction quickFix = myFixture.getAvailableIntention(quickFixName);
      assertNotNull(quickFix);
      myFixture.launchAction(quickFix);
      myFixture.checkResultByFile(BASE_PATH + getTestName(false) + "_after.gradle");
    }
  }
}