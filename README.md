# Tanzu Platform Chat SPA

This repository now contains a simplified single page application built with Vue.js. All Java and Maven dependencies have been removed.

## Prerequisites

- Node.js 20+

## Running the demo

Start the lightweight Node server:

```bash
node server.js
```

Open <http://localhost:8080> in your browser to use the chat interface. The server exposes two minimal endpoints:

- `GET /chat?chat=hello` – streams a demo response using Server-Sent Events
- `GET /metrics` – returns static platform metrics

These endpoints are placeholders for the original functionality.
