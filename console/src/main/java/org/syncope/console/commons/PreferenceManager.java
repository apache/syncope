/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.commons;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.Cookie;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.util.crypt.Base64;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class PreferenceManager {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            PreferenceManager.class);

    private static final int ONE_YEAR_TIME = 60 * 60 * 24 * 365;

    private static final TypeReference MAP_TYPE_REF =
            new TypeReference<Map<String, String>>() {
            };

    private static final List<Integer> PAGINATOR_CHOICES =
            Arrays.asList(new Integer[]{10, 25, 50});

    @Autowired
    private ObjectMapper mapper;

    public List<Integer> getPaginatorChoices() {
        return PAGINATOR_CHOICES;
    }

    private Map<String, String> getPrefs(final String value)
            throws IOException {

        return mapper.readValue(value, MAP_TYPE_REF);
    }

    private String setPrefs(final Map<String, String> prefs)
            throws IOException {

        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, prefs);

        return writer.toString();
    }

    public String get(final WebRequest request, final String key) {
        String result = null;

        Cookie prefCookie = request.getCookie(Constants.PREFS_COOKIE_NAME);
        if (prefCookie != null) {
            Map<String, String> prefs;
            try {
                prefs = getPrefs(new String(Base64.decodeBase64(
                        prefCookie.getValue().getBytes())));
            } catch (IOException e) {
                LOG.error("Could not get preferences from "
                        + prefCookie.getValue(), e);

                prefs = new HashMap<String, String>();
            }
            result = prefs.get(key);
        } else {
            LOG.warn("Could not find cookie " + Constants.PREFS_COOKIE_NAME);
        }

        return result;
    }

    public Integer getPaginatorRows(final WebRequest request,
            final String key) {

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

    public List<String> getList(final WebRequest request,
            final String key) {

        List<String> result = new ArrayList<String>();

        String compound = get(request, key);
        if (compound != null) {
            String[] items = compound.split(";");
            if (items != null) {
                result.addAll(Arrays.asList(items));
            } else {
                LOG.error("While exploding compund " + compound);
            }
        }

        return result;
    }

    public void set(final WebRequest request, final WebResponse response,
            final String key, final String value) {

        Cookie prefCookie = request.getCookie(Constants.PREFS_COOKIE_NAME);
        if (prefCookie == null) {
            prefCookie = new Cookie(Constants.PREFS_COOKIE_NAME, "");
        }

        Map<String, String> prefs;
        try {
            prefs = getPrefs(new String(Base64.decodeBase64(
                    prefCookie.getValue().getBytes())));
        } catch (IOException e) {
            LOG.error("Could not get preferences from "
                    + prefCookie.getValue(), e);

            prefs = new HashMap<String, String>();
        }
        prefs.put(key, value);

        try {
            prefCookie.setValue(new String(Base64.encodeBase64(
                    setPrefs(prefs).getBytes())));
        } catch (IOException e) {
            LOG.error("Could not set preferences from " + prefs);
        }

        prefCookie.setMaxAge(ONE_YEAR_TIME);
        response.addCookie(prefCookie);
    }

    public void setList(final WebRequest request, final WebResponse response,
            final String key, final List<String> values) {

        set(request, response, key,
                StringUtils.join(values.toArray(new String[]{}), ";"));
    }
}
