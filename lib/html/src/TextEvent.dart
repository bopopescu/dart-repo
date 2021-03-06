// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

interface TextEvent extends UIEvent default TextEventWrappingImplementation {

  TextEvent(String type, Window view, String data, [bool canBubble,
      bool cancelable]);

  String get data();
}
