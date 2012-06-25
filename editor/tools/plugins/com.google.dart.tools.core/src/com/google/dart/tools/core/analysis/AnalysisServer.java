/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.core.analysis;

import com.google.dart.compiler.SystemLibraryManager;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.internal.model.EditorLibraryManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Provides analysis of Dart code for Dart editor
 */
public class AnalysisServer {

  private static final String CACHE_V3_TAG = "v3";
  private static final String CACHE_V2_TAG = "v2";
  private static final String CACHE_V1_TAG = "v1";
  private static final String ANALYZE_CONTEXT_TAG = AnalyzeContextTask.class.getSimpleName();
  private static final String END_LIBRARIES_TAG = "</end-libraries>";
  private static final String END_QUEUE_TAG = "</end-queue>";

  private static PerformanceListener performanceListener;

  public static PerformanceListener getPerformanceListener() {
    return performanceListener;
  }

  public static void setPerformanceListener(PerformanceListener performanceListener) {
    AnalysisServer.performanceListener = performanceListener;
  }

  private IdleListener[] idleListeners = new IdleListener[0];

  /**
   * The target (VM, Dartium, JS) against which user libraries are resolved. Targets are immutable
   * and can be accessed on any thread.
   */
  private final EditorLibraryManager libraryManager;

  /**
   * The library files being analyzed by the receiver. Lock against {@link #queue} before accessing
   * this object.
   */
  private final ArrayList<File> libraryFiles = new ArrayList<File>();

  /**
   * The outstanding tasks to be performed. Lock against this object before accessing it.
   */
  private final ArrayList<Task> queue = new ArrayList<Task>();

  /**
   * The index at which the task being performed can insert new tasks. Tracking this allows new
   * tasks to take priority and be first in the queue. Lock against {@link #queue} before accessing
   * this field.
   */
  private int queueIndex = 0;

  /**
   * The background thread on which analysis tasks are performed or <code>null</code> if the
   * background process has not been started yet. Lock against {@link #queue} before accessing this
   * field.
   */
  private Thread backgroundThread;

  /**
   * A context representing what is "saved on disk". Contents of this object should only be accessed
   * on the background thread.
   */
  private final SavedContext savedContext = new SavedContext(this);

  /**
   * A context representing what is "currently being edited". Contents of this object should only be
   * accessed on the background thread.
   */
  private final EditContext editContext = new EditContext(this, savedContext);

  /**
   * <code>true</code> if the background thread should continue executing analysis tasks. Lock
   * against {@link #queue} before accessing this field.
   */
  private boolean analyze;

  /**
   * Flag indicating whether the server is waiting for more tasks to be queued. Lock against
   * {@link #queue} before accessing this field.
   */
  private boolean isServerIdle = false;

  /**
   * Create a new instance that processes analysis tasks on a background thread
   * 
   * @param libraryManager the target (VM, Dartium, JS) against which user libraries are resolved
   */
  public AnalysisServer(EditorLibraryManager libraryManager) {
    if (libraryManager == null) {
      throw new IllegalArgumentException();
    }
    this.libraryManager = libraryManager;
  }

  public void addIdleListener(IdleListener listener) {
    for (int i = 0; i < idleListeners.length; i++) {
      if (idleListeners[i] == listener) {
        return;
      }
    }
    int oldLen = idleListeners.length;
    IdleListener[] newListeners = new IdleListener[oldLen + 1];
    System.arraycopy(idleListeners, 0, newListeners, 0, oldLen);
    newListeners[oldLen] = listener;
    idleListeners = newListeners;
  }

  /**
   * Analyze the specified library, and keep that analysis current by tracking any changes. Also see
   * {@link Context#resolve(File, ResolveCallback)}.
   * 
   * @param file the library file (not <code>null</code>)
   */
  public void analyze(File file) {
    if (!file.isAbsolute()) {
      throw new IllegalArgumentException("File path must be absolute: " + file);
    }
    synchronized (queue) {
      if (!libraryFiles.contains(file)) {
        libraryFiles.add(file);
        // Append analysis task to the end of the queue so that any user requests take precedence
        queueAnalyzeContext();
      }
    }
  }

