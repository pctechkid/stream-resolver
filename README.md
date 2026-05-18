# Stream Resolver for Nuvio/Stremio

Stream Resolver is a small Android helper app made for Nuvio/Stremio-style streaming workflows.

It acts like a lightweight fake video player. Instead of playing the video itself, it receives a stream URL from apps like Nuvio, resolves the final redirected stream/CDN URL, then forwards that final link to mpvKt or another mpv-android based player.

## Why this exists

Some Stremio/Nuvio addons return a resolver URL first instead of the real final stream URL.

Example flow without Stream Resolver:

```text
Nuvio
→ addon resolve URL
→ external player opens
→ external player follows redirect internally
→ final CDN stream plays