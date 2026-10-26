import type { ProxyOptions } from 'vite'

// A broken upstream stream must close the downstream response so fetch-event-source retries.
export const configureSseProxy: NonNullable<ProxyOptions['configure']> = (proxy) => {
  proxy.on('proxyRes', (upstream, request, response) => {
    if (request.url?.split('?')[0] !== '/api/events') return
    upstream.once('close', () => {
      if (!upstream.complete && !response.writableEnded) response.destroy()
    })
  })
}
