// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Test infrastructure for testing pub. Unlike typical unit tests, most pub
 * tests are integration tests that stage some stuff on the file system, run
 * pub, and then validate the results. This library provides an API to build
 * tests like that.
 */
#library('test_pub');

#import('dart:io');

#import('../../../lib/unittest/unittest.dart');
#import('../../lib/file_system.dart', prefix: 'fs');
#import('../../pub/io.dart');

/**
 * Creates a new [FileDescriptor] with [name] and [contents].
 */
FileDescriptor file(String name, String contents) =>
    new FileDescriptor(name, contents);

/**
 * Creates a new [DirectoryDescriptor] with [name] and [contents].
 */
DirectoryDescriptor dir(String name, [List<Descriptor> contents]) =>
    new DirectoryDescriptor(name, contents);

/**
 * Creates a new [GitRepoDescriptor] with [name] and [contents].
 */
DirectoryDescriptor git(String name, [List<Descriptor> contents]) =>
    new GitRepoDescriptor(name, contents);

/**
 * The path of the package cache directory used for tests. Relative to the
 * sandbox directory.
 */
final String cachePath = "cache";

/**
 * The path of the mock SDK directory used for tests. Relative to the sandbox
 * directory.
 */
final String sdkPath = "sdk";

/**
 * The path of the mock app directory used for tests. Relative to the sandbox
 * directory.
 */
final String appPath = "myapp";

/**
 * The path of the packages directory in the mock app used for tests. Relative
 * to the sandbox directory.
 */
final String packagesPath = "$appPath/packages";

/**
 * The type for callbacks that will be fired during [runPub]. Takes the sandbox
 * directory as a parameter.
 */
typedef Future _ScheduledEvent(Directory parentDir);

/**
 * The list of events that are scheduled to run after the sandbox directory has
 * been created but before Pub is run.
 */
List<_ScheduledEvent> _scheduledBeforePub;

/**
 * The list of events that are scheduled to run after Pub has been run.
 */
List<_ScheduledEvent> _scheduledAfterPub;

void runPub([List<String> args, Pattern output, Pattern error,
    int exitCode = 0]) {
  var createdSandboxDir;

  var asyncDone = expectAsync0(() {});

  deleteSandboxIfCreated(onDeleted()) {
    _scheduledBeforePub = null;
    _scheduledAfterPub = null;
    if (createdSandboxDir != null) {
      deleteDir(createdSandboxDir).then((_) => onDeleted());
    } else {
      onDeleted();
    }
  }

  String pathInSandbox(path) => join(getFullPath(createdSandboxDir), path);

  final future = _setUpSandbox().chain((sandboxDir) {
    createdSandboxDir = sandboxDir;
    return _runScheduled(sandboxDir, _scheduledBeforePub);
  }).chain((_) {
    return ensureDir(pathInSandbox(appPath));
  }).chain((_) {
    // TODO(rnystrom): Hack in the cache directory path. Should pass this
    // in using environment var once #752 is done.
    args.add('--cachedir=${pathInSandbox(cachePath)}');

    // TODO(rnystrom): Hack in the SDK path. Should pass this in using
    // environment var once #752 is done.
    args.add('--sdkdir=${pathInSandbox(sdkPath)}');

    return _runPub(args, pathInSandbox(appPath));
  }).chain((result) {
    _validateOutput(output, result.stdout);
    _validateOutput(error, result.stderr);

    Expect.equals(result.exitCode, exitCode,
        'Pub returned exit code ${result.exitCode}, expected $exitCode.');

    return _runScheduled(createdSandboxDir, _scheduledAfterPub);
  });

  future.then((_) {
    deleteSandboxIfCreated(asyncDone);
  });

  future.handleException((error) {
    // If an error occurs during testing, delete the sandbox, throw the error so
    // that the test framework sees it, then finally call asyncDone so that the
    // test framework knows we're done doing asynchronous stuff.
    deleteSandboxIfCreated(() {
      guardAsync(() { throw error; }, asyncDone);
    });
    return true;
  });
}


