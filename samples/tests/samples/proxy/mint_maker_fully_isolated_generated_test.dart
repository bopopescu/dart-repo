// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// IsolateStubs=MintMakerFullyIsolatedTest.dart:Mint,Purse,PowerfulPurse
#library("MintMakerFullyIsolatedTest_generatedTest");
#import("dart:isolate");
#import("../../../proxy/promise.dart");
#import("../../../../lib/unittest/unittest.dart");

/* class = Purse (tests/stub-generator/src/MintMakerFullyIsolatedTest.dart/MintMakerFullyIsolatedTest.dart: 9) */

interface Purse$Proxy extends Proxy {
  Promise<int> queryBalance();

  Purse$Proxy sproutPurse();

  Promise<int> deposit(int amount, Purse$Proxy source);
}

class Purse$ProxyImpl extends ProxyImpl implements Purse$Proxy {
  Purse$ProxyImpl(Promise<SendPort> port) : super.forReply(port) { }
  Purse$ProxyImpl.forIsolate(Proxy isolate) : super.forReply(isolate.call([null])) { }
  factory Purse$ProxyImpl.createIsolate() {
    Proxy isolate = new Proxy.forIsolate(new Purse$Dispatcher$Isolate());
    return new Purse$ProxyImpl.forIsolate(isolate);
  }
  factory Purse$ProxyImpl.localProxy(Purse obj) {
    return new Purse$ProxyImpl(new Promise<SendPort>.fromValue(Dispatcher.serve(new Purse$Dispatcher(obj))));
  }

  Promise<int> queryBalance() {
    return this.call(["queryBalance"]);
  }

  Purse$Proxy sproutPurse() {
    return new Purse$ProxyImpl(new PromiseProxy<SendPort>(this.call(["sproutPurse"])));
  }

  Promise<int> deposit(int amount, Purse$Proxy source) {
    return new PromiseProxy<int>(this.call(["deposit", amount, source]));
  }
}

class Purse$Dispatcher extends Dispatcher<Purse> {
  Purse$Dispatcher(Purse thing) : super(thing) { }

  void process(var message, void reply(var response)) {
    String command = message[0];
    if (command == "Purse") {
    } else if (command == "queryBalance") {
      int queryBalance = target.queryBalance();
      reply(queryBalance);
    } else if (command == "sproutPurse") {
      Purse$Proxy sproutPurse = target.sproutPurse();
      reply(sproutPurse);
    } else if (command == "deposit") {
      int amount = message[1];
      List<Promise<SendPort>> promises = new List<Promise<SendPort>>();
      promises.add(new PromiseProxy<SendPort>(new Promise<SendPort>.fromValue(message[2])));
      Purse$Proxy source = new Purse$ProxyImpl(promises[0]);
      Promise<int> deposit = target.deposit(amount, source);
      reply(deposit);
    } else {
      // TODO(kasperl,benl): Somehow throw an exception instead.
      reply("Exception: command '" + command + "' not understood by Purse.");
    }
  }
}

class Purse$Dispatcher$Isolate extends Isolate {
  Purse$Dispatcher$Isolate() : super() { }

  void main() {
    this.port.receive(void _(var message, SendPort replyTo) {
      Purse thing = new Purse();
      SendPort port = Dispatcher.serve(new Purse$Dispatcher(thing));
      Proxy proxy = new Proxy.forPort(replyTo);
      proxy.send([port]);
    });
  }
}

/* class = PowerfulPurse (tests/stub-generator/src/MintMakerFullyIsolatedTest.dart/MintMakerFullyIsolatedTest.dart: 18) */

interface PowerfulPurse$Proxy extends Proxy {
  void init(Mint$Proxy mint, int balance);

  Promise<int> grab(int amount);

  Purse$Proxy weak();
}

class PowerfulPurse$ProxyImpl extends ProxyImpl implements PowerfulPurse$Proxy {
  PowerfulPurse$ProxyImpl(Promise<SendPort> port) : super.forReply(port) { }
  PowerfulPurse$ProxyImpl.forIsolate(Proxy isolate) : super.forReply(isolate.call([null])) { }
  factory PowerfulPurse$ProxyImpl.createIsolate() {
    Proxy isolate = new Proxy.forIsolate(new PowerfulPurse$Dispatcher$Isolate());
    return new PowerfulPurse$ProxyImpl.forIsolate(isolate);
  }
  factory PowerfulPurse$ProxyImpl.localProxy(PowerfulPurse obj) {
    return new PowerfulPurse$ProxyImpl(new Promise<SendPort>.fromValue(Dispatcher.serve(new PowerfulPurse$Dispatcher(obj))));
  }

