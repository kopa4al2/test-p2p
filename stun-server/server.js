const dgram = require('dgram');
const server = dgram.createSocket('udp4');

const peers = {}; // { name: { address, port } }

server.on('message', (msg, rinfo) => {
    const data = msg.toString();
    console.log(`Получено: ${data} от ${rinfo.address}:${rinfo.port}`);

    if (data.startsWith('REGISTER:')) {
        const name = data.split(':')[1];
        peers[name] = { address: rinfo.address, port: rinfo.port };

        // Пращаме на клиента собственото му публично IP/Port (за тест)
        server.send(`YOU_ARE:${rinfo.address}:${rinfo.port}`, rinfo.port, rinfo.address);

        // Ако има други пиъри, ги обявяваме (проста логика)
        Object.keys(peers).forEach(peerName => {
            if (peerName !== name) {
                const p = peers[peerName];
                // Казваме на новия за стария
                server.send(`PEER_INFO:${peerName}:${p.address}:${p.port}`, rinfo.port, rinfo.address);
                // Казваме на стария за новия
                server.send(`PEER_INFO:${name}:${rinfo.address}:${rinfo.port}`, p.port, p.address);
            }
        });
    }
});

server.bind(5000, () => console.log('Signaling server listening on port 5000'));
