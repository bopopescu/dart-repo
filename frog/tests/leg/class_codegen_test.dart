// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// Test that parameters keep their names in the output.

#import("compiler_helper.dart");
#import("parser_helper.dart");

final String TEST_ONE = @"""
class A { }
class B { }

main() {
  new A();
  new B();
}
""";

final String TEST_TWO = @"""
class A { }
class B extends A { }

main() {
  new A();
  new B();
}
""";

final String TEST_THREE = @"""
class B extends A { }
class A { }

main() {
  new B();
  new A();
}
""";

final String TEST_FOUR = @"""
class A {
  var x;
}

class B extends A {
  var y;
  var z;
}

main() {
  new B();
}
""";

final String TEST_FIVE = @"""
class A {
  var x;
  A(x) : this.x = x {}
}

main() {
  new A(3);
}
""";

twoClasses() {
  String generated = compileAll(TEST_ONE);
  Expect.isTrue(generated.contains(@'Isolate.$defineClass("A", "Object"'));
  Expect.isTrue(generated.contains(@'Isolate.$defineClass("B", "Object"'));
}

subClass() {
  checkOutput(String generated) {
    Expect.isTrue(generated.contains(@'Isolate.$defineClass("A", "Object"'));
    Expect.isTrue(generated.contains(@'Isolate.$defineClass("B", "A"'));
  }

  checkOutput(compileAll(TEST_TWO));
  checkOutput(compileAll(TEST_THREE));
}

fieldTest() {
  String generated = compileAll(TEST_FOUR);
  Expect.isTrue(generated.contains(@"""
Isolate.$defineClass("B", "A", ["z", "y", "x"],"""));
}

constructor1() {
  String generated = compileAll(TEST_FIVE);
  Expect.isTrue(generated.contains(@"new $.A(x);"));
}

main() {
  twoClasses();
  subClass();
  fieldTest();
  constructor1();
}
