const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '../base/api-spotify/src/main/resources/bridge/catalogue-bridge.js'), 'utf8');

function session(anonymous = true, registryAvailable = true) {
  const messages = [];
  const resolved = [];
  const pending = [];
  let tokenReads = 0;
  const tokenKey = { description: 'TokenProvider' };
  const platform = {
    version: 'test-web',
    getRegistry: () => ({
      _map: new Map([[tokenKey, null]]),
      resolve: key => {
        resolved.push(key.description);
        return { getToken: async () => {
          tokenReads++;
          return { accessToken: 'private-test-token', accessTokenExpirationTimestampMs: 123456, clientId: 'test-client', isAnonymous: anonymous };
        } };
      },
    }),
  };
  const root = { querySelectorAll: () => [] };
  if (registryAvailable) root.__reactContainer$test = { memoizedProps: { value: platform } };
  const context = {
    window: { DistortionCatalogue: { postMessage: value => messages.push(JSON.parse(value)) } },
    document: { getElementById: () => root },
    setTimeout: callback => pending.push(callback),
  };
  vm.runInNewContext(source, context, { timeout: 1000 });
  return { messages, resolved, pending, call: context.window.Myspoti.call, tokenReads: () => tokenReads };
}

test('anonymous bootstrap resolves no playback or library services', async () => {
  const value = session();
  assert.deepEqual(value.resolved, ['TokenProvider']);
  assert.equal(value.messages[0].type, 'ready');
  await value.call(1, 'token.get');
  assert.equal(value.messages[1].ok, true);
  assert.equal(value.messages[1].value.isAnonymous, true);
});

test('unsupported commands never reach the token or service registry', async () => {
  const value = session();
  for (const method of ['player.play', 'gql.query', 'library.add', 'unknown']) {
    await value.call(1, method, {});
    assert.equal(value.messages.at(-1).ok, false);
    assert.equal(value.messages.at(-1).value, undefined);
  }
  assert.equal(value.tokenReads(), 0);
  assert.deepEqual(value.resolved, ['TokenProvider']);
});

test('signed-in and missing anonymous markers never disclose credentials', async () => {
  for (const anonymous of [false, null]) {
    const value = session(anonymous);
    await value.call(1, 'token.get');
    assert.equal(value.messages.at(-1).ok, false);
    assert.equal(JSON.stringify(value.messages).includes('private-test-token'), false);
  }
});

test('a changed registry produces a bounded failure rather than fake readiness', () => {
  const value = session(true, false);
  let steps = 0;
  while (value.pending.length && steps < 100) { value.pending.shift()(); steps++; }
  assert.equal(value.pending.length, 0);
  assert.ok(steps < 50);
  assert.equal(value.messages.some(message => message.type === 'ready'), false);
  assert.equal(value.messages.at(-1).type, 'error');
});
