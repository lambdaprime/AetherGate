/**
 * <b>AetherGate</b> - A Java module for automated <a
 * href="https://modelcontextprotocol.io/docs/tutorials/security/authorization">OAuth 2.1-based
 * authorization flow for MCP</a> (Model Context Protocol) servers.
 *
 * <p>Available functionality:
 *
 * <ol>
 *   <li>Automated discovery of OAuth 2.1 Protected Resource Metadata (PRM) and Authorization Server
 *       metadata.
 *   <li>Dynamic client registration with support for pre-registered clients via Client ID Metadata
 *       Document (CIMD).
 *   <li>Execution of the OAuth 2.1 Authorization Code Flow with PKCE (Proof Key for Code Exchange)
 *       for enhanced security.
 *   <li>Thread-safe caching of access tokens to minimize redundant token requests.
 *   <li>Validation of token responses, including verification of the ~aud~ claim and ~Bearer~ token
 *       type.
 *   <li>Customizable user interaction handling ({@link id.aethergate.UserInteraction}) for
 *       browser-based or headless environments.
 * </ol>
 *
 * <p><b>AetherGate</b> simplifies integrating OAuth 2.1-protected MCP servers into Java
 * applications by abstracting away the complexity of dynamic client registration, PKCE generation,
 * and token validation. It provides a clean API to obtain valid Bearer access tokens without manual
 * steps.
 *
 * <h2>Requirements</h2>
 *
 * <ul>
 *   <li>Java 25+
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <p>To use <b>AetherGate</b> in your application:
 *
 * <ol>
 *   <li>Configure the client settings using {@link id.aethergate.ClientConfig#builder()}.
 *   <li>Initialize an {@link id.aethergate.AetherGate} instance.
 *   <li>Open an authorization session with {@link id.aethergate.AetherGate#openSession}.
 *   <li>Obtain an access token by calling {@link
 *       id.aethergate.AetherGate#obtainAccessToken(AuthorizationSession)}.
 * </ol>
 *
 * <p>The module handles all OAuth 2.1 flows, including dynamic client registration and PKCE, behind
 * the scenes.
 *
 * <h3>Key Components</h3>
 *
 * <ul>
 *   <li>{@link id.aethergate.AetherGate} The main facade class that orchestrates the OAuth 2.1
 *       flow.
 *   <li>{@link id.aethergate.ClientConfig} A builder for configuring client settings such as server
 *       URL, redirect URI, and scopes.
 *   <li>{@link id.aethergate.AuthorizationSession} An immutable record representing the state of an
 *       authorization process.
 * </ul>
 *
 * <h2>Examples</h2>
 *
 * <h3>Obtain access token</h3>
 *
 * {@snippet lang="java" :
 * import id.aethergate.AetherGate;
 * import id.aethergate.AuthorizationSession;
 * import id.aethergate.ClientConfig;
 * import id.aethergate.exception.DiscoveryException;
 *
 * try {
 *     var aetherGate = new AetherGate();
 *     AuthorizationSession session =
 *             aetherGate.openSession(
 *                     ClientConfig.builder()
 *                             .serverBaseUrl("http://localhost:8001/mcp")
 *                             .redirectUrl("http://localhost:8002/callback")
 *                             .scopes("user")
 *                             .isInsecure(true)
 *                             .build());
 *     System.out.println(session);
 *     String token = aetherGate.obtainAccessToken(session);
 *     System.out.println(token);
 * } catch (DiscoveryException e) {
 *     System.out.println("Server does not support OAuth 2.1-based authorization: " + e.getMessage());
 * }
 * }
 *
 * <h3>Use access token with langchain4j MCP client</h3>
 *
 * {@snippet lang="java" :
 * import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
 *
 * McpTransport transport = StreamableHttpMcpTransport.builder()
 *    .url("http://localhost:3001/mcp")
 *    .customHeaders(Map.of("Authorization", "Bearer " + token))
 *    .logRequests(true)
 *    .logResponses(true)
 *    .build();
 * }
 *
 * @see <a href="https://modelcontextprotocol.io/docs/tutorials/security/authorization">OAuth
 *     2.1-based authorization flow for MCP</a>
 * @see <a href="https://github.com/lambdaprime/aethergate">Source Repository</a>
 * @see <a
 *     href="https://github.com/lambdaprime/aethergate/blob/main/aethergate/release/CHANGELOG.md">Releases</a>
 * @author lambdaprime intid@protonmail.com
 */
module aethergate {
    exports id.aethergate;
    exports id.aethergate.exception;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires id.xfunction;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jdk8;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires java.desktop;
    requires java.net.http;
    requires org.slf4j;
}
