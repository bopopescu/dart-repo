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
package com.google.dart.tools.core.analysis;

import com.google.dart.tools.core.internal.model.EditorLibraryManager;
import com.google.dart.tools.core.internal.model.SystemLibraryManagerProvider;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

class Listener implements AnalysisListener, IdleListener {
  private static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private boolean idle;

  private final HashMap<String, HashSet<String>> parsed = new HashMap<String, HashSet<String>>();
  private final HashSet<String> resolved = new HashSet<String>();
  private final HashSet<String> discarded = new HashSet<String>();
  private final StringBuilder duplicates = new StringBuilder();

  private final ArrayList<AnalysisError> errors = new ArrayList<AnalysisError>();

  public Listener(AnalysisServer server) {
    server.addIdleListener(this);
    server.getSavedContext().addAnalysisListener(this);
    server.getEditContext().addAnalysisListener(this);
  }

  @Override
  public void discarded(AnalysisEvent event) {
    discarded.add(event.getLibraryFile().getPath());
    for (File file : event.getFiles()) {
      discarded.add(file.getPath());
    }
  }

  @Override
  public void idle(boolean idle) {
    this.idle = idle;
  }

  public boolean isIdle() {
    return idle;
  }

  @Override
  public void parsed(AnalysisEvent event) {
    String libFilePath = event.getLibraryFile().getPath();
    HashSet<String> parsedInLib = parsed.get(libFilePath);
    if (parsedInLib == null) {
      parsedInLib = new HashSet<String>();
      parsed.put(libFilePath, parsedInLib);
    }
    for (File file : event.getFiles()) {
      if (!parsedInLib.add(file.getPath())) {
        if (duplicates.length() > 0) {
          duplicates.append(LINE_SEPARATOR);
        }
        duplicates.append("Duplicate parse: " + file + LINE_SEPARATOR + "  in " + libFilePath);
      }
    }
    errors.addAll(event.getErrors());
  }

  @Override
  public void resolved(AnalysisEvent event) {
    String libPath = event.getLibraryFile().getPath();
    if (!resolved.add(libPath)) {
      if (duplicates.length() > 0) {
        duplicates.append(LINE_SEPARATOR);
      }
      duplicates.append("Duplicate resolution: " + libPath);
    }
    errors.addAll(event.getErrors());
  }

  void assertBundledLibrariesResolved() throws Exception {
    ArrayList<String> notResolved = new ArrayList<String>();
    EditorLibraryManager libraryManager = SystemLibraryManagerProvider.getAnyLibraryManager();
    ArrayList<String> librarySpecs = new ArrayList<String>(libraryManager.getAllLibrarySpecs());
    for (String urlSpec : librarySpecs) {
      URI libraryUri = new URI(urlSpec);
      File libraryFile = new File(libraryManager.resolveDartUri(libraryUri));
      String libraryPath = libraryFile.getPath();
      if (!resolved.contains(libraryPath)) {
        notResolved.add(libraryPath);
      }
    }
    if (notResolved.size() > 0) {
      AnalysisServerTest.fail("Expected these libraries to be resolved: " + notResolved);
    }
  }

  void assertNoDiscards() {
    if (discarded.size() > 0) {
      String errMsg = "Expected no discards, but found:";
      for (String path : new TreeSet<String>(discarded)) {
        errMsg += LINE_SEPARATOR + "  " + path;
      }
      fail(errMsg);
    }
  }

  void assertNoDuplicates() {
    if (duplicates.length() > 0) {
      AnalysisServerTest.fail(duplicates.toString());
    }
  }

  void assertWasDiscarded(File file) {
    if (!discarded.contains(file.getPath())) {
      String errMsg = "Expected discard" + LINE_SEPARATOR + "  " + file.getPath();
      errMsg += LINE_SEPARATOR + "but found:";
      for (String path : new TreeSet<String>(discarded)) {
        errMsg += LINE_SEPARATOR + "  " + path;
      }
      fail(errMsg);
    }
  }

  void assertWasNotParsed(File libraryFile, File file) {
    HashSet<String> parsedInLib = parsed.get(libraryFile.getPath());
    if (parsedInLib != null && parsedInLib.contains(file.getPath())) {
      fail("Did not expect parse notification for " + file + "  in " + libraryFile);
    }
  }

  void assertWasParsed(File libraryFile, File file) {
    HashSet<String> parsedInLib = parsed.get(libraryFile.getPath());
    if (parsedInLib == null || !parsedInLib.contains(file.getPath())) {
      fail("Expected parse notification for " + file + LINE_SEPARATOR + "  in " + libraryFile
          + " but found " + (parsedInLib != null ? parsedInLib : parsed));
    }
  }

  void assertWasResolved(File libraryFile) {
    if (!resolved.contains(libraryFile.getPath())) {
      fail("Expected resolved notification for " + libraryFile + " but found " + resolved);
    }
  }

  ArrayList<AnalysisError> getErrors() {
    return errors;
  }

  int getParsedCount() {
    int count = 0;
    for (HashSet<String> parsedInLib : parsed.values()) {
      count += parsedInLib.size();
    }
    return count;
  }

  HashSet<String> getResolved() {
    return resolved;
  }

  void reset() {
    parsed.clear();
    resolved.clear();
    discarded.clear();
    duplicates.setLength(0);
    errors.clear();
  }
}
