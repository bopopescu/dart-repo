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
package com.google.dart.tools.ui;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.ui.internal.viewsupport.IProblemChangedListener;
import com.google.dart.tools.ui.internal.viewsupport.ImageDescriptorRegistry;
import com.google.dart.tools.ui.internal.viewsupport.ImageImageDescriptor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import java.util.Iterator;

/**
 * LabelDecorator that decorates an element's image with error and warning overlays that represent
 * the severity of markers attached to the element's underlying resource. To see a problem
 * decoration for a marker, the marker needs to be a subtype of <code>IMarker.PROBLEM</code>.
 * <p>
 * <b>Important</b>: Although this decorator implements ILightweightLabelDecorator, do not
 * contribute this class as a decorator to the <code>org.eclipse.ui.decorators</code> extension.
 * Only use this class in your own views and label providers. Provisional API: This class/interface
 * is part of an interim API that is still under development and expected to change significantly
 * before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost
 * certainly be broken (repeatedly) as the API evolves.
 */
public class ProblemsLabelDecorator implements ILabelDecorator, ILightweightLabelDecorator {

  /**
   * This is a special <code>LabelProviderChangedEvent</code> carrying additional information
   * whether the event origins from a maker change.
   * <p>
   * <code>ProblemsLabelChangedEvent</code>s are only generated by <code>
   * ProblemsLabelDecorator</code>s.
   * </p>
   */
  public static class ProblemsLabelChangedEvent extends LabelProviderChangedEvent {

    private static final long serialVersionUID = 1L;

    private final boolean fMarkerChange;

    /**
     * Note: This constructor is for internal use only. Clients should not call this constructor.
     * 
     * @param eventSource the base label provider
     * @param changedResource the changed resources
     * @param isMarkerChange <code>true</code> if the change is a marker change; otherwise
     *          <code>false</code>
     */
    public ProblemsLabelChangedEvent(IBaseLabelProvider eventSource, IResource[] changedResource,
        boolean isMarkerChange) {
      super(eventSource, changedResource);
      fMarkerChange = isMarkerChange;
    }

    /**
     * Returns whether this event origins from marker changes. If <code>false</code> an annotation
     * model change is the origin. In this case viewers not displaying working copies can ignore
     * these events.
     * 
     * @return if this event origins from a marker change.
     */
    public boolean isMarkerChange() {
      return fMarkerChange;
    }

  }

  private static final int ERRORTICK_WARNING = DartElementImageDescriptor.WARNING;
  private static final int ERRORTICK_ERROR = DartElementImageDescriptor.ERROR;

  private ImageDescriptorRegistry fRegistry;
  private boolean fUseNewRegistry = false;
  private IProblemChangedListener fProblemChangedListener;

  private ListenerList fListeners;
  private SourceRange fCachedRange;

  /**
   * Creates a new <code>ProblemsLabelDecorator</code>.
   */
  public ProblemsLabelDecorator() {
    this(null);
    fUseNewRegistry = true;
  }