  void init(Mint$Proxy mint, int balance) {
    this.send(["init", mint, balance]);
  }

  Promise<int> grab(int amount) {
    return this.call(["grab", amount]);
  }

  Purse$Proxy weak() {
    return new Purse$ProxyImpl(this.call(["weak"]));
  }
}

class PowerfulPurse$Dispatcher extends Dispatcher<PowerfulPurse> {
  PowerfulPurse$Dispatcher(PowerfulPurse thing) : super(thing) { }

  void process(var message, void reply(var response)) {
    String command = message[0];
    if (command == "PowerfulPurse") {
    } else if (command == "init") {
      List<Promise<SendPort>> promises = new List<Promise<SendPort>>();
      promises.add(new PromiseProxy<SendPort>(new Promise<SendPort>.fromValue(message[1])));
      int balance = message[2];
      Mint$Proxy mint = new Mint$ProxyImpl(promises[0]);
      target.init(mint, balance);
    } else if (command == "grab") {
      int amount = message[1];
      int grab = target.grab(amount);
      reply(grab);
    } else if (command == "weak") {
      Purse weak = target.weak();
      SendPort port = Dispatcher.serve(new Purse$Dispatcher(weak));
      reply(port);
    } else {
      // TODO(kasperl,benl): Somehow throw an exception instead.
      reply("Exception: command '" + command + "' not understood by PowerfulPurse.");
    }
  }
}

class PowerfulPurse$Dispatcher$Isolate extends Isolate {
  PowerfulPurse$Dispatcher$Isolate() : super() { }

  void main() {
    this.port.receive(void _(var message, SendPort replyTo) {
      PowerfulPurse thing = new PowerfulPurse();
      SendPort port = Dispatcher.serve(new PowerfulPurse$Dispatcher(thing));
      Proxy proxy = new Proxy.forPort(replyTo);
      proxy.send([port]);
    });
  }
}

/* class = Mint (tests/stub-generator/src/MintMakerFullyIsolatedTest.dart/MintMakerFullyIsolatedTest.dart: 28) */

interface Mint$Proxy extends Proxy {
  Purse$Proxy createPurse(int balance);

  Promise<PowerfulPurse$Proxy> promote(Purse$Proxy purse);
}

class Mint$ProxyImpl extends ProxyImpl implements Mint$Proxy {
  Mint$ProxyImpl(Promise<SendPort> port) : super.forReply(port) { }
  Mint$ProxyImpl.forIsolate(Proxy isolate) : super.forReply(isolate.call([null])) { }
  factory Mint$ProxyImpl.createIsolate() {
    Proxy isolate = new Proxy.forIsolate(new Mint$Dispatcher$Isolate());
    return new Mint$ProxyImpl.forIsolate(isolate);
  }
  factory Mint$ProxyImpl.localProxy(Mint obj) {
    return new Mint$ProxyImpl(new Promise<SendPort>.fromValue(Dispatcher.serve(new Mint$Dispatcher(obj))));
  }

  Purse$Proxy createPurse(int balance) {
    return new Purse$ProxyImpl(new PromiseProxy<SendPort>(this.call(["createPurse", balance])));
  }

  Promise<PowerfulPurse$Proxy> promote(Purse$Proxy purse) {
    return new Promise<PowerfulPurse$Proxy>.fromValue(new PowerfulPurse$ProxyImpl(new PromiseProxy<SendPort>(new PromiseProxy<SendPort>(this.call(["promote", purse])))));
  }
}

class Mint$Dispatcher extends Dispatcher<Mint> {
  Mint$Dispatcher(Mint thing) : super(thing) { }

  void process(var message, void reply(var response)) {
    String command = message[0];
    if (command == "Mint") {
    } else if (command == "createPurse") {
      int balance = message[1];
      Purse$Proxy createPurse = target.createPurse(balance);
      reply(createPurse);
    } else if (command == "promote") {
      List<Promise<SendPort>> promises = new List<Promise<SendPort>>();
      promises.add(new PromiseProxy<SendPort>(new Promise<SendPort>.fromValue(message[1])));
      Purse$Proxy purse = new Purse$ProxyImpl(promises[0]);
      Promise<PowerfulPurse$Proxy> promote = target.promote(purse);
      reply(promote);
    } else {
      // TODO(kasperl,benl): Somehow throw an exception instead.
      reply("Exception: command '" + command + "' not understood by Mint.");
    }
  }
}

