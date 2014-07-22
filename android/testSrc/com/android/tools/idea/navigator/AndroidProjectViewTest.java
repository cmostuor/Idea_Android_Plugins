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
package com.android.tools.idea.navigator;

import com.android.tools.idea.navigator.nodes.AndroidViewProjectNode;
import com.android.tools.idea.templates.AndroidGradleTestCase;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Queryable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectView.TestProjectTreeStructure;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.ProjectViewTestUtil;
import org.jetbrains.annotations.Nullable;

public class AndroidProjectViewTest extends AndroidGradleTestCase {
  private AndroidProjectViewPane myPane;

  public void testSimple() throws Exception {
    loadProject("projects/navigator/packageview/simple");

    myPane = createPane();
    TestAndroidTreeStructure structure = new TestAndroidTreeStructure(getProject(), myTestRootDisposable);

    Queryable.PrintInfo printInfo = new Queryable.PrintInfo();
    PsiDirectory dir = getBaseFolder();
    assertNotNull(dir);

    String expected =
      getProject().getName() + "\n" +
      " app (Android)\n" +
      "  assets\n" +
      "   raw.asset.txt\n" +
      "  java\n" +
      "   Debug.java\n" +
      "   app\n" +
      "    MainActivity\n" +
      "  manifests\n" +
      "   AndroidManifest.xml (debug)\n" +
      "   AndroidManifest.xml (main)\n" +
      "  res\n" +
      "   drawable\n" +
      "    ic_launcher.png (mdpi)\n" +
      "   layout\n" +
      "    activity_main.xml\n" +
      "   menu\n" +
      "    main.xml\n" +
      "   values\n" +
      "    dimens.xml (3)\n" +
      "     dimens.xml\n" +
      "     dimens.xml (debug)\n" +
      "     dimens.xml (w820dp)\n" +
      "    strings.xml (2)\n" +
      "     strings.xml\n" +
      "     strings.xml (debug)\n" +
      "    styles.xml\n" +
      "  rs\n" +
      "   test.rs\n" +
      " lib (Android)\n" +
      "  java\n" +
      "   lib\n" +
      "  manifests\n" +
      "   AndroidManifest.xml (main)\n" +
      "  res\n" +
      "   drawable\n" +
      "    ic_launcher.png (mdpi)\n" +
      "   values\n" +
      "    strings.xml\n";
    int numLines = expected.split("\n").length;
    ProjectViewTestUtil
      .assertStructureEqual(structure, expected, numLines, PlatformTestUtil.createComparator(printInfo), structure.getRootElement(),
                            printInfo);
  }

  @Nullable
  private PsiDirectory getBaseFolder() throws Exception {
    VirtualFile folder = getProject().getBaseDir();
    assertNotNull("project basedir is null!", folder);
    return PsiManager.getInstance(getProject()).findDirectory(folder);
  }

  private class TestAndroidTreeStructure extends TestProjectTreeStructure {
    public TestAndroidTreeStructure(Project project, Disposable parentDisposable) {
      super(project, parentDisposable);
    }

    @Override
    protected AbstractTreeNode createRoot(Project project, ViewSettings settings) {
      return new AndroidViewProjectNode(project, settings, myPane);
    }

    @Override
    public boolean isShowLibraryContents() {
      return false;
    }

    @Override
    public boolean isHideEmptyMiddlePackages() {
      return true;
    }
  }

  private AndroidProjectViewPane createPane() {
    final AndroidProjectViewPane pane = new AndroidProjectViewPane(getProject());
    pane.createComponent();
    Disposer.register(getProject(), pane);
    return pane;
  }
}