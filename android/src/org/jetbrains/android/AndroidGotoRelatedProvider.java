package org.jetbrains.android;

import com.android.SdkConstants;
import com.android.resources.ResourceType;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.navigation.GotoRelatedProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.Processor;
import com.intellij.util.containers.HashSet;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.util.AndroidCommonUtils;
import org.jetbrains.android.util.AndroidResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Eugene.Kudelevsky
 */
public class AndroidGotoRelatedProvider extends GotoRelatedProvider {

  private static final String[] CONTEXT_CLASSES = {
    SdkConstants.CLASS_ACTIVITY,
    SdkConstants.CLASS_FRAGMENT,
    SdkConstants.CLASS_V4_FRAGMENT,
    "android.widget.Adapter"
  };

  @NotNull
  @Override
  public List<? extends GotoRelatedItem> getItems(@NotNull PsiElement element) {
    final Computable<List<GotoRelatedItem>> items = getLazyItemsComputable(element);
    return items != null ? items.compute() : Collections.<GotoRelatedItem>emptyList();
  }

  @Nullable
  private static Computable<List<GotoRelatedItem>> getLazyItemsComputable(@NotNull PsiElement element) {
    final PsiFile file = element.getContainingFile();

    if (!(file instanceof XmlFile) && !(file instanceof PsiJavaFile)) {
      return null;
    }
    final VirtualFile vFile = file.getVirtualFile();

    if (vFile == null) {
      return null;
    }
    final Project project = element.getProject();

    if (!FileIndexFacade.getInstance(project).isInContent(vFile)) {
      return null;
    }
    final Module module = ModuleUtilCore.findModuleForFile(vFile, project);

    if (module == null) {
      return null;
    }
    final AndroidFacet facet = AndroidFacet.getInstance(module);

    if (facet == null) {
      return null;
    }
    if (file instanceof PsiJavaFile) {
      PsiClass aClass = PsiTreeUtil.getParentOfType(element, PsiClass.class, false);

      if (aClass == null) {
        final PsiClass[] rootClasses = ((PsiJavaFile)file).getClasses();

        if (rootClasses.length == 1) {
          aClass = rootClasses[0];
        }
      }

      if (aClass != null) {
        return getLazyItemsForClass(aClass, facet);
      }
    }
    else {
      return getLazyItemsForXmlFile((XmlFile)file, facet);
    }
    return null;
  }

  @Nullable
  public static Computable<List<GotoRelatedItem>> getLazyItemsForXmlFile(@NotNull XmlFile file, @NotNull AndroidFacet facet) {
    final String resourceType = facet.getLocalResourceManager().getFileResourceType(file);

    if (ResourceType.LAYOUT.getName().equals(resourceType)) {
      return collectRelatedJavaFiles(file, facet);
    }
    return null;
  }

  @Nullable
  static Computable<List<GotoRelatedItem>> getLazyItemsForClass(@NotNull PsiClass aClass, @NotNull AndroidFacet facet) {
    if (!isInheritorOfContextClass(aClass, facet.getModule())) {
      return null;
    }
    final List<GotoRelatedItem> items = collectRelatedLayoutFiles(facet, aClass);

    if (items.isEmpty()) {
      return null;
    }
    return new Computable<List<GotoRelatedItem>>() {
      @Override
      public List<GotoRelatedItem> compute() {
        return items;
      }
    };
  }

