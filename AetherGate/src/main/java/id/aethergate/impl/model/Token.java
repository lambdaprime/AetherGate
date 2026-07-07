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
import java.time.Instant;
import java.util.Optional;

/**
 * Holds an access token, its absolute expiry instant and an optional refresh token.
 *
 * @param accessToken the bearer token value; must be non‑blank
 * @param expiresAt the instant when the token expires; must be in the future
 * @param refreshToken an optional refresh token; may be empty
 * @author lambdaprime intid@protonmail.com
 */
public record Token(String accessToken, Instant expiresAt, Optional<String> refreshToken) {
    public Token {
        Preconditions.isTrue(
                accessToken != null && !accessToken.isBlank(), "accessToken must be non‑blank");
        Preconditions.isTrue(expiresAt != null, "expiresAt must not be null");
        Preconditions.isTrue(expiresAt.isAfter(Instant.now()), "expiresAt must be in the future");
        // Normalize refresh token to an empty Optional if null
        refreshToken = Optional.ofNullable(refreshToken.orElse(null));
    }
}