  /**
   * Called when a file or directory has been added or removed or file content has been modified.
   * Use {@link #discard(File)} if the file or directory content should no longer be analyzed.
   * 
   * @param file the file or directory (not <code>null</code>)
   */
  public void changed(File file) {
    queueNewTask(new FileChangedTask(this, savedContext, file));
  }

  /**
   * Stop analyzing the specified library or all libraries in the specified directory tree.
   * 
   * @param file the library file (not <code>null</code>)
   */
  public void discard(File file) {

    // If this is a dart file, then discard the library

    if (file.isFile() || (!file.exists() && DartCore.isDartLikeFileName(file.getName()))) {
      synchronized (queue) {
        libraryFiles.remove(file);
        queueNewTask(new DiscardLibraryTask(this, savedContext, file));
      }
      return;
    }

    // Otherwise, discard all libraries in the specified directory tree

    String prefix = file.getAbsolutePath() + File.separator;
    synchronized (queue) {
      Iterator<File> iter = libraryFiles.iterator();
      while (iter.hasNext()) {
        File libraryFile = iter.next();
        if (libraryFile.getPath().startsWith(prefix)) {
          iter.remove();
          queueNewTask(new DiscardLibraryTask(this, savedContext, libraryFile));
        }
      }
    }
  }

  /**
   * Answer the context containing analysis of Dart source currently being edited
   */
  public EditContext getEditContext() {
    return editContext;
  }

  /**
   * Answer the context containing analysis of Dart source on disk
   */
  public SavedContext getSavedContext() {
    return savedContext;
  }

  /**
   * Answer <code>true</code> if the receiver does not have any queued tasks and the receiver's
   * background thread is waiting for new tasks to be queued.
   */
  public boolean isIdle() {
    synchronized (queue) {
      return isServerIdle;
    }
  }

  /**
   * Reload the cached information from the previous session. This method must be called before
   * {@link #start()} has been called when the server is not yet running.
   * 
   * @return <code>true</code> if the cached information was successfully loaded, else
   *         <code>false</code>
   */
  public boolean readCache() {
    File cacheFile = getAnalysisStateFile();
    if (cacheFile.exists()) {
      try {
        BufferedReader reader = new BufferedReader(new FileReader(cacheFile));
        try {
          return readCache(reader);
        } finally {
          try {
            reader.close();
          } catch (IOException e) {
            DartCore.logError("Failed to close analysis cache: " + cacheFile, e);
          }
        }
      } catch (IOException e) {
        DartCore.logError("Failed to read analysis cache: " + cacheFile, e);
        //$FALL-THROUGH$
      }
    }
    reanalyze();
    return false;
  }

  /**
   * Called when all cached information should be discarded and all libraries reanalyzed
   */
  public void reanalyze() {
    queueNewTask(new EverythingChangedTask(this, savedContext));
  }

  public void removeIdleListener(IdleListener listener) {
    int oldLen = idleListeners.length;
    for (int i = 0; i < oldLen; i++) {
      if (idleListeners[i] == listener) {
        IdleListener[] newListeners = new IdleListener[oldLen - 1];
        System.arraycopy(idleListeners, 0, newListeners, 0, i);
        System.arraycopy(idleListeners, i + 1, newListeners, i, oldLen - 1 - i);
        return;
      }
    }
  }

  /**
   * Scan the specified file or recursively scan the specified directory for libraries to analyze.
   * If the fullScan parameter is <code>false</code> and the scan takes too long or includes too
   * many bytes of Dart source code, then the scan will stop and the specified folder marked so that
   * it will not be analyzed.
   * 
   * @param file the file or directory of files to scan (not <code>null</code>)
   * @param fullScan <code>true</code> if the scan should recurse infinitely deep and for however
   *          long the scan takes or <code>false</code> if the scan should stop once the time or
   *          size threshold has been reached.
   */
  public void scan(File file, boolean fullScan) {
    queueNewTask(new LibraryScanTask(this, savedContext, file, fullScan));
  }

