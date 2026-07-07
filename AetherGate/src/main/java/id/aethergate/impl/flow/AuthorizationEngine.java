/*
 * Copyright 2026 AetherGate
 * 
 * Website: https://github.com/lambdaprime
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package id.aethergate.impl.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.aethergate.AuthorizationSession;
import id.aethergate.UserInteraction;
import id.aethergate.exception.AuthorizationException;
import id.aethergate.exception.TokenException;
import id.aethergate.impl.model.Token;
import id.aethergate.impl.model.TokenResponse;
import id.xfunction.Preconditions;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes the OAuth 2.1 Authorization Code Flow with PKCE for a single {@link
 * AuthorizationSession}.
 *
 * <p>The flow consists of:
 *
 * <ul>
 *   <li>building the authorization request URL,
 *   <li>delegating to {@link UserInteraction} to let the user authenticate,
 *   <li>exchanging the received code for a token, and
 *   <li>validating the token response.
 * </ul>
 *
 * @author lambdaprime intid@protonmail.com
 */
public class AuthorizationEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationEngine.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final UserInteraction userInteraction;

    /**
     * Constructs a new {@code AuthorizationEngine}.
     *
     * @param httpClient the HTTP client used for outbound calls
     * @param objectMapper the Jackson mapper configured for the library
     * @param userInteraction implementation that opens a browser and extracts redirect parameters
     */
    public AuthorizationEngine(
            HttpClient httpClient, ObjectMapper objectMapper, UserInteraction userInteraction) {
        Preconditions.notNull(httpClient, "httpClient must not be null");
        Preconditions.notNull(objectMapper, "objectMapper must not be null");
        Preconditions.notNull(userInteraction, "userInteraction must not be null");
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.userInteraction = userInteraction;
    }

    /**
     * Performs the complete authorization flow for {@code session} and returns a valid Bearer
     * access token.
     *
     * @param session the prepared session containing all required data
     * @return the raw access token string (without the "Bearer " prefix)
     * @throws AuthorizationException if any step of the user‑interaction or code exchange fails
     * @throws TokenException if the token response is invalid or cannot be parsed
     */
    public Token performFlow(AuthorizationSession session)
            throws AuthorizationException, TokenException {
        try {
            // 1. Build authorization URL
            var authUrl = buildAuthorizationUrl(session);
            LOGGER.debug("Authorization URL: {}", authUrl);

            // 2. Open browser (or custom UI) and obtain redirect URI containing code & state
            URI redirectUri = userInteraction.startAuthorization(authUrl);
            Map<String, String> params = userInteraction.extractParameters(redirectUri);

            var codeOpt = Optional.ofNullable(params.get("code"));
            var stateOpt = Optional.ofNullable(params.get("state"));

            if (codeOpt.isEmpty() || stateOpt.isEmpty()) {
                throw new AuthorizationException("Missing 'code' or 'state' in redirect URI");
            }
            var code = codeOpt.get();
            var returnedState = stateOpt.get();

            // 3. Validate state matches the one generated for the session
            if (!session.state().equals(returnedState)) {
                throw new AuthorizationException(
                        "Invalid state parameter returned by authorization server");
            }

            // 4. Exchange code for token
            TokenResponse tokenResponse = requestToken(session, code);

            // 5. Basic validation of the token response
            validateTokenResponse(tokenResponse, session.resourceUri());
            return new Token(
                    tokenResponse.accessToken(),
                    Instant.now().plusSeconds(tokenResponse.expiresIn()),
                    tokenResponse.refreshToken());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthorizationException("IO or interruption while performing flow", e);
        }
    }

    private URI buildAuthorizationUrl(AuthorizationSession session) {
        var asMeta = session.asMetadata();
        var sb = new StringBuilder();
        sb.append(asMeta.authorizationEndpoint());
        sb.append('?');
        sb.append("response_type=code");
        sb.append('&').append("client_id=").append(urlEncode(session.clientId()));
        sb.append('&').append("redirect_uri=").append(urlEncode(session.redirectUri().toString()));
        sb.append('&').append("scope=").append(urlEncode(String.join(" ", session.scopes())));
        sb.append('&').append("code_challenge_method=S256");
        sb.append('&').append("code_challenge=").append(urlEncode(session.pkceChallenge()));
        sb.append('&').append("resource=").append(urlEncode(session.resourceUri().toString()));
        sb.append('&').append("state=").append(urlEncode(session.state()));
        return URI.create(sb.toString());
    }

    private TokenResponse requestToken(AuthorizationSession session, String code)
            throws IOException, InterruptedException, TokenException {
        var asMeta = session.asMetadata();
        var form = new StringBuilder();
        form.append("grant_type=authorization_code");
        form.append('&').append("code=").append(urlEncode(code));
        form.append('&').append("client_id=").append(urlEncode(session.clientId()));
        form.append('&')
                .append("redirect_uri=")
                .append(urlEncode(session.redirectUri().toString()));
        form.append('&').append("code_verifier=").append(urlEncode(session.pkceVerifier()));
        form.append('&').append("resource=").append(urlEncode(session.resourceUri().toString()));

        // Include client authentication if a secret is present (confidential client)
        URI tokenEndpoint = asMeta.tokenEndpoint();
        var requestBuilder =
                HttpRequest.newBuilder()
                        .uri(tokenEndpoint)
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form.toString()));

        session.clientSecret()
                .ifPresent(
                        secret -> {
                            var credentials = session.clientId() + ":" + secret;
                            var encoded =
                                    java.util.Base64.getEncoder()
                                            .encodeToString(
                                                    credentials.getBytes(StandardCharsets.UTF_8));
                            requestBuilder.header("Authorization", "Basic " + encoded);
                        });

        var request = requestBuilder.build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        LOGGER.debug(
                "Token endpoint {} responded with status {} response: {}",
                tokenEndpoint,
                status,
                response.body());
        if (status != 200) {
            throw new TokenException(
                    "Token endpoint %s returned non‑OK status %s: %s"
                            .formatted(tokenEndpoint, status, response.body()));
        }
        var body = response.body();
        try {
            return objectMapper.readValue(body, TokenResponse.class);
        } catch (IOException e) {
            throw new TokenException("Failed to parse token response", e);
        }
    }

    private void validateTokenResponse(TokenResponse token, URI expectedResource)
            throws TokenException {
        if (!"Bearer".equalsIgnoreCase(token.tokenType())) {
            throw new TokenException("Unsupported token_type: " + token.tokenType());
        }
        List<String> audiences = token.aud();
        if (audiences != null
                && !audiences.isEmpty()
                && audiences.stream().noneMatch(a -> a.equals(expectedResource.toString()))) {
            throw new TokenException(
                    "Token audience does not match expected=%s actual=%s"
                            .formatted(expectedResource, audiences));
        }
        // Basic expiry check
        var expiresAt = Instant.now().plusSeconds(token.expiresIn());
        if (expiresAt.isBefore(Instant.now())) {
            throw new TokenException("Received token is already expired");
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
