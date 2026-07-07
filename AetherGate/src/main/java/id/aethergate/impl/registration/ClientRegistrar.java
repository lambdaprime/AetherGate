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
package id.aethergate.impl.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.aethergate.ClientConfig;
import id.aethergate.ClientCredentials;
import id.aethergate.exception.RegistrationException;
import id.aethergate.impl.model.AuthorizationServerMetadata;
import id.xfunction.Preconditions;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolve client credentials for a given {@link AuthorizationServerMetadata} using the strategy
 * defined in {@link ClientConfig}. The resolver supports three modes:
 *
 * <ul>
 *   <li>pre‑registered static client (clientId/clientSecret supplied directly)
 *   <li>fetching a pre‑published ClientID Metadata Document (CIMD) from a configurable URL
 *   <li>dynamic client registration when the AS provides a {@code registration_endpoint}
 * </ul>
 *
 * If none of these mechanisms are applicable, a {@link RegistrationException} is thrown.
 *
 * @author lambdaprime intid@protonmail.com
 */
public class ClientRegistrar {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientRegistrar.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /** Create a registrar with the supplied {@link HttpClient}. */
    public ClientRegistrar(HttpClient httpClient, ObjectMapper objectMapper) {
        Preconditions.notNull(httpClient, "httpClient must not be null");
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /** Resolve client credentials according to the configuration and server metadata. */
    public ClientCredentials resolve(ClientConfig config, AuthorizationServerMetadata asMeta)
            throws RegistrationException {
        Preconditions.notNull(config, "config must not be null");
        Preconditions.notNull(asMeta, "AuthorizationServerMetadata must not be null");

        // 1. Pre‑registered static client.
        if (config.preRegisteredClient().isPresent()) {
            ClientCredentials client = config.preRegisteredClient().get();
            String clientId = client.clientId();
            Optional<String> clientSecret = client.clientSecret();
            LOGGER.debug("Using pre‑registered clientId {}", clientId);
            return new ClientCredentials(clientId, clientSecret);
        }

        // 2. CIMD fetch.
        if (config.cimdUri().isPresent()) {
            ClientIdMetadataDocument cimd = fetchCimd(config.cimdUri().get());
            validateRedirectUri(cimd.redirectUris(), config.redirectUri());
            LOGGER.debug("Fetched clientId {} from CIMD", cimd.clientId());
            return new ClientCredentials(cimd.clientId(), cimd.clientSecret());
        }

        // 3. Dynamic registration.
        if (asMeta.registrationEndpoint().isPresent()) {
            URI regEndpoint = asMeta.registrationEndpoint().get();
            var dynResp =
                    dynamicRegister(
                            regEndpoint,
                            config,
                            List.of("client_secret_basic", "client_secret_post").iterator());
            LOGGER.debug("Dynamically registered clientId {}", dynResp.clientId());
            return new ClientCredentials(
                    dynResp.clientId(), Optional.ofNullable(dynResp.clientSecret()));
        }

        throw new RegistrationException(
                "Unable to obtain client credentials: no suitable registration method configured");
    }

    /** Fetch a CIMD JSON document from the supplied URL. */
    private ClientIdMetadataDocument fetchCimd(URI url) throws RegistrationException {
        var request =
                HttpRequest.newBuilder()
                        .uri(url)
                        .GET()
                        .header("Accept", "application/json")
                        .build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            LOGGER.debug("CIMD request to {} returned status {}", url, status);
            if (status != 200) {
                throw new RegistrationException("Failed to fetch CIMD: HTTP " + status);
            }
            return objectMapper.readValue(response.body(), ClientIdMetadataDocument.class);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RegistrationException("Error fetching CIMD", e);
        }
    }

    /** Perform dynamic client registration against the AS registration endpoint. */
    private DynamicRegistrationResponse dynamicRegister(
            URI endpoint, ClientConfig config, Iterator<String> authMethod)
            throws RegistrationException {
        if (!authMethod.hasNext()) {
            throw new RegistrationException(
                    "Dynamic registration failed due to 400 Bad Request: endpoint=%s"
                            .formatted(endpoint));
        }
        var requestPayload =
                new DynamicRegistrationRequest(
                        List.of(config.redirectUri()),
                        List.of("authorization_code", "refresh_token"),
                        Optional.of(authMethod.next()),
                        Optional.empty(),
                        config.scopes());
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(requestPayload);
        } catch (IOException e) {
            throw new RegistrationException("Failed to serialize dynamic registration request", e);
        }

        var request =
                HttpRequest.newBuilder()
                        .uri(endpoint)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            LOGGER.debug("Dynamic registration request to {} returned status={}", endpoint, status);
            if (status == 400) {
                LOGGER.debug("Trying next auth method");
                return dynamicRegister(endpoint, config, authMethod);
            } else if (status != 201 && status != 200) {
                throw new RegistrationException(
                        "Dynamic registration failed: endpoint=%s, status=%s response=%s"
                                .formatted(endpoint, status, response.body()));
            }
            return objectMapper.readValue(response.body(), DynamicRegistrationResponse.class);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RegistrationException("Error during dynamic client registration", e);
        }
    }

    /** Ensure at least one redirect URI from the CIMD matches the configured redirect URI. */
    private void validateRedirectUri(List<URI> allowed, URI expected) throws RegistrationException {
        var match = allowed.stream().anyMatch(uri -> uri.equals(expected));
        Preconditions.isTrue(
                match,
                "Configured redirect_uri %s is not present in CIMD's redirect_uris",
                expected);
    }
}
