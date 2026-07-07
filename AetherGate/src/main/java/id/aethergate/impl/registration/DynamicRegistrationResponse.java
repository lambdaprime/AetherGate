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

/**
 * Response received from a successful dynamic registration request.
 *
 * @author lambdaprime intid@protonmail.com
 */
public record DynamicRegistrationResponse(
        String clientId,
        String clientSecret,
        String registrationAccessToken,
        String registrationClientUri,
        long clientIdIssuedAt,
        long clientSecretExpiresAt) {}