  private static boolean isInheritorOfContextClass(@NotNull PsiClass psiClass, @NotNull Module module) {
    final JavaPsiFacade facade = JavaPsiFacade.getInstance(module.getProject());

    for (String contextClassName : CONTEXT_CLASSES) {
      final PsiClass contextClass = facade.findClass(
        contextClassName, module.getModuleWithDependenciesAndLibrariesScope(false));

      if (contextClass != null && psiClass.isInheritor(contextClass, true)) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  private static Computable<List<GotoRelatedItem>> collectRelatedJavaFiles(@NotNull XmlFile file, @NotNull AndroidFacet facet) {
    final String resType = ResourceType.LAYOUT.getName();
    final String resourceName = AndroidCommonUtils.getResourceName(resType, file.getName());
    final PsiField[] fields = AndroidResourceUtil.findResourceFields(facet, resType, resourceName, true);

    if (fields.length == 0 || fields.length > 1) {
      return null;
    }
    final PsiField field = fields[0];
    final Module module = facet.getModule();
    final GlobalSearchScope scope = module.getModuleScope(false);

    return new Computable<List<GotoRelatedItem>>() {
      @Override
      public List<GotoRelatedItem> compute() {
        final JavaPsiFacade facade = JavaPsiFacade.getInstance(module.getProject());
        final List<PsiClass> psiContextClasses = new ArrayList<PsiClass>();

        for (String contextClassName : CONTEXT_CLASSES) {
          final PsiClass contextClass = facade.findClass(
            contextClassName, module.getModuleWithDependenciesAndLibrariesScope(false));

          if (contextClass != null) {
            psiContextClasses.add(contextClass);
          }
        }

        if (psiContextClasses.isEmpty()) {
          return Collections.emptyList();
        }
        final List<GotoRelatedItem> result = new ArrayList<GotoRelatedItem>();

        ReferencesSearch.search(field, scope).forEach(new Processor<PsiReference>() {
          @Override
          public boolean process(PsiReference reference) {
            PsiElement element = reference.getElement();

            if (!(element instanceof PsiReferenceExpression)) {
              return true;
            }
            element = element.getParent();

            if (!(element instanceof PsiExpressionList)) {
              return true;
            }
            element = element.getParent();

            if (!(element instanceof PsiMethodCallExpression)) {
              return true;
            }
            final String methodName = ((PsiMethodCallExpression)element).
              getMethodExpression().getReferenceName();

            if ("setContentView".equals(methodName) || "inflate".equals(methodName)) {
              final PsiClass relatedClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);

              if (relatedClass != null && isInheritorOfOne(relatedClass, psiContextClasses)) {
                result.add(new GotoRelatedItem(relatedClass, "JAVA"));
              }
            }
            return true;
          }
        });
        return result;
      }
    };
  }

  private static boolean isInheritorOfOne(@NotNull PsiClass psiClass, @NotNull Collection<PsiClass> possibleBaseClasses) {
    for (PsiClass baseClass : possibleBaseClasses) {
      if (psiClass.isInheritor(baseClass, true)) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  private static List<GotoRelatedItem> collectRelatedLayoutFiles(@NotNull final AndroidFacet facet, @NotNull PsiClass context) {
    final Set<PsiFile> files = new HashSet<PsiFile>();

    context.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitReferenceExpression(PsiReferenceExpression expression) {
        super.visitReferenceExpression(expression);

        final String resClassName = ResourceType.LAYOUT.getName();
        final Pair<String, String> pair = AndroidResourceUtil.getReferredResourceField(facet, expression, resClassName);

        if (pair == null) {
          return;
        }
        final String resFieldName = pair.getSecond();
        final List<PsiElement> resources = facet.getLocalResourceManager().findResourcesByFieldName(resClassName, resFieldName);

        for (PsiElement resource : resources) {
          if (resource instanceof PsiFile) {
            files.add((PsiFile)resource);
          }
        }
      }
    });
    if (files.isEmpty()) {
      return Collections.emptyList();
    }
    final List<GotoRelatedItem> result = new ArrayList<GotoRelatedItem>(files.size());

    for (PsiFile file : files) {
      result.add(new MyGotoRelatedLayoutItem(file));
    }
    return result;
  }

  private static class MyGotoRelatedLayoutItem extends GotoRelatedItem {
    private final PsiFile myFile;

    public MyGotoRelatedLayoutItem(@NotNull PsiFile file) {
      super(file, "Layout Files");
      myFile = file;
    }

    @Nullable
    @Override
    public String getCustomContainerName() {
      final PsiDirectory directory = myFile.getContainingDirectory();
      return directory != null ? "(" + directory.getName() + ")" : null;
    }
  }
}