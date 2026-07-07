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
package id.aethergate.impl.model;

import id.xfunction.Preconditions;
import java.util.List;
import java.util.Optional;

/**
 * Represent the JSON response from the OAuth 2.1 token endpoint.
 *
 * @author lambdaprime intid@protonmail.com
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Optional<String> refreshToken,
        Optional<String> scope,
        List<String> aud) {

    public TokenResponse {
        Preconditions.isTrue(
                accessToken != null && !accessToken.isBlank(), "access_token must be non-blank");
        Preconditions.isTrue(
                tokenType != null && tokenType.equalsIgnoreCase("Bearer"),
                "token_type must be 'Bearer'");
        Preconditions.isTrue(expiresIn > 0, "expires_in must be positive");
        if (aud == null) aud = List.of();
    }
}
