// Close stdin so no terminal input can kill us
process.stdin.destroy();

const { createServer } = require('vite');

(async () => {
  const server = await createServer({
    configFile: './vite.config.js',
    server: { port: 3000 }
  });
  await server.listen();
  server.printUrls();
  console.log('\n✅ Server running (immune to terminal input)');

  // Keep process alive
  process.on('SIGINT', () => { server.close(); process.exit(0); });
  process.on('SIGTERM', () => { server.close(); process.exit(0); });
})();
