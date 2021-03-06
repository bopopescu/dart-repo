#library('Suites.dart');

class Origin {
  final String author;
  final String url;

  const Origin(this.author, this.url);
}

class SuiteDescription {
  final String file;
  final String name;
  final Origin origin;
  final String description;
  final List<String> tags;
  final List testVariants;

  const SuiteDescription(this.file, this.name, this.origin,
                         this.description, this.tags, this.testVariants);
}

class Suites {
  static final JOHN_RESIG = const Origin('John Resig', 'http://ejohn.org/');

  static final CATEGORIES = const {
    // Platform tags
    'js': 'DOM Core Tests (JavaScript)',
    'dart': 'DOM Core Tests (dart)',
    'frog': 'DOM Core Tests (frog)',
    'dart2js': 'DOM Core Tests (dart2js)',

    // Library tags
    'dom': 'DOM Core Tests (dart:dom_deprecated)',
    'html': 'DOM Core Tests (dart:html)',
    'htmlidiomatic': 'DOM Core Tests (dart:html) Idiomatic',
  };

  static final _CORE_TEST_OPTIONS = const [
      // A list of valid combinations for core Dromaeo DOM tests.
      // Each item in the list is a pair of (platform x [variants]).
      const ['js', const ['']],
      const ['dart', const ['html', 'htmlidiomatic']],
      const ['frog', const ['dom', 'html', 'htmlidiomatic']],
      const ['dart2js', const ['dom', 'html', 'htmlidiomatic']],
    ];

  static final _CORE_SUITE_DESCRIPTIONS = const [
      const SuiteDescription(
          'dom-attr.html',
          'DOM Attributes',
          JOHN_RESIG,
          'Setting and getting DOM node attributes',
          const ['attributes'],
          _CORE_TEST_OPTIONS),
      const SuiteDescription(
          'dom-modify.html',
          'DOM Modification',
          JOHN_RESIG,
          'Creating and injecting DOM nodes into a document',
          const ['modify'],
          _CORE_TEST_OPTIONS),
      const SuiteDescription(
          'dom-query.html',
          'DOM Query',
          JOHN_RESIG,
          'Querying DOM elements in a document',
          const ['query'],
          _CORE_TEST_OPTIONS),
      const SuiteDescription(
          'dom-traverse.html',
          'DOM Traversal',
          JOHN_RESIG,
          'Traversing a DOM structure',
          const ['traverse'],
          _CORE_TEST_OPTIONS),
  ];

  // Mappings from original path to actual path given platform/library.
  static _getHtmlPathForVariant(platform, lib, path) {
    if (lib != '') {
      lib = '-$lib';
    }
    switch (platform) {
      case 'js':
      case 'dart':
        return path.replaceFirst('.html', '$lib.html');
      case 'frog':
      case 'dart2js':
        return '$platform/${path.replaceFirst(".html", "$lib-js.html")}';
    }
  }

  static var _SUITE_DESCRIPTIONS;

  static List<SuiteDescription> get SUITE_DESCRIPTIONS() {
    if (null !== _SUITE_DESCRIPTIONS) {
      return _SUITE_DESCRIPTIONS;
    }
    _SUITE_DESCRIPTIONS = <SuiteDescription>[];

    // Expand the list to include a unique SuiteDescription for each
    // tested variant.
    for (SuiteDescription suite in _CORE_SUITE_DESCRIPTIONS) {
      List variants = suite.testVariants;
      for (List variant in variants) {
        assert(variant.length == 2);
        String platform = variant[0];
        List<String> libraries = variant[1];
        for(String lib in libraries) {
          String path = _getHtmlPathForVariant(platform, lib, suite.file);
          final combined = new List.from(suite.tags);
          combined.add(platform);
          if (lib != '') {
            combined.add(lib);
            lib = ':$lib';
          }
          final name = null === variant
              ? suite.name
              : '${suite.name} ($platform$lib)';
          _SUITE_DESCRIPTIONS.add(new SuiteDescription(
                                      path,
                                      name,
                                      suite.origin,
                                      suite.description,
                                      combined,
                                      []));
        }
      }
    }

    return _SUITE_DESCRIPTIONS;
  }

  static List<SuiteDescription> getSuites(String tags) {
    // A disjunction of conjunctions (e.g.,
    // 'js&modify|dart&dom&modify').
    final taglist = tags.split('|').map((tag) => tag.split('&'));

    bool match(suite) {
      // If any conjunction matches, return true.
      for (final tagset in taglist) {
        if (tagset.every((tag) => suite.tags.indexOf(tag) >= 0)) {
          return true;
        }
      }
      return false;
    }
    final suites = SUITE_DESCRIPTIONS.filter(match);

    suites.sort((s1, s2) => s1.name.compareTo(s2.name));
    return suites;
  }

  static getCategory(String tags) {
    if (CATEGORIES.containsKey(tags)) {
      return CATEGORIES[tags];
    }
    for (final suite in _CORE_SUITE_DESCRIPTIONS) {
      if (suite.tags[0] == tags) {
        return suite.name;
      }
    }
    return null;
  }
}
