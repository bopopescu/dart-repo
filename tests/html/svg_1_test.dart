#library('SVG1Test');
#import('../../lib/unittest/unittest.dart');
#import('../../lib/unittest/html_config.dart');
#import('dart:html');

// Test that SVG is present in dart:html API

main() {
  useHtmlConfiguration();

  test('simpleRect', () {
      var div = new Element.tag('div');
      document.body.nodes.add(div);
      div.innerHTML = @'''
<svg id='svg1' width='200' height='100'>
<rect id='rect1' x='10' y='20' width='130' height='40' rx='5'fill='blue'></rect>
</svg>
''';

    var e = document.query('#svg1');
    Expect.isTrue(e != null);

    SVGRectElement r = document.query('#rect1');
    Expect.equals(10, r.x.baseVal.value);
    Expect.equals(20, r.y.baseVal.value);
    Expect.equals(40, r.height.baseVal.value);
    Expect.equals(130, r.width.baseVal.value);
    Expect.equals(5, r.rx.baseVal.value);
  });
}
