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

import id.aethergate.impl.model.AuthorizationServerMetadata;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Holds the session data for an OAuth 2.1 Authorization Code flow with PKCE.
 *
 * @param asMetadata the metadata of the selected Authorization Server
 * @param clientId the client identifier issued by the AS (public or confidential)
 * @param clientSecret optional client secret; empty for public clients
 * @param redirectUri the redirect URI registered with the AS
 * @param scopes the set of scopes requested for the token
 * @param pkceVerifier the PKCE code verifier generated for the flow
 * @param pkceChallenge the PKCE code challenge derived from the verifier (method S256)
 * @param state a cryptographically random nonce used to bind the request and response
 * @param resourceUri the canonical MCP resource URI for which the token is requested
 * @author lambdaprime intid@protonmail.com
 */
public record AuthorizationSession(
        AuthorizationServerMetadata asMetadata,
        String clientId,
        Optional<String> clientSecret,
        URI redirectUri,
        List<String> scopes,
        String pkceVerifier,
        String pkceChallenge,
        String state,
        URI resourceUri) {
    // No additional behavior – the record is immutable and thread‑safe.
}
