// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.ast;

/**
 * Represents a Dart string literal value.
 */
public class DartStringLiteral extends DartLiteral {

  public static DartStringLiteral get(String x) {
    return new DartStringLiteral(x);
  }

  private final String value;

  private DartStringLiteral(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public void visitChildren(ASTVisitor<?> visitor) {
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitStringLiteral(this);
  }
}
