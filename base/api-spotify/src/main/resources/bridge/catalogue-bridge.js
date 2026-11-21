(() => {
  if (window.Myspoti) return;
  const post = value => window.DistortionCatalogue.postMessage(JSON.stringify(value));
  let platform;
  let token;
  const find = () => {
    const root = document.getElementById('main') || document.body;
    if (!root) return false;
    for (const element of [root, ...root.querySelectorAll('*')]) {
      const key = Object.keys(element).find(k => k.startsWith('__reactContainer$'));
      if (!key) continue;
      const pending = [element[key]];
      const seen = new Set();
      while (pending.length) {
        const fiber = pending.pop();
        if (!fiber || seen.has(fiber)) continue;
        seen.add(fiber);
        const value = fiber.memoizedProps && fiber.memoizedProps.value;
        if (value && typeof value.getRegistry === 'function') {
          const registry = value.getRegistry();
          const tokenKey = [...registry._map.keys()].find(k => k && k.description === 'TokenProvider');
          if (tokenKey) {
            platform = value;
            token = registry.resolve(tokenKey);
            return true;
          }
        }
        if (fiber.sibling) pending.push(fiber.sibling);
        if (fiber.child) pending.push(fiber.child);
      }
    }
    return false;
  };
  window.Myspoti = {
    call: async (id, method, args) => {
      try {
        if (method !== 'token.get' || !token) throw new Error('Unsupported catalogue command');
        const value = await token.getToken();
        if (value.isAnonymous !== true) throw new Error('Anonymous session required');
        post({type: 'result', id, ok: true, value: {
          accessToken: value.accessToken,
          expiresAtMs: value.accessTokenExpirationTimestampMs,
          clientId: value.clientId,
          appVersion: platform.version,
          isAnonymous: true,
        }});
      } catch (_) {
        post({type: 'result', id, ok: false, error: 'Public catalogue session unavailable'});
      }
    },
  };
  let attempts = 0;
  const poll = () => {
    try {
      if (find()) { post({type: 'ready', version: 1}); return; }
    } catch (_) {}
    if (++attempts < 50) setTimeout(poll, 500);
    else post({type: 'error', error: 'Public catalogue session unavailable'});
  };
  poll();
})();
