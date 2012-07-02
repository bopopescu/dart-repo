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
package com.google.dart.engine.ast.visitor;

import com.google.dart.engine.ast.*;

/**
 * Instances of the class <code>GeneralizingASTVisitor</code> implement an AST visitor that will
 * recursively visit all of the nodes in an AST structure (like instances of the class
 * {@link RecursiveASTVisitor}). In addition, when a node of a specific type is visited not only
 * will the visit method for that specific type of node be invoked, but additional methods for the
 * superclasses of that node will also be invoked. For example, using an instance of this class to
 * visit a {@link Block} will cause the method {@link #visitBlock(Block)} to be invoked but will
 * also cause the methods {@link #visitStatement(Statement)} and {@link #visitNode(ASTNode)} to be
 * subsequently invoked. This allows visitors to be written that visit all statements without
 * needing to override the visit method for each of the specific subclasses of {@link Statement}.
 * <p>
 * Subclasses that override a visit method must either invoke the overridden visit method or
 * explicitly invoke the more general visit method. Failure to do so will cause the visit methods
 * for superclasses of the node to not be invoked and will cause the children of the visited node to
 * not be visited.
 */
public class GeneralizingASTVisitor<R> implements ASTVisitor<R> {
  @Override
  public R visitAdjacentStrings(AdjacentStrings node) {
    return visitStringLiteral(node);
  }

  @Override
  public R visitArgumentList(ArgumentList node) {
    return visitNode(node);
  }

  @Override
  public R visitArrayAccess(ArrayAccess node) {
    return visitExpression(node);
  }

