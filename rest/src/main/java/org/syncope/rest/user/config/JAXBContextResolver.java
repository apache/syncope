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
package org.syncope.rest.user.config;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import org.syncope.rest.user.jaxb.AttributeValues;
import org.syncope.rest.user.jaxb.Attributes;
import org.syncope.rest.user.jaxb.SearchParameters;
import org.syncope.rest.user.jaxb.SearchResults;

@Provider
public final class JAXBContextResolver implements ContextResolver<JAXBContext> {

    private final JAXBContext context;
    private final Set<Class> types;
    private final Class[] cTypes = {AttributeValues.class,
        Attributes.class, SearchParameters.class, SearchResults.class};

    public JAXBContextResolver() throws Exception {
        this.types = new HashSet(Arrays.asList(cTypes));
        this.context = new JSONJAXBContext(
                JSONConfiguration.natural().build(), cTypes);
    }

    public JAXBContext getContext(Class<?> objectType) {
        return (types.contains(objectType)) ? context : null;
    }
}

