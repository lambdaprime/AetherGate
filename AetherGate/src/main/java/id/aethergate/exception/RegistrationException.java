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

public class RegistrationException extends RuntimeException {
    /** Constructs a new {@code RegistrationException} with the specified detail message. */
    public RegistrationException(String message) {
        super(message);
    }

    /** Constructs a new {@code RegistrationException} with the specified cause. */
    public RegistrationException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code RegistrationException} with the specified detail message and cause.
     */
    public RegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
