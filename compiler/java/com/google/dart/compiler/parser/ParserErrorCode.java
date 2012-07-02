// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.parser;

import com.google.dart.compiler.ErrorCode;
import com.google.dart.compiler.ErrorSeverity;
import com.google.dart.compiler.SubSystem;

/**
 * {@link ErrorCode}s for parser.
 * <p>
 * The convention in this file (with some exceptions) is that the enumeration name matches at least
 * the beginning default English translation of the message.
 */
public enum ParserErrorCode implements ErrorCode {
  // TODO(zundel): error message needs JUnit test
  ABSTRACT_MEMBER_IN_INTERFACE("Abstract members are not allowed in interfaces"),
  ABSTRACT_METHOD_WITH_BODY("Abstract method cannot have a body"),
  ABSTRACT_TOP_LEVEL_ELEMENT("Only class can be abstract top-level element"),
  BREAK_OUTSIDE_OF_LOOP("'break' used outside of loop, case statement"),
  BUILT_IN_IDENTIFIER_AS_TYPE_NAME("Built-in identifier cannot be used as type name"),
  BUILT_IN_IDENTIFIER_AS_TYPEDEF_NAME("Built-in identifier can not be used as type alias name"),
  BUILT_IN_IDENTIFIER_AS_TYPE_VARIABLE_NAME("Built-in identifier can not be used as type variable name"),
  CATCH_OR_FINALLY_EXPECTED("catch or finally clause expected."),
  CONTINUE_IN_CASE_MUST_HAVE_LABEL("'continue' in case statement must have a label as a target."),
  CONTINUE_OUTSIDE_OF_LOOP("'continue' used outside of loop or case statement"),
  DEFAULT_VALUE_CAN_NOT_BE_SPECIFIED_IN_CLOSURE(
      "Default values cannot be specified in closure parameter"),
  DEFAULT_VALUE_CAN_NOT_BE_SPECIFIED_IN_TYPEDEF(
      "Default values cannot be specified in closure type definition"),
  // TODO(zundel): error message needs JUnit test
  DEFAULT_POSITIONAL_PARAMETER("Positional parameters cannot have default values"),
  DEPRECATED_USE_OF_FACTORY_KEYWORD("Deprecated use of the 'factory' keyword: use 'default' instead"),
  DIRECTIVE_OUT_OF_ORDER("Directive out of order"),
  // TODO(zundel): error message needs JUnit test
  DISALLOWED_ABSTRACT_KEYWORD("Abstract keyword not allowed here"),
  DISALLOWED_FACTORY_KEYWORD("Factory keyword not allowed here"),
  // TODO(zundel): error message needs JUnit test
  EXPECTED_ARRAY_OR_MAP_LITERAL("Expected array or map literal"),
  // TODO(zundel): error message needs JUnit test
  EXPECTED_CASE_OR_DEFAULT("Expected 'case' or 'default'"),
  EXPECTED_CLASS_DECLARATION_LBRACE("Expected '{' in class or interface declaration"),
  // TODO(zundel): error message needs JUnit test
  EXPECTED_COMMA_OR_RIGHT_BRACE("Expected ',' or '}'"),
  EXPECTED_COMMA_OR_RIGHT_PAREN("Expected ',' or ')', but got '%s'"),
  // TODO(zundel): error message needs JUnit test
  EXPECTED_EOS("Unexpected token '%s' (expected end of file)"),
  // TODO(zundel): error message needs JUnit test
  EXPECTED_EXPRESSION_AFTER_COMMA("Expected expression after comma"),
  EXPECTED_EXTENDS("Expected 'extends'"),
  // TODO(zundel): error message needs JUnit test
  EXPECTED_IDENTIFIER("Expected identifier"),
  // TODO(zundel): error message needs JUnit test
  EXPECTED_LEFT_PAREN("'(' expected"),
  // TODO(zundel): error message needs JUnit test
  EXPECTED_PREFIX_KEYWORD("Expected 'prefix' after comma"),
  // TODO(zundel): error message needs JUnit test
  EXPECTED_PREFIX_IDENTIFIER("Prefix string can only contain valid identifier characters"),
  // TODO(zundel): error message needs JUnit test
  EXPECTED_SEMICOLON("Expected ';'"),
  // TODO(zundel): error message needs JUnit test
  EXPECTED_STRING_LITERAL("Expected string literal"),
  // TODO(zundel): error message needs JUnit test
  EXPECTED_STRING_LITERAL_MAP_ENTRY_KEY("Expected string literal for map entry key"),
  EXPECTED_TOKEN("Unexpected token '%s' (expected '%s')"),
  // TODO(zundel): error message needs JUnit test
  EXPECTED_VAR_FINAL_OR_TYPE("Expected 'var', 'final' or type"),
  // TODO(zundel): this error message is out of date
  NATIVE_ONLY_CLASS("Native keyword can be specified only for classes"),
  NATIVE_ONLY_CORE_LIB("Native keyword can be used only in corelib"),
  // TODO(zundel): error message needs JUnit test
  FACTORY_CANNOT_BE_ABSTRACT("A factory cannot be abstract"),
  // TODO(zundel): error message needs JUnit test
  FACTORY_CANNOT_BE_STATIC("A factory cannot be static"),
  // TODO(zundel): error message needs JUnit test
  FACTORY_MEMBER_IN_INTERFACE("Factory members are not allowed in interfaces"),
  FINAL_IS_NOT_ALLOWED_ON_A_METHOD_DEFINITION("'final' is not allowed on a method definition"),
  // TODO(zundel): error message needs JUnit test
  FOR_IN_WITH_COMPLEX_VARIABLE("Only simple variables can be assigned to in a for-in construct"),
  // TODO(zundel): error message needs JUnit test
  FOR_IN_WITH_MULTIPLE_VARIABLES("Too many variable declarations in a for-in construct"),
  // TODO(zundel): error message needs JUnit test
  FOR_IN_WITH_VARIABLE_INITIALIZER("Cannot initialize for-in variables"),
  // TODO(zundel): error message needs JUnit test
  FUNCTION_TYPED_PARAMETER_IS_FINAL("Formal parameter with a function type cannot be const"),
  // TODO(zundel): error message needs JUnit test
  FUNCTION_TYPED_PARAMETER_IS_VAR("Formal parameter with a function type cannot be var"),
  FUNCTION_NAME_EXPECTED_IDENTIFIER("Function name expected to be an identifier"),
  // TODO(zundel): error message needs JUnit test
  ILLEGAL_ASSIGNMENT_TO_NON_ASSIGNABLE("Illegal assignment to non-assignable expression"),
  // TODO(zundel): error message needs JUnit test
  ILLEGAL_NUMBER_OF_PARAMETERS("Illegal number of parameters"),
  INCOMPLETE_STRING_LITERAL("Incomplete string literal"),
  INTERFACE_METHOD_WITH_BODY("Interface method cannot have a body"),
  // TODO(zundel): error message needs JUnit test
  INVALID_FIELD_DECLARATION("Wrong syntax for field declaration"),
  INVALID_IDENTIFIER("The token '%s' cannot be used as an identifier"),
  // TODO(zundel): error message needs JUnit test
  INVALID_OPERATOR_CHAINING("Cannot chain '%s'"),
  LABEL_NOT_FOLLOWED_BY_CASE_OR_DEFAULT("Label not followed by 'case', 'default', or statement"),
  LOCAL_CANNOT_BE_STATIC("Local function cannot be static"),
  MISSING_FUNCTION_NAME(ErrorSeverity.WARNING, "a function name is required for a declaration"),
  NAMED_PARAMETER_NOT_ALLOWED("Named parameter is not allowed for operator or setter method"),
  NO_SPACE_AFTER_PLUS("Cannot have space between plus and numeric literal"),
  NO_UNARY_PLUS_OPERATOR("No unary plus operator in Dart"),
  // TODO(zundel): error message needs JUnit test
  NON_FINAL_STATIC_MEMBER_IN_INTERFACE("Non-final static members are not allowed in interfaces"),
  ONLY_ONE_LIBRARY_DIRECTIVE("Only one library directive may be declared in a file"),
  // TODO(zundel): error message needs JUnit test
  OPERATOR_CANNOT_BE_STATIC("Operators cannot be static"),
  OPERATOR_IS_NOT_USER_DEFINABLE("Operator is not user definable"),
  POSITIONAL_AFTER_NAMED_ARGUMENT("Positional argument after named argument"),
  REDIRECTING_CONSTRUCTOR_CANNOT_HAVE_A_BODY("Redirecting constructor cannot have a body"),
  REDIRECTING_CONSTRUCTOR_PARAM("Redirecting constructor cannot have initializers"),
  REDIRECTING_CONSTRUCTOR_ITSELF("Redirecting constructor cannot have initializers"),
  REDIRECTING_CONSTRUCTOR_MULTIPLE("Multiple redirecting constructor invocations"),
  REDIRECTING_CONSTRUCTOR_OTHER("Redirecting constructor cannot have initializers"),
  SUPER_CONSTRUCTOR_MULTIPLE("'super' must be called only once in the initialization list"),
  SUPER_CANNOT_BE_USED_AS_THE_SECOND_OPERAND(
      "'super' cannot be used as the second operand in a binary expression."),
  SUPER_IS_NOT_VALID_AS_A_BOOLEAN_OPERAND("'super' is not valid as a boolean operand"),
  SUPER_IS_NOT_VALID_ALONE_OR_AS_A_BOOLEAN_OPERAND(
      "'super' is not valid alone or as a boolean operand"),
  TOP_LEVEL_CANNOT_BE_STATIC("Top-level field or method cannot be static"),
  UNREACHABLE_CODE_IN_CASE(ErrorSeverity.WARNING, "Unreachable code in case statement"),
  UNEXPECTED_TOKEN("Unexpected token '%s'"),
  UNEXPECTED_TOKEN_IN_STRING_INTERPOLATION("Unexpected token in string interpolation: %s"),
  // TODO(zundel): error message needs JUnit test
  UNEXPECTED_TYPE_ARGUMENT("unexpected type argument"),
  VAR_IS_NOT_ALLOWED_ON_A_METHOD_DEFINITION("'var' is not allowed on a method definition"),
  // TODO(zundel): error message needs JUnit test
  VOID_FIELD("Field cannot be of type void"),
  // TODO(zundel): error message needs JUnit test
  VOID_PARAMETER("Parameter cannot be of type void");

  private final ErrorSeverity severity;
  private final String message;

  /**
   * Initialize a newly created error code to have the given message and ERROR severity.
   */
  private ParserErrorCode(String message) {
    this(ErrorSeverity.ERROR, message);
  }

  /**
   * Initialize a newly created error code to have the given severity and message.
   */
  private ParserErrorCode(ErrorSeverity severity, String message) {
    this.severity = severity;
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public ErrorSeverity getErrorSeverity() {
    return severity;
  }

  @Override
  public SubSystem getSubSystem() {
    return SubSystem.PARSER;
  }

  @Override
  public boolean needsRecompilation() {
    return true;
  }
}