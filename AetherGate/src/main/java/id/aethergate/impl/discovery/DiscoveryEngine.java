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
package id.aethergate.impl.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.aethergate.exception.DiscoveryException;
import id.aethergate.exception.InsecureUrlException;
import id.aethergate.impl.model.AuthorizationServerMetadata;
import id.aethergate.impl.model.ProtectedResourceMetadata;
import id.xfunction.Preconditions;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs discovery of the authorization server for a given MCP base URL.
 *
 * @author lambdaprime intid@protonmail.com
 */
public final class DiscoveryEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryEngine.class);
    private static final String WELL_KNOWN_PRM = ".well-known/oauth-protected-resource";
    private static final String WELL_KNOWN_AS_METADATA = ".well-known/oauth-authorization-server";
    private static final Pattern RESOURCE_METADATA_REGEX =
            Pattern.compile("resource_metadata=\"([^\"]+)\"");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private Duration requestTimeout;
    private boolean isSecure;

    /**
     * Constructs a DiscoveryEngine with the provided {@link HttpClient}.
     *
     * @param httpClient HTTP client used for all outbound calls; must not be null.
     */
    public DiscoveryEngine(
            HttpClient httpClient,
            Duration requestTimeout,
            boolean isInsecure,
            ObjectMapper objectMapper) {
        this.isSecure = !isInsecure;
        Preconditions.notNull(httpClient, "httpClient must not be null");
        this.httpClient = httpClient;
        this.requestTimeout = requestTimeout;
        this.objectMapper = objectMapper;
    }

    /**
     * Discovers the {@link AuthorizationServerMetadata} for the supplied MCP base URL. The method
     * performs the following steps:
     *
     * <ol>
     *   <li>Fetches Protected Resource Metadata (PRM).
     *   <li>Selects the first {@code authorization_servers} entry.
     *   <li>Retrieves OAuth 2.1 Authorization Server metadata from that server's well‑known
     *       endpoint.
     * </ol>
     *
     * @param mcpBaseUri Base URL of the MCP server (e.g., {@code https://api.example.com}).
     * @return Parsed {@link AuthorizationServerMetadata}.
     * @throws DiscoveryException if any step fails or validation constraints are violated.
     */
    public AuthorizationServerMetadata discover(URI mcpBaseUri) throws DiscoveryException {
        Preconditions.isTrue(
                mcpBaseUri != null && !mcpBaseUri.toString().isBlank(),
                "mcpBaseUrl must not be blank");
        ProtectedResourceMetadata prm = obtainPrm(mcpBaseUri);

        var authServerUrl =
                selectAuthorizationServer(prm.authorizationServers())
                        .orElseThrow(
                                () ->
                                        new DiscoveryException(
                                                "No authorization server listed in PRM"));
        LOGGER.debug("Selected Authorization Server: {}", authServerUrl);

        var asMetaUri = URI.create(ensureTrailingSlash(authServerUrl) + WELL_KNOWN_AS_METADATA);
        LOGGER.debug("Fetching Authorization Server Metadata from {}", asMetaUri);
        var asMetadata =
                fetchJson(asMetaUri, AuthorizationServerMetadata.class)
                        .orElseThrow(
                                () -> new DiscoveryException("Unable to retrieve AS metadata"));
        validateAsMetadata(asMetadata);
        return asMetadata;
    }

    /**
     * Fetches Protected Resource Metadata (PRM).
     *
     * <ol>
     *   <li>Attempts to fetch Protected Resource Metadata (PRM) from the WWW-Authenticate header.
     *   <li>Falls back to fetching PRM from well-known locations if no header is found.
     * </ol>
     */
    private ProtectedResourceMetadata obtainPrm(URI mcpBaseUri) {
        ProtectedResourceMetadata prm =
                fetchPrmFromWWWAuthenticate(mcpBaseUri, List.of("GET", "POST").iterator())
                        .orElse(null);
        if (prm == null) {
            var prmUri = URI.create(ensureTrailingSlash(mcpBaseUri) + WELL_KNOWN_PRM).normalize();
            LOGGER.debug("Fetching Protected Resource Metadata from {}", prmUri);
            prm =
                    fetchJson(prmUri, ProtectedResourceMetadata.class)
                            .orElseThrow(() -> new DiscoveryException("Unable to retrieve PRM"));
        }
        validatePrm(prm);
        return prm;
    }

    /**
     * Attempts to fetch Protected Resource Metadata (PRM) from the WWW-Authenticate header.
     *
     * @param mcpBaseUri The base URI of the MCP server.
     * @return Optional containing PRM if found in the header, otherwise empty.
     */
    private Optional<ProtectedResourceMetadata> fetchPrmFromWWWAuthenticate(
            URI mcpBaseUri, Iterator<String> method) {
        if (!method.hasNext()) {
            LOGGER.debug(
                    "Iterated over all possible HTTP methods, none of them is allowed by the"
                            + " server");
            return Optional.empty();
        }
        try {
            // Create a request to trigger a 401 response (if needed)
            var request =
                    HttpRequest.newBuilder()
                            .uri(mcpBaseUri)
                            .timeout(requestTimeout)
                            .header("Authorization", "Bearer invalid-token") // Force 401
                            .method(method.next(), BodyPublishers.ofString(""))
                            .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            switch (status) {
                case 401 -> {
                    String wwwAuthHeader =
                            response.headers().firstValue("WWW-Authenticate").orElse("");
                    LOGGER.debug("Received WWW-Authenticate header: {}", wwwAuthHeader);

                    // Extract resource_metadata URL from the header
                    var matcher = RESOURCE_METADATA_REGEX.matcher(wwwAuthHeader);
                    if (matcher.find()) {
                        String resourceMetadataUrl = matcher.group(1);
                        LOGGER.debug("Extracted PRM URL from header: {}", resourceMetadataUrl);

                        URI prmUri = URI.create(resourceMetadataUrl).normalize();
                        return fetchJson(prmUri, ProtectedResourceMetadata.class);
                    }
                }
                case 405 -> {
                    // Method Not Allowed - move to next
                    return fetchPrmFromWWWAuthenticate(mcpBaseUri, method);
                }
                default ->
                        LOGGER.debug(
                                "Failed to fetch PRM from WWW-Authenticate header status={}"
                                        + " body={}",
                                status,
                                response.body());
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.debug("Failed to fetch PRM from WWW-Authenticate header", e);
        }
        return Optional.empty();
    }

    private <T> Optional<T> fetchJson(URI uri, Class<T> type) throws DiscoveryException {
        var request =
                HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(requestTimeout)
                        .header("Accept", "application/json")
                        .GET()
                        .build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            LOGGER.debug("GET {} -> {}", uri, status);
            if (status != 200) {
                throw new DiscoveryException("Unexpected HTTP status " + status + " for " + uri);
            }
            var body = response.body();
            var value = objectMapper.readValue(body, type);
            return Optional.ofNullable(value);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DiscoveryException("Failed to fetch JSON from " + uri, e);
        }
    }

    private void validatePrm(ProtectedResourceMetadata prm) throws DiscoveryException {
        // Ensure URLs are HTTPS and required fields are present.
        if (prm.authorizationServers() == null || prm.authorizationServers().isEmpty()) {
            throw new DiscoveryException("PRM must contain at least one authorization server");
        }
        for (URI url : prm.authorizationServers()) {
            if (isSecure && !url.toString().startsWith("https://"))
                throw new InsecureUrlException("Authorization server URL " + prm);
        }
        if (isSecure && !prm.resource().startsWith("https://")) {
            throw new InsecureUrlException("PRM resource " + prm);
        }
    }

    private void validateAsMetadata(AuthorizationServerMetadata meta) throws DiscoveryException {
        if (isSecure && meta.issuer() != null && meta.issuer().getScheme() == "https:")
            throw new InsecureUrlException("Issuer URL " + meta.issuer());
        if (isSecure
                && meta.authorizationEndpoint() != null
                && meta.authorizationEndpoint().getScheme() == "https:")
            throw new InsecureUrlException(
                    "Authorization endpoint " + meta.authorizationEndpoint());
        if (isSecure
                && meta.tokenEndpoint() != null
                && meta.tokenEndpoint().getScheme() == "https:")
            throw new InsecureUrlException("Token endpoint  " + meta.tokenEndpoint());
        if (meta.responseTypesSupported() == null
                || !meta.responseTypesSupported().contains("code")) {
            throw new DiscoveryException(
                    "Authorization server does not support response_type=code");
        }
        if (meta.codeChallengeMethodsSupported() == null
                || !meta.codeChallengeMethodsSupported().contains("S256")) {
            throw new DiscoveryException("Authorization server does not support PKCE S256 method");
        }
    }

    private Optional<URI> selectAuthorizationServer(List<URI> servers) {
        return servers.stream().findFirst();
    }

    private URI ensureTrailingSlash(URI uri) {
        var url = uri.toString();
        return url.endsWith("/") ? uri : URI.create(url + "/");
    }
}
