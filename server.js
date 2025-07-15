const http = require('http');
const fs = require('fs');
const path = require('path');

const metrics = {
  conversationId: '',
  chatModel: 'demo-model',
  embeddingModel: 'demo-embed',
  vectorStoreName: 'demo-vector',
  agents: [],
  prompts: {
    totalPrompts: 0,
    serversWithPrompts: 0,
    available: false,
    promptsByServer: {}
  }
};

const server = http.createServer((req, res) => {
  if (req.url.startsWith('/metrics')) {
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify(metrics));
    return;
  }

  if (req.url.startsWith('/chat')) {
    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      Connection: 'keep-alive'
    });

    res.write(`data: ${JSON.stringify({ content: 'Hello from server' })}\n\n`);
    setTimeout(() => {
      res.write(`event: close\ndata: \n\n`);
      res.end();
    }, 1000);
    return;
  }

  // serve static files
  let filePath = path.join(__dirname, 'frontend', req.url === '/' ? 'index.html' : req.url);
  fs.readFile(filePath, (err, data) => {
    if (err) {
      res.writeHead(404);
      res.end('Not found');
    } else {
      res.writeHead(200);
      res.end(data);
    }
  });
});

server.listen(8080, () => console.log('Server running on http://localhost:8080'));
