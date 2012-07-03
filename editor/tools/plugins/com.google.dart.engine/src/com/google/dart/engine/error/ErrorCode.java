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
package com.google.dart.engine.error;

/**
 * Instances of the class {@code ErrorCode} define the behavior common to objects representing error
 * codes associated with {@link AnalysisError analysis errors}.
 */
public interface ErrorCode {
  /**
   * Return the {@link ErrorSeverity severity} of this error.
   */
  public ErrorSeverity getErrorSeverity();

  /**
   * Return the message template used to create the message to be displayed for this error.
   */
  public String getMessage();

  /**
   * Return the {@link SubSystem} which issued this error.
   */
  public SubSystem getSubSystem();

  /**
   * Return {@code true} if this {@link ErrorCode} should cause recompilation of the source during
   * the next incremental compilation.
   */
  public boolean needsRecompilation();
}
