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
package com.android.tools.idea.structure;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.ui.TabbedPaneWrapper;
import com.intellij.ui.navigation.History;
import com.intellij.ui.navigation.Place;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a Project Structure editor for an individual module. Can load a number of sub-editors in a tabbed pane.
 */
public class AndroidModuleEditor implements Place.Navigator, Disposable {
  private final Project myProject;
  private final String myName;
  private final List<ModuleConfigurationEditor> myEditors = new ArrayList<ModuleConfigurationEditor>();
  private JComponent myGenericSettingsPanel;

  public AndroidModuleEditor(@NotNull Project project, String moduleName) {
    myProject = project;
    myName = moduleName;
  }

  public JComponent getPanel() {
    if (myGenericSettingsPanel == null) {
      myEditors.clear();
      myEditors.add(new ModuleDependenciesEditor(myProject, myName));
      if (myEditors.size() == 1) {
        ModuleConfigurationEditor editor = myEditors.get(0);
        myGenericSettingsPanel = editor.createComponent();
        editor.reset();
      } else {
        TabbedPaneWrapper tabbedPane = new TabbedPaneWrapper(this);
        for (ModuleConfigurationEditor editor : myEditors) {
          JComponent component = editor.createComponent();
          if (component != null) {
            tabbedPane.addTab(editor.getDisplayName(), component);
            editor.reset();
          }
        }
        myGenericSettingsPanel = tabbedPane.getComponent();
      }
    }
    return myGenericSettingsPanel;
  }

  @Override
  public void dispose() {
    for (final ModuleConfigurationEditor myEditor : myEditors) {
      myEditor.disposeUIResources();
    }
    myEditors.clear();
    myGenericSettingsPanel = null;
  }

  public boolean isModified() {
    for (ModuleConfigurationEditor moduleElementsEditor : myEditors) {
      if (moduleElementsEditor.isModified()) {
        return true;
      }
    }
    return false;
  }

  public void apply() throws ConfigurationException {
    for (ModuleConfigurationEditor editor : myEditors) {
      editor.saveData();
      editor.apply();
    }
  }

  public String getName() {
    return myName;
  }

  @Override
  @Nullable
  public ActionCallback navigateTo(@Nullable Place place, boolean requestFocus) {
    return null;
  }

  @Override
  public void queryPlace(@NotNull Place place) {
  }

  @Override
  public void setHistory(final History history) {
  }
}