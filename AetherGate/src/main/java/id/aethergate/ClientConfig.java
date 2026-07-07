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

import id.xfunction.Preconditions;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Immutable configuration holder for {@link id.aethergate.AetherGate}.<br>
 * Use the {@link Builder} to construct an instance.
 *
 * @param requestTimeout timeout of HTTP requests
 * @param isInsecure allow HTTP insecure connections
 * @author lambdaprime intid@protonmail.com
 */
public record ClientConfig(
        URI serverBaseUri,
        URI redirectUri,
        Optional<ClientCredentials> preRegisteredClient,
        Optional<URI> cimdUri,
        List<String> scopes,
        Duration requestTimeout,
        boolean isInsecure) {

    /** Returns a new {@link Builder} instance. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link ClientConfig}. All fields are optional except {@code serverBaseUrl}
     * and {@code redirectUri}, which must be provided before calling {@link #build()}.
     */
    public static final class Builder {
        private URI serverBaseUrl;
        private URI redirectUri;
        private Optional<String> preRegisteredClientId = Optional.empty();
        private Optional<String> preRegisteredClientSecret = Optional.empty();
        private Optional<URI> cimdUrl = Optional.empty();
        private List<String> scopes = List.of();
        private boolean isInsecure = false;
        private Duration requestTimeout = Duration.ofSeconds(45);

        private Builder() {}

        public Builder serverBaseUrl(String url) {
            this.serverBaseUrl = URI.create(url);
            return this;
        }

        public Builder redirectUrl(String uri) {
            this.redirectUri = URI.create(uri);
            return this;
        }

        public Builder preRegisteredClientId(String clientId) {
            this.preRegisteredClientId = Optional.ofNullable(clientId);
            return this;
        }

        public Builder preRegisteredClientSecret(String secret) {
            this.preRegisteredClientSecret = Optional.ofNullable(secret);
            return this;
        }

        public Builder cimdUrl(URI uri) {
            this.cimdUrl = Optional.ofNullable(uri);
            return this;
        }

        public Builder scopes(String... scopes) {
            this.scopes = Arrays.asList(scopes);
            return this;
        }

        public Builder isInsecure(boolean isInsecure) {
            this.isInsecure = isInsecure;
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        /** Build the immutable {@link ClientConfig} instance. */
        public ClientConfig build() {
            // Basic validation using Preconditions (assumed available)
            Preconditions.isTrue(
                    serverBaseUrl != null && !serverBaseUrl.toString().isBlank(),
                    "serverBaseUrl must be provided");
            Preconditions.isTrue(redirectUri != null, "redirectUri must be provided");
            return new ClientConfig(
                    serverBaseUrl,
                    redirectUri,
                    preRegisteredClientId.map(
                            clientId -> new ClientCredentials(clientId, preRegisteredClientSecret)),
                    cimdUrl,
                    List.copyOf(scopes),
                    requestTimeout,
                    isInsecure);
        }
    }
}
