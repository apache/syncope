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
package org.apache.syncope.client.console;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.util.cookies.CookieDefaults;
import org.apache.wicket.util.cookies.CookieUtils;
import org.apache.wicket.util.crypt.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreferenceManager implements Serializable {

    private static final long serialVersionUID = 3581434664555284193L;

    private static final Logger LOG = LoggerFactory.getLogger(PreferenceManager.class);

    private static final String COOKIE_NAME = "syncope2ConsolePrefs";

    private static final int ONE_YEAR_TIME = 60 * 60 * 24 * 365;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final TypeReference<Map<String, String>> MAP_TYPE_REF = new TypeReference<Map<String, String>>() {
    };

    private static final List<Integer> PAGINATOR_CHOICES = Arrays.asList(new Integer[] { 10, 25, 50 });

    private static final CookieUtils COOKIE_UTILS;

    static {
        CookieDefaults cookieDefaults = new CookieDefaults();
        cookieDefaults.setMaxAge(ONE_YEAR_TIME);
        COOKIE_UTILS = new CookieUtils(cookieDefaults);
    }

    public List<Integer> getPaginatorChoices() {
        return PAGINATOR_CHOICES;
    }

    private Map<String, String> getPrefs(final String value) {
        Map<String, String> prefs;
        try {
            if (StringUtils.isNotBlank(value)) {
                prefs = MAPPER.readValue(value, MAP_TYPE_REF);
            } else {
                throw new Exception("Invalid cookie value '" + value + "'");
            }
        } catch (Exception e) {
            LOG.debug("No preferences found", e);
            prefs = new HashMap<>();
        }

        return prefs;
    }

    private String setPrefs(final Map<String, String> prefs) throws IOException {
        StringWriter writer = new StringWriter();
        MAPPER.writeValue(writer, prefs);

        return writer.toString();
    }

    public String get(final Request request, final String key) {
        String result = null;

        String prefString = COOKIE_UTILS.load(COOKIE_NAME);
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
            result = NumberUtils.toInt(value, 10);
        }

        return result;
    }

    public List<String> getList(final Request request, final String key) {
        final List<String> result = new ArrayList<>();

        final String compound = get(request, key);

        if (StringUtils.isNotBlank(compound)) {
            String[] items = compound.split(";");
            result.addAll(Arrays.asList(items));
        }

        return result;
    }

    public void set(final Request request, final Response response, final Map<String, List<String>> prefs) {
        Map<String, String> current = new HashMap<>();

        String prefString = COOKIE_UTILS.load(COOKIE_NAME);
        if (prefString != null) {
            current.putAll(getPrefs(new String(Base64.decodeBase64(prefString.getBytes()))));
        }

        // after retrieved previous setting in order to overwrite the key ...
        for (Map.Entry<String, List<String>> entry : prefs.entrySet()) {
            current.put(entry.getKey(), StringUtils.join(entry.getValue(), ";"));
        }

        try {
            COOKIE_UTILS.save(COOKIE_NAME, new String(Base64.encodeBase64(setPrefs(current).getBytes())));
        } catch (IOException e) {
            LOG.error("Could not save {} info: {}", getClass().getSimpleName(), current, e);
        }
    }

    public void set(final Request request, final Response response, final String key, final String value) {
        String prefString = COOKIE_UTILS.load(COOKIE_NAME);

        final Map<String, String> current = new HashMap<>();
        if (prefString != null) {
            current.putAll(getPrefs(new String(Base64.decodeBase64(prefString.getBytes()))));
        }

        // after retrieved previous setting in order to overwrite the key ...
        current.put(key, value);

        try {
            COOKIE_UTILS.save(COOKIE_NAME, new String(Base64.encodeBase64(setPrefs(current).getBytes())));
        } catch (IOException e) {
            LOG.error("Could not save {} info: {}", getClass().getSimpleName(), current, e);
        }
    }

    public void setList(final Request request, final Response response, final String key, final List<String> values) {
        set(request, response, key, StringUtils.join(values, ";"));
    }

    public void setList(final Request request, final Response response, final Map<String, List<String>> prefs) {
        set(request, response, prefs);
    }
}
