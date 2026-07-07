> Based on [simple-auth](https://github.com/modelcontextprotocol/python-sdk/tree/v1.28.1) example from Python mcp-sdk v1.28.1

Build:
```
docker image rm mcp-simple-auth
docker build . -t mcp-simple-auth
```

Run container:
```
docker run --rm \
  --network=host \
  -it mcp-simple-auth \
  /bin/bash
```

Inside container:

- Start Authorization Server on port 9000
```
uv run mcp-simple-auth-as --port=9000 &
uv run mcp-simple-auth-rs --port=8001 --auth-server=http://localhost:9000 --transport=streamable-http --oauth-strict &
```

- Test from host:
```
curl -v http://127.0.0.1:8001/.well-known/oauth-protected-resource/mcp
```
