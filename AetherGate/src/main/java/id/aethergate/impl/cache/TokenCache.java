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
package id.aethergate.impl.cache;

import id.aethergate.impl.model.Token;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Stores access tokens in memory with a size limit and LRU eviction policy.
 *
 * <p>The cache key is composed of the client identifier, the resource URI and the requested scopes.
 * Tokens are automatically removed when they expire.
 *
 * @author lambdaprime intid@protonmail.com
 */
public final class TokenCache {

    /** Simple immutable key used for cache look‑ups. */
    public record CacheKey(String clientId, URI resourceUri, List<String> scopes) {}

    private static final int DEFAULT_MAX_ENTRIES = 1_000;

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<CacheKey, Token> store;
    private final int maxEntries;

    /** Creates a cache with the default maximum number of entries. */
    public TokenCache() {
        this(DEFAULT_MAX_ENTRIES);
    }

    /**
     * Creates a cache with a custom size limit.
     *
     * @param maxEntries maximum number of cached tokens before LRU eviction occurs
     */
    public TokenCache(int maxEntries) {
        this.maxEntries = maxEntries;
        // accessOrder=true enables LRU ordering for removal of eldest entries.
        this.store =
                new LinkedHashMap<>(maxEntries, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<CacheKey, Token> eldest) {
                        return size() > TokenCache.this.maxEntries;
                    }
                };
    }

    public boolean isEmpty() {
        return store.isEmpty();
    }

    /**
     * Retrieves a valid access token for the given key.
     *
     * @return optional containing the token if present and not expired, otherwise empty
     */
    public Optional<Token> get(CacheKey cacheKey) {
        lock.lock();
        try {
            var entry = store.get(cacheKey);
            if (entry == null) {
                return Optional.empty();
            }
            // Remove the entry if it has expired.
            if (entry.expiresAt().isBefore(Instant.now())) {
                store.remove(cacheKey);
                return Optional.empty();
            }
            return Optional.of(entry);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Stores a token in the cache.
     *
     * @param key cache key identifying the client/resource/scopes
     */
    public void put(CacheKey key, Token token) {
        lock.lock();
        try {
            store.put(key, token);
        } finally {
            lock.unlock();
        }
    }

    /** Removes all entries whose expiry time is in the past. */
    public void evictExpired() {
        lock.lock();
        try {
            var now = Instant.now();
            store.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
        } finally {
            lock.unlock();
        }
    }
}
