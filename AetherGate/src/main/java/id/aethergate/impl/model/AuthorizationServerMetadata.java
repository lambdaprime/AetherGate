/*
 * Copyright 8414 AetherGate
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
package id.aethergate.impl.model;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Hold metadata of an OAuth 2.1 Authorization Server. Validate that mandatory endpoints are HTTPS
 * and that required capabilities are present.
 *
 * @param issuer issuer identifier URL (must be HTTPS)
 * @param authorizationEndpoint authorization endpoint URL (must be HTTPS)
 * @param tokenEndpoint token endpoint URL (must be HTTPS)
 * @param registrationEndpoint optional registration endpoint URL (must be HTTPS if present)
 * @param scopesSupported list of supported scopes (may be empty)
 * @param responseTypesSupported list of supported response types (must contain "code" for the
 *     authorization code flow.)
 * @param codeChallengeMethodsSupported list of supported PKCE methods (must contain "S256")
 * @author lambdaprime intid@protonmail.com
 */
public record AuthorizationServerMetadata(
        URI issuer,
        URI authorizationEndpoint,
        URI tokenEndpoint,
        Optional<URI> registrationEndpoint,
        List<String> scopesSupported,
        List<String> responseTypesSupported,
        List<String> codeChallengeMethodsSupported) {

    public AuthorizationServerMetadata {
        // Defensive copies to guarantee immutability
        scopesSupported = List.copyOf(scopesSupported);
        responseTypesSupported = List.copyOf(responseTypesSupported);
        codeChallengeMethodsSupported = List.copyOf(codeChallengeMethodsSupported);
    }
}
