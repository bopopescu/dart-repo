#library('WindowOpenTest');
#import('../../lib/unittest/unittest.dart');
#import('../../lib/unittest/html_config.dart');
#import('dart:html');

main() {
  evaluateJavaScript(code) {
    final scriptTag = new Element.tag('script');
    scriptTag.innerHTML = code;
    document.body.nodes.add(scriptTag);
  }
  evaluateJavaScript('layoutTestController.setCanOpenWindows()');

  useHtmlConfiguration();
  test('TwoArgumentVersion', () {
    Window win = window.open('../resources/pong.html', 'testWindow');
    closeWindow(win);
  });
  test('ThreeArgumentVersion', () {
    Window win = window.open("resources/pong.html", "testWindow", "scrollbars=yes,width=75,height=100");
    closeWindow(win);
  });
}

closeWindow(win) {
  win.close();
  doneHandler() {
    if (!win.closed) {
      window.setTimeout(expectAsync0(doneHandler), 1);
    }
  }
  window.setTimeout(expectAsync0(doneHandler), 1);
}
