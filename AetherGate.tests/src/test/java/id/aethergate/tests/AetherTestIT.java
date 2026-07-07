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
package id.aethergate.tests;

import id.aethergate.AetherGate;
import id.aethergate.AuthorizationSession;
import id.aethergate.ClientConfig;
import id.aethergate.exception.DiscoveryException;
import org.junit.jupiter.api.Test;

/**
 * @author lambdaprime intid@protonmail.com
 */
public class AetherTestIT {

    @Test
    public void test() {}

    public static void main(String[] args) {
        try {
            var aetherGate = new AetherGate();
            AuthorizationSession session =
                    aetherGate.openSession(
                            ClientConfig.builder()
                                    .serverBaseUrl("http://localhost:8001/mcp")
                                    .redirectUrl("http://localhost:8002/callback")
                                    .scopes("user")
                                    .isInsecure(true)
                                    .build());
            System.out.println(session);
            var token = aetherGate.obtainAccessToken(session);
            System.out.println(token);
        } catch (DiscoveryException e) {
            System.out.println(
                    "server does not support OAuth 2.1-based authorization: " + e.getMessage());
        }
    }
}
