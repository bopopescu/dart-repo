/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.tools.ui.actions;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.frog.Dart2JSCompiler;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.ui.ImportedDartLibraryContainer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

/**
 * An action to create an optimized Javascript build of a Dart library.
 */
public class GenerateJavascriptAction extends AbstractInstrumentedAction implements
    IWorkbenchAction, ISelectionListener, IPartListener {

  class DeployOptimizedJob extends Job {
    private DartLibrary library;

    public DeployOptimizedJob(IWorkbenchPage page, DartLibrary library) {
      super(ActionMessages.GenerateJavascriptAction_jobTitle);

      this.library = library;

      // Synchronize on the workspace root to catch any builds that are in progress.
      setRule(ResourcesPlugin.getWorkspace().getRoot());

      // Make sure we display a progress dialog if we do block.
      setUser(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      if (DartCore.getPlugin().getCompileWithFrog()) {
        try {
          monitor.beginTask(
              ActionMessages.GenerateJavascriptAction_Compiling + library.getElementName(),
              IProgressMonitor.UNKNOWN);

          Dart2JSCompiler.compileLibrary(library, monitor, DartCore.getConsole());

          return Status.OK_STATUS;
        } catch (OperationCanceledException exception) {
          // The user cancelled.
          DartCore.getConsole().println("Generation cancelled.");

          return Status.CANCEL_STATUS;
        } catch (Exception exception) {
          DartCore.getConsole().println(
              NLS.bind(ActionMessages.GenerateJavascriptAction_FailException, exception.toString()));

          return Status.CANCEL_STATUS;
        } finally {
          monitor.done();
        }
      }

      return new Status(Status.WARNING, "Deploy optimized", "Dart SDK not installed");
    }
  }

  private IWorkbenchWindow window;

  private Object selectedObject;

  public GenerateJavascriptAction(IWorkbenchWindow window) {
    this.window = window;

    setText(ActionMessages.GenerateJavascriptAction_title);
    setActionDefinitionId("com.google.dart.tools.ui.generateJavascript");
    setDescription(ActionMessages.GenerateJavascriptAction_description);
    setToolTipText(ActionMessages.GenerateJavascriptAction_tooltip);
    //setImageDescriptor(DartToolsPlugin.getImageDescriptor("icons/full/dart16/library_opt.png"));
    setEnabled(false);

    window.getPartService().addPartListener(this);
    window.getSelectionService().addSelectionListener(this);
  }

  @Override
  public void dispose() {

  }

  @Override
  public void partActivated(IWorkbenchPart part) {
    if (part instanceof IEditorPart) {
      handleEditorActivated((IEditorPart) part);
    }
  }

  @Override
  public void partBroughtToTop(IWorkbenchPart part) {

  }

  @Override
  public void partClosed(IWorkbenchPart part) {

  }

  @Override
  public void partDeactivated(IWorkbenchPart part) {

  }

  @Override
  public void partOpened(IWorkbenchPart part) {

  }

  @Override
  public void run() {
    EmitInstrumentationCommand();
    deployOptimized(window.getActivePage());
  }

  @Override
  public void selectionChanged(IWorkbenchPart part, ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      handleSelectionChanged((IStructuredSelection) selection);
    }
  }

  private void deployOptimized(IWorkbenchPage page) {
    boolean isSaveNeeded = isSaveAllNeeded(page);

    if (isSaveNeeded) {
      if (!saveDirtyEditors(page)) {
        // The user cancelled the launch.
        return;
      }
    }

    final DartLibrary library = getCurrentLibrary();

    if (library == null) {
      MessageDialog.openError(
          window.getShell(),
          ActionMessages.GenerateJavascriptAction_unableToLaunch,
          ActionMessages.GenerateJavascriptAction_noneSelected);
    } else {
      DeployOptimizedJob job = new DeployOptimizedJob(page, library);
      job.schedule(isSaveNeeded ? 100 : 0);
    }
  }

  private DartLibrary getCurrentLibrary() {
    IResource resource = null;
    DartElement element = null;

    if (selectedObject instanceof IResource) {
      resource = (IResource) selectedObject;
    }

    if (resource != null) {
      element = DartCore.create(resource);
    }

    if (selectedObject instanceof DartElement) {
      element = (DartElement) selectedObject;
    }

    if (selectedObject instanceof ImportedDartLibraryContainer) {
      element = ((ImportedDartLibraryContainer) selectedObject).getDartLibrary();
    }

    if (element == null) {
      return null;
    } else {
      // DartElement in a library
      DartLibrary library = element.getAncestor(DartLibrary.class);

      return library;
    }
  }

  private void handleEditorActivated(IEditorPart editorPart) {
    if (editorPart.getEditorInput() instanceof IFileEditorInput) {
      IFileEditorInput input = (IFileEditorInput) editorPart.getEditorInput();

      handleSelectionChanged(new StructuredSelection(input.getFile()));
    }
  }

  private void handleSelectionChanged(IStructuredSelection selection) {
    if (selection != null && !selection.isEmpty()) {
      selectedObject = selection.getFirstElement();

      setEnabled(true);
    } else {
      selectedObject = null;

      setEnabled(false);
    }
  }

  private boolean isSaveAllNeeded(IWorkbenchPage page) {
    IEditorReference[] editors = page.getEditorReferences();
    for (int i = 0; i < editors.length; i++) {
      IEditorReference ed = editors[i];
      if (ed.isDirty()) {
        return true;
      }
    }
    return false;
  }

  private boolean saveDirtyEditors(IWorkbenchPage page) {
    return page.saveAllEditors(false);
  }

}
