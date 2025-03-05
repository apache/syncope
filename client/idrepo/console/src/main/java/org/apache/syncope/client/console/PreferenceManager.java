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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.wicket.util.cookies.CookieDefaults;
import org.apache.wicket.util.cookies.CookieUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PreferenceManager implements Serializable {

    private static final long serialVersionUID = 3581434664555284193L;

    private static final Logger LOG = LoggerFactory.getLogger(PreferenceManager.class);

    private static final String COOKIE_NAME = "syncope2ConsolePrefs";

    private static final int ONE_YEAR_TIME = 60 * 60 * 24 * 365;

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    private static final TypeReference<Map<String, String>> MAP_TYPE_REF = new TypeReference<>() {
    };

    private static final List<Integer> PAGINATOR_CHOICES = List.of(10, 25, 50);

    private static final CookieUtils COOKIE_UTILS;

    static {
        CookieDefaults cookieDefaults = new CookieDefaults();
        cookieDefaults.setMaxAge(ONE_YEAR_TIME);
        COOKIE_UTILS = new CookieUtils(cookieDefaults);
    }

    public static List<Integer> getPaginatorChoices() {
        return PAGINATOR_CHOICES;
    }

    private static Map<String, String> getPrefs(final String value) {
        Map<String, String> prefs;
        try {
            if (StringUtils.isNotBlank(value)) {
                prefs = MAPPER.readValue(value, MAP_TYPE_REF);
            } else {
                throw new Exception("Invalid cookie value '" + value + '\'');
            }
        } catch (Exception e) {
            LOG.debug("No preferences found", e);
            prefs = new HashMap<>();
        }

        return prefs;
    }

    private static String setPrefs(final Map<String, String> prefs) throws IOException {
        StringWriter writer = new StringWriter();
        MAPPER.writeValue(writer, prefs);

        return writer.toString();
    }

    public static String get(final String key) {
        String result = null;

        String prefString = COOKIE_UTILS.load(COOKIE_NAME);
        if (prefString != null) {
            Map<String, String> prefs = getPrefs(new String(Base64.getDecoder().decode(prefString.getBytes())));
            result = prefs.get(key);
        }

        return result;
    }

    public static Integer getPaginatorRows(final String key) {
        Integer result = getPaginatorChoices().getFirst();

        String value = get(key);
        if (value != null) {
            result = NumberUtils.toInt(value, 10);
        }

        return result;
    }

    public static List<String> getList(final String key) {
        final List<String> result = new ArrayList<>();

        final String compound = get(key);

        if (StringUtils.isNotBlank(compound)) {
            String[] items = compound.split(";");
            result.addAll(List.of(items));
        }

        return result;
    }

    public static void set(final Map<String, List<String>> prefs) {
        Map<String, String> current = new HashMap<>();

        String prefString = COOKIE_UTILS.load(COOKIE_NAME);
        if (prefString != null) {
            current.putAll(getPrefs(new String(Base64.getDecoder().decode(prefString.getBytes()))));
        }

        // after retrieved previous setting in order to overwrite the key ...
        prefs.forEach((key, values) -> current.put(key, String.join(";", values)));

        try {
            COOKIE_UTILS.save(COOKIE_NAME, Base64.getEncoder().encodeToString(setPrefs(current).getBytes()));
        } catch (IOException e) {
            LOG.error("Could not save {} info: {}", PreferenceManager.class.getSimpleName(), current, e);
        }
    }

    public static void set(final String key, final String value) {
        String prefString = COOKIE_UTILS.load(COOKIE_NAME);

        final Map<String, String> current = new HashMap<>();
        if (prefString != null) {
            current.putAll(getPrefs(new String(Base64.getDecoder().decode(prefString.getBytes()))));
        }

        // after retrieved previous setting in order to overwrite the key ...
        current.put(key, value);

        try {
            COOKIE_UTILS.save(COOKIE_NAME, Base64.getEncoder().encodeToString(setPrefs(current).getBytes()));
        } catch (IOException e) {
            LOG.error("Could not save {} info: {}", PreferenceManager.class.getSimpleName(), current, e);
        }
    }

    public static void setList(final String key, final List<String> values) {
        set(key, String.join(";", values));
    }

    public static void setList(final Map<String, List<String>> prefs) {
        set(prefs);
    }

    private PreferenceManager() {
        // private constructor for static utility class
    }
}
