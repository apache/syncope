/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.console.commons;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.cookies.CookieDefaults;
import org.apache.wicket.util.cookies.CookieUtils;
import org.apache.wicket.util.crypt.Base64;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

public class PreferenceManager {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PreferenceManager.class);

    private static final String PREFMAN_KEY = "prefMan";

    private static final int ONE_YEAR_TIME = 60 * 60 * 24 * 365;

    private static final TypeReference MAP_TYPE_REF = new TypeReference<Map<String, String>>() {
    };

    private static final List<Integer> PAGINATOR_CHOICES = Arrays.asList(new Integer[]{10, 25, 50});

    @Autowired
    private ObjectMapper mapper;

    private final CookieUtils cookieUtils;

    public PreferenceManager() {
        CookieDefaults cookieDefaults = new CookieDefaults();
        cookieDefaults.setMaxAge(ONE_YEAR_TIME);
        cookieUtils = new CookieUtils(cookieDefaults);
    }

    public List<Integer> getPaginatorChoices() {
        return PAGINATOR_CHOICES;
    }

    private Map<String, String> getPrefs(final String value) {
        Map<String, String> prefs;
        try {
            if (StringUtils.hasText(value)) {
                prefs = mapper.readValue(value, MAP_TYPE_REF);
            } else {
                throw new Exception("Invalid cookie value '" + value + "'");
            }
        } catch (Exception e) {
            LOG.debug("No preferences found", e);
            prefs = new HashMap<String, String>();
        }

        return prefs;
    }

    private String setPrefs(final Map<String, String> prefs) throws IOException {
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, prefs);

        return writer.toString();
    }

    public String get(final Request request, final String key) {
        String result = null;

        String prefString = cookieUtils.load(PREFMAN_KEY);
        if (prefString != null) {
            final Map<String, String> prefs = getPrefs(new String(Base64.decodeBase64(prefString.getBytes())));
            result = prefs.get(key);
        }

        return result;
    }

    public Integer getPaginatorRows(final Request request, final String key) {
        Integer result = getPaginatorChoices().get(0);

        String value = get(request, key);
        if (value != null) {
            try {
                result = Integer.valueOf(value);
            } catch (NumberFormatException e) {
                LOG.error("Unparsable value " + value, e);
            }
        }

        return result;
    }

    public List<String> getList(final Request request, final String key) {
        final List<String> result = new ArrayList<String>();

        final String compound = get(request, key);

        if (StringUtils.hasText(compound)) {
            String[] items = compound.split(";");
            result.addAll(Arrays.asList(items));
        }

        return result;
    }

    public void set(final Request request, final Response response, final Map<String, List<String>> prefs) {
        String prefString = cookieUtils.load(PREFMAN_KEY);

        final Map<String, String> current = new HashMap<String, String>();
        if (prefString != null) {
            current.putAll(getPrefs(new String(Base64.decodeBase64(prefString.getBytes()))));
        }

        // after retrieved previous setting in order to overwrite the key ...
        for (Entry<String, List<String>> entry : prefs.entrySet()) {
            current.put(entry.getKey(), StringUtils.collectionToDelimitedString(entry.getValue(), ";"));
        }

        try {
            cookieUtils.save(PREFMAN_KEY, new String(Base64.encodeBase64(setPrefs(current).getBytes())));
        } catch (IOException e) {
            LOG.error("Could not save {} info: {}", getClass().getSimpleName(), current, e);
        }
    }

    public void set(final Request request, final Response response, final String key, final String value) {
        String prefString = cookieUtils.load(PREFMAN_KEY);

        final Map<String, String> current = new HashMap<String, String>();
        if (prefString != null) {
            current.putAll(getPrefs(new String(Base64.decodeBase64(prefString.getBytes()))));
        }

        // after retrieved previous setting in order to overwrite the key ...
        current.put(key, value);

        try {
            cookieUtils.save(PREFMAN_KEY, new String(Base64.encodeBase64(setPrefs(current).getBytes())));
        } catch (IOException e) {
            LOG.error("Could not save {} info: {}", getClass().getSimpleName(), current, e);
        }
    }

    public void setList(final Request request, final Response response, final String key, final List<String> values) {
        set(request, response, key, StringUtils.collectionToDelimitedString(values, ";"));
    }

    public void setList(final Request request, final Response response, final Map<String, List<String>> prefs) {
        set(request, response, prefs);
    }
}
