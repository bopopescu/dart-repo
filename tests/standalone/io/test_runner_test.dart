// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import("dart:io");
#import("../../../tools/testing/dart/test_runner.dart");
#import("../../../tools/testing/dart/status_file_parser.dart");
#import("../../../tools/testing/dart/test_options.dart");
#source("process_test_util.dart");

class TestController {
  static final int numTests = 4;
  static int numCompletedTests = 0;

  // Used as TestCase.completedCallback.
  static processCompletedTest(TestCase testCase) {
    TestOutput output = testCase.output;
    print("Test: ${testCase.commands.last().commandLine}");
    if (output.unexpectedOutput) {
      throw "Unexpected output: ${output.result}";
    }
    print("stdout: ");
    for (var line in output.stdout) print(line);
    print("stderr: ");
    for (var line in output.stderr) print(line);

    print("Time: ${output.time}");
    print("Exit code: ${output.exitCode}");

    ++numCompletedTests;
    print("$numCompletedTests/$numTests");
    if (numCompletedTests == numTests) {
      print("test_runner_test.dart PASSED");
    }
  }
}


TestCase MakeTestCase(String testName, List<String> expectations) {
  String test_path = "tests/standalone/${testName}.dart";
  // Working directory may be dart/runtime rather than dart.
  if (!new File(test_path).existsSync()) {
    test_path = "../tests/standalone/${testName}.dart";
  }

  var configuration = new TestOptionsParser().parse(['--timeout', '2'])[0];
  return new TestCase(testName,
                      [new Command(new Options().executable,
                                   <String>["--ignore-unrecognized-flags",
                                            "--enable_type_checks",
                                            test_path])],
                      configuration,
                      TestController.processCompletedTest,
                      new Set<String>.from(expectations));
}


void main() {
  new RunningProcess(MakeTestCase("pass_test", [PASS])).start();
  new RunningProcess(MakeTestCase("fail_test", [FAIL])).start();
  new RunningProcess(MakeTestCase("timeout_test", [TIMEOUT])).start();

  // The crash test sometimes times out.  Run it with a large timeout to help
  // diagnose the delay.
  // The test loads a new executable, which may sometimes take a long time.
  // It involves a wait on the VM event loop, and possible system delays.
  var configuration = new TestOptionsParser().parse(['--timeout', '60'])[0];
  new RunningProcess(new TestCase("CrashTest",
                                  [new Command(getProcessTestFileName(),
                                               const ["0", "0", "1", "1"])],
                                  configuration,
                                  TestController.processCompletedTest,
                                  new Set<String>.from([CRASH]))).start();
  Expect.equals(4, TestController.numTests);
  // Throw must be from body of start() function for this test to work.
  Expect.throws(new RunningProcess(MakeTestCase("pass_test", [SKIP])).start);
}