/**
 * Wraps a test that needs git in order to run. This validates that the test is
 * running on a builbot in which case we expect git to be installed. If we are
 * not running on the buildbot, we will instead see if git is installed and
 * skip the test if not. This way, users don't need to have git installed to
 * run the tests locally (unless they actually care about the pub git tests).
 */
void withGit(void callback()) {
  isGitInstalled.then(expectAsync1((installed) {
    if (installed || Platform.environment.containsKey('BUILDBOT_BUILDERNAME')) {
      callback();
    }
  }));
}

Future<Directory> _setUpSandbox() {
  return createTempDir('pub-test-sandbox-');
}

_runScheduled(Directory parentDir, List<_ScheduledEvent> scheduled) {
  if (scheduled == null) return new Future.immediate(null);
  var future = Futures.wait(scheduled.map((event) => event(parentDir)));
  scheduled.clear();
  return future;
}

Future<ProcessResult> _runPub(List<String> pubArgs, String workingDir) {
  // Find a dart executable we can use to run pub. Uses the one that the
  // test infrastructure uses. We are not using new Options.executable here
  // because that gets confused if you invoked Dart through a shell script.
  final scriptDir = new File(new Options().script).directorySync().path;
  final platform = Platform.operatingSystem;
  final dartBin = join(scriptDir, '../../../tools/testing/bin/$platform/dart');

  // Find the main pub entrypoint.
  final pubPath = fs.joinPaths(scriptDir, '../../pub/pub.dart');

  final args = ['--enable-type-checks', '--enable-asserts', pubPath];
  args.addAll(pubArgs);

  return runProcess(dartBin, args, workingDir);
}

/**
 * Compares the [actual] output from running pub with [expected]. For [String]
 * patterns, ignores leading and trailing whitespace differences and tries to
 * report the offending difference in a nice way. For other [Pattern]s, just
 * reports whether the output contained the pattern.
 */
void _validateOutput(Pattern expected, List<String> actual) {
  if (expected == null) return;

  if (expected is String) return _validateOutputString(expected, actual);
  var actualText = Strings.join(actual, "\n");
  if (actualText.contains(expected)) return;
  Expect.fail('Expected output to match "$expected", was:\n$actualText');
}

void _validateOutputString(String expectedText, List<String> actual) {
  final expected = expectedText.split('\n');

  // Strip off the last line. This lets us have expected multiline strings
  // where the closing ''' is on its own line. It also fixes '' expected output
  // to expect zero lines of output, not a single empty line.
  expected.removeLast();

  final length = Math.min(expected.length, actual.length);
  for (var i = 0; i < length; i++) {
    if (expected[i].trim() != actual[i].trim()) {
      Expect.fail(
        'Output line ${i + 1} was: ${actual[i]}\nexpected: ${expected[i]}');
    }
  }

  if (expected.length > actual.length) {
    final message = new StringBuffer();
    message.add('Missing expected output:\n');
    for (var i = actual.length; i < expected.length; i++) {
      message.add(expected[i]);
      message.add('\n');
    }

    Expect.fail(message.toString());
  }

  if (expected.length < actual.length) {
    final message = new StringBuffer();
    message.add('Unexpected output:\n');
    for (var i = expected.length; i < actual.length; i++) {
      message.add(actual[i]);
      message.add('\n');
    }

    Expect.fail(message.toString());
  }
}

/**
 * Base class for [FileDescriptor] and [DirectoryDescriptor] so that a
 * directory can contain a heterogeneous collection of files and
 * subdirectories.
 */
class Descriptor {
  /**
   * The short name of this file or directory.
   */
  final String name;

  Descriptor(this.name);

  /**
   * Creates the file or directory within [dir]. Returns a [Future] that is
   * completed after the creation is done.
   */
  abstract Future create(dir);

  /**
   * Validates that this descriptor correctly matches the corresponding file
   * system entry within [dir]. Returns a [Future] that completes to `null` if
   * the entry is valid, or throws an error if it failed.
   */
  abstract Future validate(String dir);

  /**
   * Schedules the directory to be created before Pub is run with [runPub]. The
   * directory will be created relative to the sandbox directory.
   */
  // TODO(nweiz): Use implicit closurization once issue 2984 is fixed.
  void scheduleCreate() => _scheduleBeforePub((dir) => this.create(dir));

