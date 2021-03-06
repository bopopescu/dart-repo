/*
 * Copyright 2012, the Dart project authors.
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
package com.google.dart.engine.ast;

/**
 * Instances of the class {@code Statement} defines the behavior common to nodes that represent a
 * statement.
 * 
 * <pre>
 * statement ::=
 *     {@link Block block}
 *   | {@link VariableDeclarationStatement initializedVariableDeclaration ';'}
 *   | {@link ForStatement forStatement}
 *   | {@link ForEachStatement forEachStatement}
 *   | {@link WhileStatement whileStatement}
 *   | {@link DoStatement doStatement}
 *   | {@link SwitchStatement switchStatement}
 *   | {@link IfStatement ifStatement}
 *   | {@link TryStatement tryStatement}
 *   | {@link BreakStatement breakStatement}
 *   | {@link ContinueStatement continueStatement}
 *   | {@link ReturnStatement returnStatement}
 *   | {@link ThrowStatement throwStatement}
 *   | {@link ExpressionStatement expressionStatement}
 *   | {@link FunctionDeclarationStatement functionSignature functionBody}
 * </pre>
 */
public abstract class Statement extends ASTNode {
}
