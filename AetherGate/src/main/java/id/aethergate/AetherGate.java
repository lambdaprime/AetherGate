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
package id.aethergate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import id.aethergate.exception.AuthorizationException;
import id.aethergate.exception.DiscoveryException;
import id.aethergate.exception.RegistrationException;
import id.aethergate.exception.TokenException;
import id.aethergate.impl.cache.TokenCache;
import id.aethergate.impl.cache.TokenCache.CacheKey;
import id.aethergate.impl.discovery.DiscoveryEngine;
import id.aethergate.impl.flow.AuthorizationEngine;
import id.aethergate.impl.flow.DefaultUserInteraction;
import id.aethergate.impl.flow.PkceGenerator;
import id.aethergate.impl.model.AuthorizationServerMetadata;
import id.aethergate.impl.model.Token;
import id.aethergate.impl.registration.ClientRegistrar;
import id.xfunction.Preconditions;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AetherGate public API.
 *
 * @author lambdaprime intid@protonmail.com
 */
public class AetherGate {
    private static final Logger LOGGER = LoggerFactory.getLogger(AetherGate.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                    .registerModule(new Jdk8Module())
                    .registerModule(new JavaTimeModule());

    public static final int DEFAULT_CACHE_SIZE = 100;

    // Lazy‑initialized components
    private ClientRegistrar clientRegistrar;
    private AuthorizationEngine authorizationEngine;
    private TokenCache tokenCache;

    /** Use default {@link UserInteraction} implementation. */
    public AetherGate() {
        this(new DefaultUserInteraction(), DEFAULT_CACHE_SIZE);
    }

    public AetherGate(UserInteraction userInteraction, int cacheSize) {
        tokenCache = new TokenCache(cacheSize);
        clientRegistrar = new ClientRegistrar(HTTP_CLIENT, OBJECT_MAPPER);
        authorizationEngine = new AuthorizationEngine(HTTP_CLIENT, OBJECT_MAPPER, userInteraction);
    }

    /**
     * Open new {@link AuthorizationSession} for the given server (eg. MCP).
     *
     * @param config client configuration
     * @return an immutable session object containing all data required for token acquisition
     * @throws DiscoveryException when discovery of PRM/AS metadata fails
     * @throws RegistrationException when client credentials cannot be resolved
     */
    public AuthorizationSession openSession(ClientConfig config) {
        Preconditions.notNull(config, "config must not be null");
        // Initialise engines on first use.
        DiscoveryEngine discoveryEngine =
                new DiscoveryEngine(
                        HTTP_CLIENT, config.requestTimeout(), config.isInsecure(), OBJECT_MAPPER);

        URI serverBaseUri = config.serverBaseUri();
        AuthorizationServerMetadata asMetadata = discoveryEngine.discover(serverBaseUri);
        ClientCredentials clientCreds = clientRegistrar.resolve(config, asMetadata);

        // Build PKCE pair and state nonce.
        var pkce = new PkceGenerator();
        var verifier = pkce.generateVerifier();
        var challenge = pkce.generateChallenge(verifier);
        var state = pkce.generateState();

        return new AuthorizationSession(
                asMetadata,
                clientCreds.clientId(),
                clientCreds.clientSecret(),
                config.redirectUri(),
                config.scopes(),
                verifier,
                challenge,
                state,
                serverBaseUri);
    }

    /**
     * Obtain a valid Bearer access token for the given session. The method first checks an
     * in‑memory cache; if no fresh token is present it performs the full authorization code flow.
     *
     * @param session previously created {@link AuthorizationSession}
     * @return raw access token (not prefixed with "Bearer ")
     * @throws TokenException when token exchange fails or validation rejects the token
     * @throws AuthorizationException when the user‑interaction step fails
     */
    public String obtainAccessToken(AuthorizationSession session)
            throws TokenException, AuthorizationException {
        Preconditions.notNull(session, "session must not be null");

        // Try cached token.
        var cacheKey = new CacheKey(session.clientId(), session.resourceUri(), session.scopes());
        Token maybeCached = tokenCache.get(cacheKey).orElse(null);
        if (maybeCached != null && maybeCached.expiresAt().isAfter(Instant.now())) {
            LOGGER.debug("Returning cached access token for client {}", session.clientId());
            return maybeCached.accessToken();
        }

        if (tokenCache.isEmpty()) {
            LOGGER.debug("Token not found in cache because cache is empty, create new token ...");
        }

        // Perform full flow.
        Token token = authorizationEngine.performFlow(session);
        Preconditions.isTrue(
                token.accessToken() != null && !token.accessToken().isBlank(),
                "access_token must be present");

        // Store in cache.
        tokenCache.put(cacheKey, token);

        return token.accessToken();
    }
}
