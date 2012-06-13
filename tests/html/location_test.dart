#library('LocationTest');
#import('../../lib/unittest/unittest.dart');
#import('../../lib/unittest/html_config.dart');
#import('dart:html');

main() {
  useHtmlConfiguration();

  test('location hash', () {
      final location = window.location;
      Expect.isTrue(location is Location);

      // The only navigation we dare try is hash.
      location.hash = 'hello';
      var h = location.hash;
      Expect.equals('#hello', h);
    });
}
