// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.dart.compiler.DartCompiler;
import com.google.dart.compiler.DartCompilerContext;
import com.google.dart.compiler.DartSource;
import com.google.dart.compiler.LibraryDeps;
import com.google.dart.compiler.LibrarySource;
import com.google.dart.compiler.resolver.Elements;
import com.google.dart.compiler.resolver.LibraryElement;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Represents the parsed source from a {@link LibrarySource}.
 */
public class LibraryUnit {
  private final LibrarySource libSource;
  private final LibraryNode selfSourcePath;
  private final List<LibraryNode> importPaths = Lists.newArrayList();
  private final List<LibraryNode> sourcePaths = Lists.newArrayList();
  private final List<LibraryNode> resourcePaths = Lists.newArrayList();
  private final List<LibraryNode> nativePaths = Lists.newArrayList();

  private final Map<String, DartUnit> units = new ConcurrentSkipListMap<String, DartUnit>();
  private final Collection<LibraryUnit> imports = Lists.newArrayList();
  private final Map<LibraryUnit, String> prefixes = Maps.newHashMap();

  private final LibraryElement element;

  private LibraryDeps deps;

  private LibraryNode entryNode;
  private DartUnit selfDartUnit;

  private String name;

  private DartExpression entryPoint;

  private int sourceCount;

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public LibraryUnit(LibrarySource libSource) {
    assert libSource != null;
    this.libSource = libSource;
    element = Elements.libraryElement(this);

    // get the name part of the path, since it needs to be relative
    // TODO(jbrosenberg): change this to use lazy init
    // Note: We don't want an encoded relative path.
    String self = libSource.getUri().getSchemeSpecificPart();
    int lastSlash;
    if ((lastSlash = self.lastIndexOf('/')) > -1) {
      self = self.substring(lastSlash + 1);
    }
    selfSourcePath = new LibraryNode(self);
  }

  public void addImportPath(LibraryNode path) {
    assert path != null;
    importPaths.add(path);
  }

  public void addSourcePath(LibraryNode path) {
    assert path != null;
    sourcePaths.add(path);
    sourceCount++;
  }

  public void addResourcePath(LibraryNode path) {
    assert path != null;
    resourcePaths.add(path);
  }

  public int getSourceCount() {
    return sourceCount;
  }

  public void addNativePath(LibraryNode path) {
    assert path != null;
    nativePaths.add(path);
  }

  public void putUnit(DartUnit unit) {
    unit.setLibrary(this);
    units.put(unit.getSourceName(), unit);
  }

  public DartUnit getUnit(String sourceName) {
    return units.get(sourceName);
  }

  public void addImport(LibraryUnit unit, LibraryNode node) {
    if (unit != null) {
      imports.add(unit);
      if (node != null && node.getPrefix() != null) {
        prefixes.put(unit, node.getPrefix());
      }
    }
  }

  public String getPrefixOf(LibraryUnit library) {
    return prefixes.get(library);
  }

  public LibraryUnit getLibraryWithPrefix(String prefixToMatch) {
    Iterator<LibraryUnit> libraries = prefixes.keySet().iterator();
    Iterator<String> prefixStrings = prefixes.values().iterator();
    while (prefixStrings.hasNext()) {
      LibraryUnit library = libraries.next();
      String prefix = prefixStrings.next();
      if (prefix.equals(prefixToMatch)) {
        return library;
      }
    }
    return null;
  }

  public Collection<LibraryUnit> getLibrariesWithPrefix(String prefixToMatch) {
    Set<Entry<LibraryUnit,String>> entries = prefixes.entrySet();
    List<LibraryUnit> result = new ArrayList<LibraryUnit>(entries.size());
    for (Entry<LibraryUnit,String> entry : entries) {
      if (entry.getValue().equals(prefixToMatch)) {
        result.add(entry.getKey());
      }
    }
    return result;
  }

  public LibraryElement getElement() {
    return element;
  }

  public Iterable<DartUnit> getUnits() {
    return units.values();
  }

  public Iterable<LibraryUnit> getImports() {
    return imports;
  }

  public boolean hasImport(LibraryUnit unit) {
    return imports.contains(unit);
  }

  public DartExpression getEntryPoint() {
    return entryPoint;
  }

  public void setEntryPoint(DartExpression entry) {
    this.entryPoint = entry;
  }

  public DartUnit getSelfDartUnit() {
    return this.selfDartUnit;
  }

  public void setSelfDartUnit(DartUnit unit) {
    this.selfDartUnit = unit;
  }

  /**
   * Return a collection of paths to {@link LibrarySource}s upon which this
   * library or application depends.
   *
   * @return the paths (not <code>null</code>, contains no <code>null</code>)
   */
  public Iterable<LibraryNode> getImportPaths() {
    return importPaths;
  }

  /**
   * Return all prefixes used by this library.
   */
  public Set<String> getPrefixes() {
    return new HashSet<String>(prefixes.values());
  }

  public void initializePrefixes(LibraryUnit source) {
    for (Map.Entry<LibraryUnit,String> e : source.prefixes.entrySet()) {
      prefixes.put(e.getKey(), e.getValue());
    }
  }

  /**
   * Return the path for dart source that corresponds to the same dart file as
   * this library unit. This is added to the set of sourcePaths for this unit.
   *
   * @return the self source path for this unit.
   */
  public LibraryNode getSelfSourcePath() {
    return selfSourcePath;
  }

  /**
   * Answer the source associated with this unit
   *
   * @return the library source (not <code>null</code>)
   */
  public LibrarySource getSource() {
    return libSource;
  }

  /**
   * Return a collection of paths to {@link DartSource}s that are included in
   * this library or application.
   *
   * @return the paths (not <code>null</code>, contains no <code>null</code>)
   */
  public Iterable<LibraryNode> getSourcePaths() {
    return sourcePaths;
  }

  /**
   * Return a collection of paths to resources that are included in
   * this library or application.
   *
   * @return the paths (not <code>null</code>, contains no <code>null</code>)
   */
  public Iterable<LibraryNode> getResourcePaths() {
    return resourcePaths;
  }

  /**
   * Returns a collection of paths to native {@link DartSource}s that are included in this library.
   *
   * @return the paths (not <code>null</code>, contains no <code>null</code>)
   */
  public Iterable<LibraryNode> getNativePaths() {
    return nativePaths;
  }

  /**
   * Return the declared entry method, if any
   *
   * @return the entry method or <code>null</code> if not defined
   */
  public LibraryNode getEntryNode() {
    return entryNode;
  }

  /**
   * Set the declared entry method.
   *
   * @param libraryNode the entry method or <code>null</code> if none
   */
  public void setEntryNode(LibraryNode libraryNode) {
    this.entryNode = libraryNode;
  }

  /**
   * Gets the dependencies associated with this library. If no dependencies artifact exists,
   * or the file is invalid, it will return an empty deps object.
   */
  public LibraryDeps getDeps(DartCompilerContext context) throws IOException {
    if (deps != null) {
      return deps;
    }

    Reader reader = context.getArtifactReader(libSource, "", DartCompiler.EXTENSION_DEPS);
    if (reader != null) {
      deps = LibraryDeps.fromReader(reader);
      reader.close();
    }

    if (deps == null) {
      deps = new LibraryDeps();
    }
    return deps;
  }

  /**
   * Writes this library's associated dependencies.
   */
  public void writeDeps(DartCompilerContext context) throws IOException {
    Writer writer = context.getArtifactWriter(libSource, "", DartCompiler.EXTENSION_DEPS);
    deps.write(writer);
    writer.close();
  }
}
