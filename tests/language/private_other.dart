// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
// Dart test for testing access to private fields.

class PrivateOther {

  final _myPrecious;

  const PrivateOther() : this._myPrecious = "Another Ring";

}