  /**
   * Schedules the directory to be validated after Pub is run with [runPub]. The
   * directory will be validated relative to the sandbox directory.
   */
  void scheduleValidate() =>
    _scheduleAfterPub((parentDir) => validate(parentDir.path));
}

/**
 * Describes a file. These are used both for setting up an expected directory
 * tree before running a test, and for validating that the file system matches
 * some expectations after running it.
 */
class FileDescriptor extends Descriptor {
  /**
   * The text contents of the file.
   */
  final String contents;

  FileDescriptor(String name, this.contents) : super(name);

  /**
   * Creates the file within [dir]. Returns a [Future] that is completed after
   * the creation is done.
   */
  Future<File> create(dir) {
    return writeTextFile(join(dir, name), contents);
  }

  /**
   * Validates that this file correctly matches the actual file at [path].
   */
  Future validate(String path) {
    path = join(path, name);
    return fileExists(path).chain((exists) {
      if (!exists) Expect.fail('Expected file $path does not exist.');

      return readTextFile(path).transform((text) {
        if (text == contents) return null;

        Expect.fail('File $path should contain:\n\n$contents\n\n'
                    'but contained:\n\n$text');
      });
    });
  }
}

/**
 * Describes a directory and its contents. These are used both for setting up
 * an expected directory tree before running a test, and for validating that
 * the file system matches some expectations after running it.
 */
class DirectoryDescriptor extends Descriptor {
  /**
   * The files and directories contained in this directory.
   */
  final List<Descriptor> contents;

  DirectoryDescriptor(String name, this.contents) : super(name);

  /**
   * Creates the file within [dir]. Returns a [Future] that is completed after
   * the creation is done.
   */
  Future<Directory> create(parentDir) {
    final completer = new Completer<Directory>();

    // Create the directory.
    createDir(join(parentDir, name)).then((dir) {
      if (contents == null) {
        completer.complete(dir);
      } else {
        // Recursively create all of its children.
        final childFutures = contents.map((child) => child.create(dir));
        Futures.wait(childFutures).then((_) {
          // Only complete once all of the children have been created too.
          completer.complete(dir);
        });
      }
    });

    return completer.future;
  }

  /**
   * Validates that the directory at [path] contains all of the expected
   * contents in this descriptor. Note that this does *not* check that the
   * directory doesn't contain other unexpected stuff, just that it *does*
   * contain the stuff we do expect.
   */
  Future validate(String path) {
    // Validate each of the items in this directory.
    final entryFutures = contents.map(
        (entry) => entry.validate(join(path, name)));

    // If they are all valid, the directory is valid.
    return Futures.wait(entryFutures).transform((entries) => null);
  }
}

/**
 * Describes a Git repository and its contents.
 */
class GitRepoDescriptor extends DirectoryDescriptor {
  GitRepoDescriptor(String name, List<Descriptor> contents)
  : super(name, contents);

  /**
   * Creates the Git repository and commits the contents.
   */
  Future<Directory> create(parentDir) {
    var workingDir;
    Future runGit(List<String> args) {
      return runProcess('git', args, workingDir: workingDir.path).
        transform((result) {
          if (!result.success) throw "Error running git: ${result.stderr}";
          return null;
        });
    }

    return super.create(parentDir).chain((rootDir) {
      workingDir = rootDir;
      return runGit(['init']);
    }).chain((_) => runGit(['add', '.']))
      .chain((_) => runGit(['commit', '-m', 'initial commit']))
      .transform((_) => workingDir);
  }
}

/**
 * Schedules a callback to be called before Pub is run with [runPub].
 */
void _scheduleBeforePub(_ScheduledEvent event) {
  if (_scheduledBeforePub == null) _scheduledBeforePub = [];
  _scheduledBeforePub.add(event);
}

/**
 * Schedules a callback to be called after Pub is run with [runPub].
 */
void _scheduleAfterPub(_ScheduledEvent event) {
  if (_scheduledAfterPub == null) _scheduledAfterPub = [];
  _scheduledAfterPub.add(event);
}
