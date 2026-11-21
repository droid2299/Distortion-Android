(() => {
  if (window.Myspoti) return;
  const post = value => window.DistortionCatalogue.postMessage(JSON.stringify(value));
  let version;
  let token;
  const clientVersion = () => {
    const script = document.getElementById('appServerConfig');
    if (!script) return null;
    const text = script.textContent.trim();
    let config;
    try { config = JSON.parse(text); } catch (_) { config = JSON.parse(atob(text)); }
    return typeof config.clientVersion === 'string' ? config.clientVersion : null;
  };
  const find = () => {
    const root = document.getElementById('main') || document.body;
    if (!root) return false;
    for (const element of [root, ...root.querySelectorAll('*')]) {
      const key = Object.keys(element).find(k => k.startsWith('__reactContainer$'));
      if (!key) continue;
      const pending = [element[key], element[key]?.stateNode?.current];
      const seen = new Set();
      while (pending.length && seen.size < 50000) {
        const fiber = pending.pop();
        if (!fiber || seen.has(fiber)) continue;
        seen.add(fiber);
        const value = fiber.memoizedProps && fiber.memoizedProps.value;
        try {
          const registry = value && (typeof value.getRegistry === 'function' ? value.getRegistry() : value);
          if (registry && registry._map && typeof registry.resolve === 'function') {
            const tokenKey = [...registry._map.keys()].find(k => k && k.description === 'TokenProvider');
            const currentVersion = typeof value.version === 'string' ? value.version : clientVersion();
            if (tokenKey && currentVersion) {
              token = registry.resolve(tokenKey);
              version = currentVersion;
              return true;
            }
          }
        } catch (_) {}
        if (fiber.sibling) pending.push(fiber.sibling);
        if (fiber.child) pending.push(fiber.child);
        if (fiber.alternate) pending.push(fiber.alternate);
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
          appVersion: version,
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
