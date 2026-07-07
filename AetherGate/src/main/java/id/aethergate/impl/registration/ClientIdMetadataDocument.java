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

import id.xfunction.Preconditions;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Represents a JSON document describing a pre‑registered client (CIMD).
 *
 * <p>Representation of a CIMD document (subset required for registration).
 *
 * @author lambdaprime intid@protonmail.com
 * @param clientId the client identifier (non‑blank)
 * @param clientSecret optional client secret
 * @param redirectUris list of HTTPS redirect URIs (at least one)
 * @param grantTypes list of grant types (must include "authorization_code")
 * @param tokenEndpointAuthMethod optional authentication method for the token endpoint
 * @param scope optional default scope string
 */
public record ClientIdMetadataDocument(
        String clientId,
        Optional<String> clientSecret,
        List<URI> redirectUris,
        List<String> grantTypes,
        Optional<String> tokenEndpointAuthMethod,
        Optional<String> scope) {
    public ClientIdMetadataDocument {
        Preconditions.isTrue(
                clientId != null && !clientId.isBlank(), "client_id must be non‑blank");
        Preconditions.isTrue(
                redirectUris != null && !redirectUris.isEmpty(),
                "redirect_uris must contain at least one URI");
        Preconditions.isTrue(
                grantTypes != null && grantTypes.contains("authorization_code"),
                "grant_types must contain \"authorization_code\"");
        redirectUris.forEach(
                uri ->
                        Preconditions.isTrue(
                                "https".equalsIgnoreCase(uri.getScheme()),
                                "redirect_uri must use HTTPS: " + uri));
    }
}