  /**
   * Start the background analysis process if it has not already been started
   */
  public void start() {
    synchronized (queue) {
      if (analyze) {
        return;
      }
      analyze = true;
      isServerIdle = false;
      backgroundThread = new Thread(new Runnable() {

        @Override
        public void run() {
          try {
            while (true) {

              // Running :: Execute tasks from the queue
              while (true) {
                Task task;
                synchronized (queue) {
                  // if no longer analyzing or queue is empty, then switch to idle state
                  if (!analyze || queue.isEmpty()) {
                    break;
                  }
                  queueIndex = 0;
                  task = queue.remove(0);
                }
                try {
                  task.perform();
                } catch (Throwable e) {
                  DartCore.logError("Analysis Task Exception", e);
                }
              }

              // Notify :: Changing state from Running to Idle
              notifyAndSetServerIdle(true);

              // Idle :: Wait for new tasks on the queue
              synchronized (queue) {
                // while analyzing wait for new tasks
                while (analyze && queue.isEmpty()) {
                  try {
                    queue.wait();
                  } catch (InterruptedException e) {
                    //$FALL-THROUGH$
                  }
                }
                // if no longer analyzing, then exit background thread
                if (!analyze) {
                  break;
                }
              }

              // Notify :: Changing state from Idle to Running
              notifyAndSetServerIdle(false);

            }
          } catch (Throwable e) {
            DartCore.logError("Analysis Server Exception", e);
          }
        }
      }, getClass().getSimpleName());
      backgroundThread.start();
    }
  }

  /**
   * Signal the background analysis thread to stop and wait for up to 5 seconds for it to do so.
   */
  public void stop() {
    synchronized (queue) {
      if (!analyze) {
        return;
      }
      analyze = false;
      queue.notifyAll();
      if (!waitForIdle(5000)) {
        DartCore.logError("Gave up waiting for " + getClass().getSimpleName() + " to stop");
      }
    }
  }

  /**
   * Wait up to the specified number of milliseconds for the receiver to be idle. If the specified
   * number is less than or equal to zero, then this method returns immediately.
   * 
   * @param milliseconds the number of milliseconds to wait for idle
   * @return <code>true</code> if the receiver is idle
   */
  public boolean waitForIdle(long milliseconds) {
    return waitForIdle(true, milliseconds);
  }

