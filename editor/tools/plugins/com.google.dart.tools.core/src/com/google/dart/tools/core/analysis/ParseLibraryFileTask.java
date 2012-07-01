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

import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.UrlLibrarySource;
import com.google.dart.compiler.ast.DartDirective;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.DartCore;

import static com.google.dart.tools.core.analysis.AnalysisUtility.parse;
import static com.google.dart.tools.core.analysis.AnalysisUtility.toLibrarySource;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parse a library source file and create the associated {@link Library}
 */
class ParseLibraryFileTask extends Task {
  private final AnalysisServer server;
  private final Context context;
  private final File libraryFile;

  private final UrlLibrarySource librarySource;

  private final ParseCallback callback;

  ParseLibraryFileTask(AnalysisServer server, Context context, File libraryFile,
      ParseCallback callback) {
    this.server = server;
    this.context = context;
    this.libraryFile = libraryFile;
    this.callback = callback;
    this.librarySource = toLibrarySource(server, libraryFile);
  }

  @Override
  public boolean isBackgroundAnalysis() {
    return callback == null;
  }

  @Override
  public boolean isPriority() {
    return false;
  }

  @Override
  public void perform() {
    Library library = context.getCachedLibrary(libraryFile);

    // Get the cached unit or parse the source

    DartUnit unit = context.getCachedUnit(library, libraryFile);
    if (unit == null || library == null) {
      Set<String> prefixes = new HashSet<String>();
      ErrorListener errorListener = new ErrorListener(server);
      DartSource source = librarySource.getSourceFor(libraryFile.getName());

      unit = parse(libraryFile, source, prefixes, errorListener);

      context.cacheUnresolvedUnit(libraryFile, unit);
      if (library == null || library.shouldNotify) {
        AnalysisEvent event = new AnalysisEvent(libraryFile, errorListener.getErrors());
        event.addFileAndDartUnit(libraryFile, unit);
        event.notifyParsed(context);
      }
    }

    // Ensure the library is built

    if (library == null) {
      List<DartDirective> directives = unit.getDirectives();
      library = Library.fromDartUnit(server, libraryFile, librarySource, directives);
      context.cacheLibrary(library);
    }

    // Notify the caller

    if (callback != null) {
      try {
        callback.parsed(unit);
      } catch (Throwable e) {
        DartCore.logError("Exception during parse notification", e);
      }
    }
  }
}
