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
package id.aethergate.impl.flow;

import id.aethergate.UserInteraction;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation that asks user to follow the authorize URL and paste the redirect URL
 * back. It extracts query parameters using simple URL decoding.
 *
 * {@snippet lang="plain" :
 * Open following URL in the browser and paste back the obtained redirect URL:
 * http://localhost:9000/authorize?response_type=code&client_id=xxxxxxxx-2832-4fb0-xxxx-593xda1xxxxx&redirect_uri=http%3A%2F%2Flocalhost%3A8002%2Fcallback&scope=user&code_challenge_method=S256&code_challenge=piUbzoxxxxxxxxxxxxxxxxxxxxxxxxkrrq5b_IUOuGY&resource=http%3A%2F%2Flocalhost%3A8001%2Fmcp&state=xxxxxxxxxxx
 * }
 *
 * <p>Once users open the authorize URL in the browser and authenticate they will be redirected to a
 * localhost URL which they need to paste back to <b>AetherGate</b> to complete the authorization
 * flow:
 *
 * {@snippet lang="plain" :
 * http://localhost:8002/callback?code=mcp_a45bxxxxxxxxxxxxxxxxxxxx00dbece3&state=xxxxxxxxxxx
 * }
 *
 * @author lambdaprime intid@protonmail.com
 */
public class DefaultUserInteraction implements UserInteraction {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultUserInteraction.class);

    @Override
    public URI startAuthorization(URI authUrl) throws IOException {
        IO.println(
                "Open following URL in the browser and paste back the obtained redirect" + " URL:");
        IO.println(authUrl);
        return URI.create(IO.readln());
    }

    @Override
    public Map<String, String> extractParameters(URI uri) {
        var query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return Collections.emptyMap();
        }
        var map = new HashMap<String, String>();
        for (var pair : query.split("&")) {
            var idx = pair.indexOf('=');
            if (idx > 0) {
                var key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                var value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(key, value);
            } else {
                var key = URLDecoder.decode(pair, StandardCharsets.UTF_8);
                map.put(key, "");
            }
        }
        return Collections.unmodifiableMap(map);
    }
}