  /**
   * Note: This constructor is for internal use only. Clients should not call this constructor.
   * 
   * @param registry The registry to use or <code>null</code> to use the JavaScript plugin's image
   *          registry
   */
  public ProblemsLabelDecorator(ImageDescriptorRegistry registry) {
    fRegistry = registry;
    fProblemChangedListener = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see IBaseLabelProvider#addListener(ILabelProviderListener)
   */
  @Override
  public void addListener(ILabelProviderListener listener) {
    if (fListeners == null) {
      fListeners = new ListenerList();
    }
    fListeners.add(listener);
    if (fProblemChangedListener == null) {
      fProblemChangedListener = new IProblemChangedListener() {
        @Override
        public void problemsChanged(IResource[] changedResources, boolean isMarkerChange) {
          fireProblemsChanged(changedResources, isMarkerChange);
        }
      };
      DartToolsPlugin.getDefault().getProblemMarkerManager().addListener(fProblemChangedListener);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang .Object,
   * org.eclipse.jface.viewers.IDecoration)
   */
  @Override
  public void decorate(Object element, IDecoration decoration) {
    int adornmentFlags = computeAdornmentFlags(element);
    if (adornmentFlags == ERRORTICK_ERROR) {
      decoration.addOverlay(DartPluginImages.DESC_OVR_ERROR);
    } else if (adornmentFlags == ERRORTICK_WARNING) {
      decoration.addOverlay(DartPluginImages.DESC_OVR_WARNING);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see ILabelDecorator#decorateImage(Image, Object)
   */
  @Override
  public Image decorateImage(Image image, Object obj) {
    int adornmentFlags = computeAdornmentFlags(obj);
    if (adornmentFlags != 0) {
      ImageDescriptor baseImage = new ImageImageDescriptor(image);
      Rectangle bounds = image.getBounds();
      return getRegistry().get(
          new DartElementImageDescriptor(baseImage, adornmentFlags, new Point(bounds.width,
              bounds.height)));
    }
    return image;
  }

  /*
   * (non-Javadoc)
   * 
   * @see ILabelDecorator#decorateText(String, Object)
   */
  @Override
  public String decorateText(String text, Object element) {
    return text;
  }

  /*
   * (non-Javadoc)
   * 
   * @see IBaseLabelProvider#dispose()
   */
  @Override
  public void dispose() {
    if (fProblemChangedListener != null) {
      DartToolsPlugin.getDefault().getProblemMarkerManager().removeListener(fProblemChangedListener);
      fProblemChangedListener = null;
    }
    if (fRegistry != null && fUseNewRegistry) {
      fRegistry.dispose();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see IBaseLabelProvider#isLabelProperty(Object, String)
   */
  @Override
  public boolean isLabelProperty(Object element, String property) {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see IBaseLabelProvider#removeListener(ILabelProviderListener)
   */
  @Override
  public void removeListener(ILabelProviderListener listener) {
    if (fListeners != null) {
      fListeners.remove(listener);
      if (fListeners.isEmpty() && fProblemChangedListener != null) {
        DartToolsPlugin.getDefault().getProblemMarkerManager().removeListener(
            fProblemChangedListener);
        fProblemChangedListener = null;
      }
    }
  }

  /**
   * Note: This method is for internal use only. Clients should not call this method.
   * 
   * @param obj the element to compute the flags for
   * @return the adornment flags
   */
  protected int computeAdornmentFlags(Object obj) {
    try {
      if (obj instanceof DartElement) {
        DartElement element = (DartElement) obj;
        int type = element.getElementType();
        DartX.todo();
        switch (type) {
          case DartElement.DART_MODEL:
          case DartElement.DART_PROJECT:
            // case DartElement.PACKAGE_FRAGMENT_ROOT:
            return getErrorTicksFromMarkers(element.getResource(), IResource.DEPTH_INFINITE, null);
            // case DartElement.PACKAGE_FRAGMENT:
          case DartElement.COMPILATION_UNIT:
            // case DartElement.CLASS_FILE:
            return getErrorTicksFromMarkers(element.getResource(), IResource.DEPTH_ONE, null);
            // case DartElement.IMPORT_DECLARATION:
            // case DartElement.IMPORT_CONTAINER:
          case DartElement.TYPE:
            // case DartElement.INITIALIZER:
          case DartElement.METHOD:
          case DartElement.FIELD:
            // case DartElement.LOCAL_VARIABLE:
            CompilationUnit cu = element.getAncestor(CompilationUnit.class);
            if (cu != null) {
              SourceReference ref = (type == DartElement.COMPILATION_UNIT) ? null
                  : (SourceReference) element;
              // The assumption is that only source elements in compilation unit
              // can have markers
              IAnnotationModel model = isInJavaAnnotationModel(cu);
              int result = 0;
              if (model != null) {
                // open in JavaScript editor: look at annotation model
                result = getErrorTicksFromAnnotationModel(model, ref);
              } else {
                result = getErrorTicksFromMarkers(cu.getResource(), IResource.DEPTH_ONE, ref);
              }
              fCachedRange = null;
              return result;
            }
            break;
          default:
        }
      } else if (obj instanceof IResource) {
        return getErrorTicksFromMarkers((IResource) obj, IResource.DEPTH_INFINITE, null);
      }
    } catch (CoreException e) {
      if (e instanceof DartModelException) {
        if (((DartModelException) e).isDoesNotExist()) {
          return 0;
        }
      }
      if (e.getStatus().getCode() == IResourceStatus.MARKER_NOT_FOUND) {
        return 0;
      }

      DartToolsPlugin.log(e);
    }
    return 0;
  }

  protected int getErrorTicksFromMarkers(IResource res, int depth, SourceReference sourceElement)
      throws CoreException {
    if (res == null || !res.isAccessible()) {
      return 0;
    }
    int severity = 0;
    if (sourceElement == null) {
      severity = res.findMaxProblemSeverity(IMarker.PROBLEM, true, depth);
    } else {
      IMarker[] markers = res.findMarkers(IMarker.PROBLEM, true, depth);
      if (markers != null && markers.length > 0) {
        for (int i = 0; i < markers.length && (severity != IMarker.SEVERITY_ERROR); i++) {
          IMarker curr = markers[i];
          if (isMarkerInRange(curr, sourceElement)) {
            int val = curr.getAttribute(IMarker.SEVERITY, -1);
            if (val == IMarker.SEVERITY_WARNING || val == IMarker.SEVERITY_ERROR) {
              severity = val;
            }
          }
        }
      }
    }
    if (severity == IMarker.SEVERITY_ERROR) {
      return ERRORTICK_ERROR;
    } else if (severity == IMarker.SEVERITY_WARNING) {
      return ERRORTICK_WARNING;
    }
    return 0;
  }

  /**
   * Tests if a position is inside the source range of an element.
   * 
   * @param pos Position to be tested.
   * @param sourceElement Source element (must be a DartElement)
   * @return boolean Return <code>true</code> if position is located inside the source element.
   * @throws CoreException Exception thrown if element range could not be accessed.
   */
  protected boolean isInside(int pos, SourceReference sourceElement) throws CoreException {
    if (fCachedRange == null) {
      fCachedRange = sourceElement.getSourceRange();
    }
    SourceRange range = fCachedRange;
    if (range != null) {
      int rangeOffset = range.getOffset();
      return (rangeOffset <= pos && rangeOffset + range.getLength() > pos);
    }
    return false;
  }

  private void fireProblemsChanged(IResource[] changedResources, boolean isMarkerChange) {
    if (fListeners != null && !fListeners.isEmpty()) {
      LabelProviderChangedEvent event = new ProblemsLabelChangedEvent(this, changedResources,
          isMarkerChange);
      Object[] listeners = fListeners.getListeners();
      for (int i = 0; i < listeners.length; i++) {
        ((ILabelProviderListener) listeners[i]).labelProviderChanged(event);
      }
    }
  }

  private int getErrorTicksFromAnnotationModel(IAnnotationModel model, SourceReference sourceElement)
      throws CoreException {
    int info = 0;
    Iterator<?> iter = model.getAnnotationIterator();
    while ((info != ERRORTICK_ERROR) && iter.hasNext()) {
      Annotation annot = (Annotation) iter.next();
      IMarker marker = isAnnotationInRange(model, annot, sourceElement);
      if (marker != null) {
        int priority = marker.getAttribute(IMarker.SEVERITY, -1);
        if (priority == IMarker.SEVERITY_WARNING) {
          info = ERRORTICK_WARNING;
        } else if (priority == IMarker.SEVERITY_ERROR) {
          info = ERRORTICK_ERROR;
        }
      }
    }
    return info;
  }

  private ImageDescriptorRegistry getRegistry() {
    if (fRegistry == null) {
      fRegistry = fUseNewRegistry ? new ImageDescriptorRegistry()
          : DartToolsPlugin.getImageDescriptorRegistry();
    }
    return fRegistry;
  }

  private IMarker isAnnotationInRange(IAnnotationModel model, Annotation annot,
      SourceReference sourceElement) throws CoreException {
    if (annot instanceof MarkerAnnotation) {
      if (sourceElement == null || isInside(model.getPosition(annot), sourceElement)) {
        IMarker marker = ((MarkerAnnotation) annot).getMarker();
        if (marker.exists() && marker.isSubtypeOf(IMarker.PROBLEM)) {
          return marker;
        }
      }
    }
    return null;
  }

  private IAnnotationModel isInJavaAnnotationModel(CompilationUnit original) {
    if (original.isWorkingCopy()) {
      IFile file = (IFile) original.getResource();
      if (file == null) {
        return null;
      }
      FileEditorInput editorInput = new FileEditorInput(file);
      return DartToolsPlugin.getDefault().getCompilationUnitDocumentProvider().getAnnotationModel(
          editorInput);
    }
    return null;
  }

  private boolean isInside(Position pos, SourceReference sourceElement) throws CoreException {
    return pos != null && isInside(pos.getOffset(), sourceElement);
  }

  private boolean isMarkerInRange(IMarker marker, SourceReference sourceElement)
      throws CoreException {
    if (marker.isSubtypeOf(IMarker.TEXT)) {
      int pos = marker.getAttribute(IMarker.CHAR_START, -1);
      return isInside(pos, sourceElement);
    }
    return false;
  }

}