class Mint$Dispatcher$Isolate extends Isolate {
  Mint$Dispatcher$Isolate() : super() { }

  void main() {
    this.port.receive(void _(var message, SendPort replyTo) {
      Mint thing = new Mint();
      SendPort port = Dispatcher.serve(new Mint$Dispatcher(thing));
      Proxy proxy = new Proxy.forPort(replyTo);
      proxy.send([port]);
    });
  }
}
interface Purse default PurseImpl{
  Purse();
  int queryBalance();
  Purse$Proxy sproutPurse();
  // The deposit has not completed until the promise completes. If we
  // supported Promise<void> then this could use that.
  Promise<int> deposit(int amount, Purse$Proxy source);
}

interface PowerfulPurse extends Purse default PurseImpl {
  PowerfulPurse();

  void init(Mint$Proxy mint, int balance);
  // Return an int so we can wait for it to complete. Shame we can't
  // have a Promise<void>.
  int grab(int amount);
  Purse weak();
}

interface Mint default MintImpl {
  Mint();

  Purse$Proxy createPurse(int balance);
  Promise<PowerfulPurse$Proxy> promote(Purse$Proxy purse);
}

// Because promises can't be used as keys in maps until they have
// completed, provide a wrapper. Note that if any key promise fails to
// resolve, then get()'s return may also fail to resolve. Right now, a
// Proxy can also be used since it has (kludgily) been made to inherit
// from Promise. Perhaps both Proxy and Promise should inherit from
// Completable?
// Note that we cannot extend Set rather than Collection because, for
// example, Set.remove() returns bool, whereas this will have to
// return Promise<bool>.
class PromiseSet<T extends Promise> implements Collection<T> {

  PromiseSet() {
    _set = new List<T>();
  }

  PromiseSet.fromList(this._set);

  void add(T t) {
    print("ProxySet.add");
    for (T x in _set) {
      if (x === t)
        return;
    }
    if (t.hasValue()) {
      for (T x in _set) {
        if (x.hasValue() && x == t)
          return;
      }
    }
    _set.add(t);
    t.addCompleteHandler((_) {
      // Remove any duplicates.
      _remove(t, 1);
    });
  }

  void _remove(T t, int threshold) {
    print("PromiseSet.remove $threshold");
    int count = 0;
    for (int n = 0; n < _set.length; ++n)
      if (_set[n].hasValue() && _set[n] == t)
        if (++count > threshold) {
          print("  remove $n");
          _set.removeRange(n, 1);
          --n;
        }
  }

  void remove(T t) {
    t.addCompleteHandler((_) {
      _remove(t, 0);
    });
  }

  int get length() => _set.length;
  void forEach(void f(T element)) { _set.forEach(f); }
  PromiseSet<T> filter(bool f(T element)) {
    return new PromiseSet<T>.fromList(_set.filter(f));
  }
  bool every(bool f(T element)) => _set.every(f);
  bool some(bool f(T element)) => _set.some(f);
  bool isEmpty() => _set.isEmpty();
  Iterator<T> iterator() => _set.iterator();

  List<T> _set;
    
} 


class PromiseMap<S extends Promise, T> {

  PromiseMap() {
    _map = new Map<S, T>();
    _incomplete = new PromiseSet<S>();
  }

  T add(S s, T t) {
    print("PromiseMap.add");
    _incomplete.add(s);
    s.addCompleteHandler((_) {
      print("PromiseMap.add move to map");
      _map[s] = t;
      _incomplete.remove(s);
    });
    return t;
  }

  Promise<T> find(S s) {
    print("PromiseMap.find");
    Promise<T> findResult = new Promise<T>();
    s.addCompleteHandler((unused) {
      print("PromiseMap.find s completed");
      T t = _map[s];
      if (t != null) {
        print("  immediate");
        findResult.complete(t);
        return;
      }
      // Otherwise, we need to wait for map[s] to complete...
      int counter = _incomplete.length;
      if (counter == 0) {
        print("  none incomplete");
        findResult.complete(null);
        return;
      }
      findResult.join(_incomplete, bool _(S completed) {
        if (completed != s) {
          if (--counter == 0) {
            print("PromiseMap.find failed");
            findResult.complete(null);
            return true;
          }
          print("PromiseMap.find miss");
          return false;
        }
        print("PromiseMap.find complete");
        findResult.complete(_map[s]);
        return true;
      });
    });
    return findResult;
  }

