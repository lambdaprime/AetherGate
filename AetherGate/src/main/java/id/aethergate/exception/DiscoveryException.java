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
package id.aethergate.exception;

/**
 * Represents an error that occurs during the discovery phase of the AetherGate workflow.
 *
 * <p>This exception is thrown when fetching or validating the Protected Resource Metadata (PRM) or
 * Authorization Server metadata fails, for example due to network errors, invalid content, or
 * missing required fields.
 */
public class DiscoveryException extends RuntimeException {
    /**
     * Constructs a new {@code DiscoveryException} with the specified detail message.
     *
     * @param message the detail message
     */
    public DiscoveryException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code DiscoveryException} with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public DiscoveryException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code DiscoveryException} with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public DiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