  @Override
  public R visitAssignmentExpression(AssignmentExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitBinaryExpression(BinaryExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitBlock(Block node) {
    return visitStatement(node);
  }

  @Override
  public R visitBlockFunctionBody(BlockFunctionBody node) {
    return visitFunctionBody(node);
  }

  @Override
  public R visitBooleanLiteral(BooleanLiteral node) {
    return visitLiteral(node);
  }

  @Override
  public R visitBreakStatement(BreakStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitCatchClause(CatchClause node) {
    return visitNode(node);
  }

  @Override
  public R visitClassDeclaration(ClassDeclaration node) {
    return visitTypeDeclaration(node);
  }

  @Override
  public R visitClassExtendsClause(ClassExtendsClause node) {
    return visitNode(node);
  }

  @Override
  public R visitComment(Comment node) {
    return visitNode(node);
  }

  @Override
  public R visitCommentReference(CommentReference node) {
    return visitNode(node);
  }

  @Override
  public R visitCompilationUnit(CompilationUnit node) {
    return visitNode(node);
  }

  public R visitCompilationUnitMember(CompilationUnitMember node) {
    return visitDeclaration(node);
  }

  @Override
  public R visitConditionalExpression(ConditionalExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitConstructorDeclaration(ConstructorDeclaration node) {
    return visitTypeMember(node);
  }

  @Override
  public R visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    return visitConstructorInitializer(node);
  }

  public R visitConstructorInitializer(ConstructorInitializer node) {
    return visitNode(node);
  }

  @Override
  public R visitContinueStatement(ContinueStatement node) {
    return visitStatement(node);
  }

  public R visitDeclaration(Declaration node) {
    return visitNode(node);
  }

  @Override
  public R visitDefaultClause(DefaultClause node) {
    return visitNode(node);
  }

  public R visitDirective(Directive node) {
    return visitNode(node);
  }

  @Override
  public R visitDoStatement(DoStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitDoubleLiteral(DoubleLiteral node) {
    return visitLiteral(node);
  }

  @Override
  public R visitEmptyStatement(EmptyStatement node) {
    return visitStatement(node);
  }

  public R visitExpression(Expression node) {
    return visitNode(node);
  }

  @Override
  public R visitExpressionFunctionBody(ExpressionFunctionBody node) {
    return visitFunctionBody(node);
  }

  @Override
  public R visitExpressionStatement(ExpressionStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitFieldDeclaration(FieldDeclaration node) {
    return visitTypeMember(node);
  }

  @Override
  public R visitFieldFormalParameter(FieldFormalParameter node) {
    return visitNormalFormalParameter(node);
  }

  @Override
  public R visitForEachStatement(ForEachStatement node) {
    return visitStatement(node);
  }

  public R visitFormalParameter(FormalParameter node) {
    return visitNode(node);
  }

  @Override
  public R visitFormalParameterList(FormalParameterList node) {
    return visitNode(node);
  }

  @Override
  public R visitForStatement(ForStatement node) {
    return visitStatement(node);
  }

  public R visitFunctionBody(FunctionBody node) {
    return visitNode(node);
  }

  @Override
  public R visitFunctionDeclaration(FunctionDeclaration node) {
    return visitNode(node);
  }

  @Override
  public R visitFunctionDeclarationStatement(FunctionDeclarationStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitFunctionExpression(FunctionExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    return visitExpression(node);
  }

  @Override
  public R visitFunctionTypedFormalParameter(FunctionTypedFormalParameter node) {
    return visitNormalFormalParameter(node);
  }

  public R visitIdentifier(Identifier node) {
    return visitExpression(node);
  }

  @Override
  public R visitIfStatement(IfStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitImplementsClause(ImplementsClause node) {
    return visitNode(node);
  }

  public R visitImportCombinator(ImportCombinator node) {
    return visitNode(node);
  }

  @Override
  public R visitImportDirective(ImportDirective node) {
    return visitDirective(node);
  }

  @Override
  public R visitImportExportCombinator(ImportExportCombinator node) {
    return visitImportCombinator(node);
  }

  @Override
  public R visitImportHideCombinator(ImportHideCombinator node) {
    return visitImportCombinator(node);
  }

  @Override
  public R visitImportPrefixCombinator(ImportPrefixCombinator node) {
    return visitImportCombinator(node);
  }

  @Override
  public R visitImportShowCombinator(ImportShowCombinator node) {
    return visitImportCombinator(node);
  }

  @Override
  public R visitInstanceCreationExpression(InstanceCreationExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitIntegerLiteral(IntegerLiteral node) {
    return visitLiteral(node);
  }

  @Override
  public R visitInterfaceDeclaration(InterfaceDeclaration node) {
    return visitTypeDeclaration(node);
  }

  @Override
  public R visitInterfaceExtendsClause(InterfaceExtendsClause node) {
    return visitNode(node);
  }

  public R visitInterpolationElement(InterpolationElement node) {
    return visitNode(node);
  }

  @Override
  public R visitInterpolationExpression(InterpolationExpression node) {
    return visitInterpolationElement(node);
  }

  @Override
  public R visitInterpolationString(InterpolationString node) {
    return visitInterpolationElement(node);
  }

  @Override
  public R visitIsExpression(IsExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitLabel(Label node) {
    return visitNode(node);
  }

  @Override
  public R visitLabeledStatement(LabeledStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitLibraryDirective(LibraryDirective node) {
    return visitDirective(node);
  }

  @Override
  public R visitListLiteral(ListLiteral node) {
    return visitTypedLiteral(node);
  }

  public R visitLiteral(Literal node) {
    return visitExpression(node);
  }

  @Override
  public R visitMapLiteral(MapLiteral node) {
    return visitTypedLiteral(node);
  }

  @Override
  public R visitMapLiteralEntry(MapLiteralEntry node) {
    return visitNode(node);
  }

  @Override
  public R visitMethodDeclaration(MethodDeclaration node) {
    return visitTypeMember(node);
  }

  @Override
  public R visitMethodInvocation(MethodInvocation node) {
    return visitNode(node);
  }

  @Override
  public R visitNamedExpression(NamedExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitNamedFormalParameter(NamedFormalParameter node) {
    return visitFormalParameter(node);
  }

//  public R visitNativeDirective(NativeDirective node) {
//    return visitDirective(node);
//  }
//
//  public R visitNativeFunctionBody(NativeFunctionBody node) {
//    return visitFunctionBody(node);
//  }

  public R visitNode(ASTNode node) {
    node.visitChildren(this);
    return null;
  }

  public R visitNormalFormalParameter(NormalFormalParameter node) {
    return visitFormalParameter(node);
  }

  @Override
  public R visitNullLiteral(NullLiteral node) {
    return visitLiteral(node);
  }

  @Override
  public R visitParenthesizedExpression(ParenthesizedExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitPostfixExpression(PostfixExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitPrefixedIdentifier(PrefixedIdentifier node) {
    return visitIdentifier(node);
  }

  @Override
  public R visitPrefixExpression(PrefixExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitPropertyAccess(PropertyAccess node) {
    return visitExpression(node);
  }

  @Override
  public R visitResourceDirective(ResourceDirective node) {
    return visitDirective(node);
  }

  @Override
  public R visitReturnStatement(ReturnStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitScriptTag(ScriptTag scriptTag) {
    return visitNode(scriptTag);
  }

  @Override
  public R visitSimpleFormalParameter(SimpleFormalParameter node) {
    return visitNormalFormalParameter(node);
  }

  @Override
  public R visitSimpleIdentifier(SimpleIdentifier node) {
    return visitIdentifier(node);
  }

  @Override
  public R visitSingleStringLiteral(SimpleStringLiteral node) {
    return visitStringLiteral(node);
  }

  @Override
  public R visitSourceDirective(SourceDirective node) {
    return visitDirective(node);
  }

  public R visitStatement(Statement node) {
    return visitNode(node);
  }

  @Override
  public R visitStringInterpolation(StringInterpolation node) {
    return visitStringLiteral(node);
  }

  public R visitStringLiteral(StringLiteral node) {
    return visitLiteral(node);
  }

  @Override
  public R visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    return visitConstructorInitializer(node);
  }

  @Override
  public R visitSuperExpression(SuperExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitSwitchCase(SwitchCase node) {
    return visitSwitchMember(node);
  }

  @Override
  public R visitSwitchDefault(SwitchDefault node) {
    return visitSwitchMember(node);
  }

  public R visitSwitchMember(SwitchMember node) {
    return visitNode(node);
  }

  @Override
  public R visitSwitchStatement(SwitchStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitThisExpression(ThisExpression node) {
    return visitExpression(node);
  }

  @Override
  public R visitThrowStatement(ThrowStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitTryStatement(TryStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitTypeAlias(TypeAlias node) {
    return visitCompilationUnitMember(node);
  }

  @Override
  public R visitTypeArguments(TypeArgumentList node) {
    return visitNode(node);
  }

  public R visitTypeDeclaration(TypeDeclaration node) {
    return visitCompilationUnitMember(node);
  }

  public R visitTypedLiteral(TypedLiteral node) {
    return visitLiteral(node);
  }

  public R visitTypeMember(TypeMember node) {
    return visitDeclaration(node);
  }

  @Override
  public R visitTypeName(TypeName node) {
    return visitNode(node);
  }

  @Override
  public R visitTypeParameter(TypeParameter node) {
    return visitNode(node);
  }

  @Override
  public R visitTypeParameterList(TypeParameterList node) {
    return visitNode(node);
  }

  @Override
  public R visitVariableDeclaration(VariableDeclaration node) {
    return visitDeclaration(node);
  }

  @Override
  public R visitVariableDeclarationList(VariableDeclarationList node) {
    return visitNode(node);
  }

  @Override
  public R visitVariableDeclarationStatement(VariableDeclarationStatement node) {
    return visitStatement(node);
  }

  @Override
  public R visitWhileStatement(WhileStatement node) {
    return visitStatement(node);
  }
}