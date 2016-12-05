# kgagent

KGAgent is a very simple browser that enables a user with a keymaster to
authenticate to a KGServer and retrieve resources from that server.

See [ADILOS](https://github.com/bitsanity/ADILOS)

The browser supports http: an kg: schemes. The format for kg: is

~~~~
  kg:host:port:resource
~~~~

where host is either DNS or IP address. The KGServer at host:port will answer
a request on a new connection with a new challenge, which the agent displays to
the user. The user with a keymaster has preregistered a public key with
the server. The user signs the challenge and provides the response to the
agent. The agent completes the challenge with the server.

Once authenticated, the agent and server exchange JSON-RPC messages on the
socket. If the connection closes the shared session is lost. Further requests
and responses are cryptographically signed and verified by each party.

The details of the request and reply messages is explained at:

[kgserver](https://github.com/bitsanity/kgserver)

Note that kgagent enables a user to authenticate to a service and does not
authenticate the service to the the user.

## Dependencies:

** Java **
- developed on Oracle JDK 1.8 on Ubuntu64

** libsecp256k1 **
- github.com/bitcoin-core/secp256k1

** Google's ZXing library **
- github.com/zxing/zxing

** json-simple library **
- For generating and processing JSON
- github.com/fangyidong/json-simple