  PromiseSet<S> _incomplete;
  Map<S, T> _map;

}


class MintImpl implements Mint {

  MintImpl() {
    print('mint');
    if (_power == null)
      _power = new PromiseMap<Purse$Proxy, PowerfulPurse$Proxy>();
  }

  Purse$Proxy createPurse(int balance) {
    print('createPurse');
    PowerfulPurse$ProxyImpl purse =
      new PowerfulPurse$ProxyImpl.createIsolate();
    Mint$Proxy thisProxy = new Mint$ProxyImpl.localProxy(this);
    purse.init(thisProxy, balance);

    Purse$Proxy weakPurse = purse.weak();
    weakPurse.addCompleteHandler((_) {
      print('cP1');
      _power.add(weakPurse, purse);
      print('cP2');
    });
    return weakPurse;
  }

  Promise<PowerfulPurse$Proxy> promote(Purse$Proxy purse) {
    print('promote $purse');
    return _power.find(purse);
  }

  static PromiseMap<Purse$Proxy, PowerfulPurse$Proxy> _power;
}

class PurseImpl implements PowerfulPurse {

  // FIXME(benl): autogenerate constructor, get rid of init(...).
  // Note that this constructor should not exist in the public interface
  // PurseImpl(this._mint, this._balance) { }
  PurseImpl() { }

  init(Mint$Proxy mint, int balance) {
    this._mint = mint;
    this._balance = balance;
  }

  int queryBalance() {
    return _balance;
  }

  Purse$Proxy sproutPurse() {
    print('sprout');
    return _mint.createPurse(0);
  }

  Promise<int> deposit(int amount, Purse$Proxy proxy) {
    print('deposit');
    Promise<PowerfulPurse$Proxy> powerful = _mint.promote(proxy);

    Promise<int> result = new Promise<int>();
    powerful.then((_) {
      Promise<int> grabbed = powerful.value.grab(amount);
      grabbed.then((int grabbedAmount) {
        _balance += grabbedAmount;
        result.complete(_balance);
      });
    });

    return result;
  }

  int grab(int amount) {
    print("grab");
    if (_balance < amount) throw "Not enough dough.";
    _balance -= amount;
    return amount;
  }

  Purse weak() {
    return this;
  }

  Mint$Proxy _mint;
  int _balance;

}


_completesWithValue(Promise promise, var expected) {
  promise.then(expectAsync1((value) {
    Expect.equals(expected, value);
  }));
}

main() {
  test("MintMakerFullyIsolatedTest", () {
    Mint$Proxy mint = new Mint$ProxyImpl.createIsolate();
    Purse$Proxy purse = mint.createPurse(100);
    // FIXME(benl): how do I write this?
    //PowerfulPurse$Proxy power = (PowerfulPurse$Proxy)purse;
    //expectEqualsStr("xxx", power.grab());
    Promise<int> balance = purse.queryBalance();
    _completesWithValue(balance, 100);

    Purse$Proxy sprouted = purse.sproutPurse();
    _completesWithValue(sprouted.queryBalance(), 0);

    Promise<int> done = sprouted.deposit(5, purse);
    Promise<int> d3 = done;
    _completesWithValue(done, 5);
    Promise<int> inner = new Promise<int>();
    Promise<int> inner2 = new Promise<int>();
    // FIXME(benl): it should not be necessary to wait here, I think,
    // but without this, the tests seem to execute prematurely.
    Promise<int> d1 = done.then((val) {
      _completesWithValue(sprouted.queryBalance(), 0 + 5);
      _completesWithValue(purse.queryBalance(), 100 - 5);

      done = sprouted.deposit(42, purse); 
      _completesWithValue(done, 5 + 42);
      Promise<int> d2 = done.then((val_) {
        Promise<int> bal1 = sprouted.queryBalance();
        _completesWithValue(bal1, 0 + 5 + 42);
        bal1.then(expectAsync1((int value) => inner.complete(0)));

        Promise<int> bal2 = purse.queryBalance();
        _completesWithValue(bal2, 100 - 5 - 42);
        bal2.then(expectAsync1((int value) => inner2.complete(0)));
        return 0;
      });
      _completesWithValue(d2, 0);
      return 0;
    });
    _completesWithValue(d1, 0);
    Promise<int> allDone = new Promise<int>();
    allDone.waitFor([d3, inner, inner2], 3);
    allDone.then((_) {
      print("##DONE##");
    });
  });
}