  /**
   * Write the cached information to the file used to store analysis state between sessions. This
   * method must be called after {@link #stop()} has been called when the server is not running.
   * 
   * @return <code>true</code> if successful, else false
   */
  public boolean writeCache() {
    File cacheFile = getAnalysisStateFile();
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFile));
      try {
        writeCache(writer);
      } finally {
        try {
          writer.close();
        } catch (IOException e) {
          DartCore.logError("Failed to close analysis cache: " + cacheFile, e);
        }
      }
      return true;
    } catch (IOException e) {
      DartCore.logError("Failed to write analysis cache: " + cacheFile, e);
      return false;
    }
  }

  IdleListener[] getIdleListeners() {
    return idleListeners;
  }

  EditorLibraryManager getLibraryManager() {
    return libraryManager;
  }

  /**
   * Answer the library files identified by {@link #analyze(File)}
   * 
   * @return an array of files (not <code>null</code>, contains no <code>null</code>s)
   */
  File[] getTrackedLibraryFiles() {
    synchronized (queue) {
      return libraryFiles.toArray(new File[libraryFiles.size()]);
    }
  }

  /**
   * Ensure that all libraries have been analyzed by adding an instance of
   * {@link AnalyzeContextTask} to the end of the queue if it has not already been added.
   */
  void queueAnalyzeContext() {
    synchronized (queue) {
      int index = queue.size() - 1;
      if (index >= 0) {
        Task lastTask = queue.get(index);
        if (lastTask instanceof AnalyzeContextTask) {
          return;
        }
      } else {
        index = 0;
      }
      queueTask(index, new AnalyzeContextTask(this, savedContext));
    }
  }

  /**
   * Add a priority task to the front of the queue. Should *not* be called by the current task being
   * performed... use {@link #queueSubTask(Task)} instead.
   */
  void queueNewTask(Task task) {
    synchronized (queue) {
      if (!analyze) {
        return;
      }
      int index = 0;
      if (!task.isPriority()) {
        while (index < queue.size() && queue.get(index).isPriority()) {
          index++;
        }
      }
      queueIndex++;
      queueTask(index, task);
    }
  }

  /**
   * Used by the current task being performed to add subtasks in a way that will not reduce the
   * priority of new tasks that have been queued while the current task is executing
   */
  void queueSubTask(Task subtask) {
    if (Thread.currentThread() != backgroundThread) {
      throw new IllegalStateException();
    }
    synchronized (queue) {
      if (!analyze) {
        return;
      }
      queue.add(queueIndex, subtask);
      queueIndex++;
    }
  }

  /**
   * Remove any tasks related to analysis that do not have callbacks. The assumption is that
   * analysis tasks with explicit callbacks are related to user requests and should be preserved.
   * This should only be called from the background thread.
   */
  void removeAllBackgroundAnalysisTasks() {
    if (Thread.currentThread() != backgroundThread) {
      throw new IllegalStateException();
    }
    synchronized (queue) {
      Iterator<Task> iter = queue.iterator();
      while (iter.hasNext()) {
        if (iter.next().isBackgroundAnalysis()) {
          iter.remove();
        }
      }
    }
  }

  /**
   * Resolve the specified path to a file.
   * 
   * @return the file or <code>null</code> if it could not be resolved
   */
  File resolvePath(URI base, String relPath) {
    if (relPath == null) {
      return null;
    }
    if (SystemLibraryManager.isDartSpec(relPath) || SystemLibraryManager.isPackageSpec(relPath)) {
      URI relativeUri;
      try {
        relativeUri = new URI(relPath);
      } catch (URISyntaxException e) {
        DartCore.logError("Failed to create URI: " + relPath, e);
        return null;
      }
      URI resolveUri = libraryManager.resolveDartUri(relativeUri);
      if (resolveUri == null) {
        return null;
      }
      return new File(resolveUri.getPath());
    }
    File file = new File(relPath);
    if (file.isAbsolute()) {
      return file;
    }
    try {
      String path = base.resolve(new URI(null, null, relPath, null)).normalize().getPath();
      if (path != null) {
        return new File(path);
      }
    } catch (URISyntaxException e) {
      //$FALL-THROUGH$
    }
    return null;
  }

  private File getAnalysisStateFile() {
    return new File(DartCore.getPlugin().getStateLocation().toFile(), "analysis.cache");
  }

  /**
   * TESTING: Answer <code>true</code> if information about the specified library is cached
   * 
   * @param file the library file (not <code>null</code>)
   */
  @SuppressWarnings("unused")
  private boolean isLibraryCached(File file) {
    synchronized (queue) {
      return savedContext.getCachedLibrary(file) != null;
    }
  }

  /**
   * TESTING: Answer <code>true</code> if specified library has been resolved
   * 
   * @param file the library file (not <code>null</code>)
   */
  @SuppressWarnings("unused")
  private boolean isLibraryResolved(File file) {
    synchronized (queue) {
      Library library = savedContext.getCachedLibrary(file);
      return library != null && library.getLibraryUnit() != null;
    }
  }

  private void notifyAndSetServerIdle(boolean idle) {
    for (IdleListener listener : getIdleListeners()) {
      try {
        listener.idle(idle);
      } catch (Throwable e) {
        DartCore.logError("Exception during idle notification", e);
      }
    }
    synchronized (queue) {
      isServerIdle = idle;
      queue.notifyAll();
    }
    Thread.yield();
  }

  /**
   * Append a task to the queue and wait for the background task to notify listeners that it is no
   * longer idle.
   */
  private void queueTask(int index, Task task) {
    synchronized (queue) {
      queue.add(index, task);
      queue.notifyAll();

      // Wait for the background thread to notify others that the server is no longer idle
      if (!waitForIdle(false, 5000) && DartCoreDebug.DEBUG_ANALYSIS) {
        try {
          throw new RuntimeException(
              "Gave up waiting for background thread to run after adding task");
        } catch (Exception e) {
          DartCore.logError(e);
        }
      }
    }
  }

  /**
   * Reload the cached information from the specified file. This method must be called before
   * {@link #start()} has been called when the server is not yet running.
   * 
   * @return <code>true</code> if successful
   */
  private boolean readCache(Reader reader) throws IOException {
    synchronized (queue) {
      if (analyze) {
        throw new IllegalStateException();
      }
    }
    CacheReader cacheReader = new CacheReader(reader);

    int version;
    String line = cacheReader.readString();
    if (CACHE_V3_TAG.equals(line)) {
      version = 3;
    } else if (CACHE_V2_TAG.equals(line)) {
      version = 2;
    } else if (CACHE_V1_TAG.equals(line)) {
      version = 1;
    } else {
      throw new IOException("Expected cache version " + CACHE_V2_TAG + " but found " + line);
    }

    // Tracked libraries
    cacheReader.readFilePaths(libraryFiles, END_LIBRARIES_TAG);
    if (version == 1) {
      queueAnalyzeContext();
      return true;
    }

    // Cached libraries
    savedContext.readCache(cacheReader);
    if (version == 2) {
      queueAnalyzeContext();
      return true;
    }

    // Queued tasks
    int index = 0;
    while (true) {
      String path = cacheReader.readString();
      if (path == null) {
        throw new IOException("Expected " + END_QUEUE_TAG + " but found EOF");
      }
      if (path.equals(END_QUEUE_TAG)) {
        break;
      }
      if (path.equals(ANALYZE_CONTEXT_TAG)) {
        queueAnalyzeContext();
        continue;
      }
      File libraryFile = new File(path);
      queue.add(index++, new AnalyzeLibraryTask(this, savedContext, libraryFile, null));
    }

    // If no queued tasks, then at minimum resolve dart:core
    if (queue.size() == 0) {
      URI dartCoreUri = null;
      try {
        dartCoreUri = new URI("dart:core");
      } catch (URISyntaxException e) {
        DartCore.logError(e);
        return true;
      }
      File dartCoreFile = AnalysisUtility.toFile(this, dartCoreUri);
      if (dartCoreFile != null) {
        queue.add(index++, new AnalyzeLibraryTask(this, savedContext, dartCoreFile, null));
      }
    }

    return true;
  }

  /**
   * Wait up to the specified number of milliseconds for the receiver to have the specified idle
   * state. If the specified number is less than or equal to zero, then this method returns
   * immediately.
   * 
   * @param idleState <code>true</code> if waiting for the receiver to be idle or <code>false</code>
   *          if waiting for the receiver to be running
   * @param milliseconds the number of milliseconds to wait for idle
   * @return <code>true</code> if the receiver is idle
   */
  private boolean waitForIdle(boolean idleState, long milliseconds) {
    synchronized (queue) {
      long end = System.currentTimeMillis() + milliseconds;
      while (isServerIdle != idleState) {
        long delta = end - System.currentTimeMillis();
        if (delta <= 0) {
          return false;
        }
        try {
          queue.wait(delta);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
      return true;
    }
  }

  /**
   * Write the cached information to the specified file. This method must be called after
   * {@link #stop()} has been called when the server is not running.
   */
  private void writeCache(Writer writer) {
    synchronized (queue) {
      if (analyze) {
        throw new IllegalStateException();
      }
    }
    CacheWriter cacheWriter = new CacheWriter(writer);
    cacheWriter.writeString(CACHE_V3_TAG);
    cacheWriter.writeFilePaths(libraryFiles, END_LIBRARIES_TAG);

    savedContext.writeCache(cacheWriter);

    for (Task task : queue) {
      if (task instanceof AnalyzeLibraryTask) {
        AnalyzeLibraryTask libTask = (AnalyzeLibraryTask) task;
        cacheWriter.writeString(libTask.getRootLibraryFile().getPath());
      } else if (task instanceof AnalyzeContextTask) {
        cacheWriter.writeString(ANALYZE_CONTEXT_TAG);
      }
    }
    cacheWriter.writeString(END_QUEUE_TAG);
  }
}
