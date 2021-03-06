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
package com.google.dart.tools.internal.corext;

import com.google.dart.compiler.DartCompilationError;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.common.HasSourceInfo;
import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.model.SourceRange;

public class SourceRangeFactory {

  public static SourceRange create(DartCompilationError error) {
    return new SourceRangeImpl(error.getStartPosition(), error.getLength());
  }

  public static SourceRange create(DartNode node) {
    return new SourceRangeImpl(node);
  }

  /**
   * @return the {@link SourceRange} which start at the end of "startInfo" and has specified length.
   */
  public static SourceRange forEndLength(HasSourceInfo startInfo, int length) {
    int start = startInfo.getSourceInfo().getEnd();
    return new SourceRangeImpl(start, length);
  }

  /**
   * @return the {@link SourceRange} which start at the end of "a" and ends at the start of "b".
   */
  public static SourceRange forEndStart(HasSourceInfo a, HasSourceInfo b) {
    int start = a.getSourceInfo().getEnd();
    int end = b.getSourceInfo().getOffset();
    return forStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at the end of "a" and ends "b".
   */
  public static SourceRange forEndStart(HasSourceInfo a, int end) {
    int start = a.getSourceInfo().getEnd();
    return forStartEnd(start, end);
  }

  /**
   * @return the {@link SourceRange} which start at start of "a" and ends at end of "b".
   */
  public static SourceRange forStartEnd(HasSourceInfo a, HasSourceInfo b) {
    int start = a.getSourceInfo().getOffset();
    int end = b.getSourceInfo().getEnd();
    return forStartEnd(start, end);
  }

  public static SourceRange forStartEnd(int start, int end) {
    return new SourceRangeImpl(start, end - start);
  }

  public static SourceRange forStartLength(int start, int length) {
    return new SourceRangeImpl(start, length);
  }

  /**
   * @return the {@link SourceRange} which start at start of "a" and ends at start of "b".
   */
  public static SourceRange forStartStart(HasSourceInfo a, HasSourceInfo b) {
    int start = a.getSourceInfo().getOffset();
    int end = b.getSourceInfo().getOffset();
    return forStartEnd(start, end);
  }

}
