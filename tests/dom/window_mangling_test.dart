// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('WindowManglingTest');
#import('../../lib/unittest/unittest.dart');
#import('../../lib/unittest/dom_config.dart');
#import('dart:dom_deprecated', prefix: 'dom');

// Defined in dom.Window.
get navigator() => "Dummy";

$eq(x, y) => false;
$eq$(x, y) => false;

main() {
  useDomConfiguration();
  var win = dom.window;

  test('windowMethod', () {
      final message = navigator;
      final x = win.navigator;
      Expect.notEquals(message, x);
    });

  test('windowEquals', () {
      Expect.isFalse($eq(win, win));
      Expect.isTrue(win == win);
    });

  test('windowEquals', () {
      Expect.isFalse($eq$(win, win));
      Expect.isTrue(win == win);
    });
}
