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
package org.apache.syncope.client.console.panels;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Base64;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.jackson.SyncopeJsonMapper;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.wicket.PageReference;
import org.bouncycastle.util.io.pem.PemReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public class ParametersModalPanel extends AbstractModalPanel<ConfParam> {

    private static final long serialVersionUID = 4024126489500665435L;

    protected static final Set<String> BASE64_EXCEPTIONS = Set.of("username");

    protected static final JsonMapper JSON_MAPPER = new SyncopeJsonMapper();

    protected static boolean isDate(final String value) {
        try {
            DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse(value);
            return true;
        } catch (ParseException pe) {
            return false;
        }
    }

    protected static boolean isBase64(final String value) {
        try {
            Base64.getDecoder().decode(value);
            return value.length() % 4 == 0 && !BASE64_EXCEPTIONS.contains(value);
        } catch (IllegalArgumentException iae) {
            return false;
        }
    }

    protected static boolean isJSON(final String value) {
        try {
            JSON_MAPPER.readTree(value);
            return true;
        } catch (JacksonException e) {
            return false;
        }
    }

    protected static boolean isXML(final String value) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.newSAXParser().getXMLReader().parse(new InputSource(new StringReader(value)));
            return true;
        } catch (IOException | ParserConfigurationException | SAXException e) {
            return false;
        }
    }

    protected static boolean isPEM(final String value) {
        try (PemReader reader = new PemReader(Reader.of(value))) {
            return reader.readPemObject() != null;
        } catch (IOException e) {
            return false;
        }
    }

    protected final ParametersWizardPanel.ParametersForm form;

    public ParametersModalPanel(
            final BaseModal<ConfParam> modal,
            final ConfParam param,
            final ConfParamOps confParamOps,
            final AjaxWizard.Mode mode,
            final PageReference pageRef) {

        super(modal, pageRef);

        PlainSchemaTO schema = new PlainSchemaTO();
        schema.setMultivalue(param.isMultivalue());
        schema.setMimeType(MediaType.APPLICATION_OCTET_STREAM);

        if (param.isInstance(Boolean.class)) {
            schema.setType(AttrSchemaType.Boolean);
        } else if (param.isInstance(Integer.class) || param.isInstance(Long.class)) {
            schema.setType(AttrSchemaType.Long);
        } else if (param.isInstance(Float.class) || param.isInstance(Double.class)) {
            schema.setType(AttrSchemaType.Double);
        } else // attempt to guess type from content: otherwise, it's bare String
        if (!param.getValues().isEmpty()) {
            // 1. is it Date?
            if (isDate(param.getValues().getFirst().toString())) {
                schema.setType(AttrSchemaType.Date);
            } else // 2. does it look like Base64?
            if (isBase64(param.getValues().getFirst().toString())) {
                schema.setType(AttrSchemaType.Binary);
                String value = new String(Base64.getDecoder().decode(param.getValues().getFirst().toString()));

                // 3. is it JSON?
                if (isJSON(value)) {
                    schema.setMimeType(MediaType.APPLICATION_JSON);
                } else // 4. is it XML?
                if (isXML(value)) {
                    schema.setMimeType(MediaType.APPLICATION_XML);
                } else // 5. is it PEM?
                if (isPEM(value)) {
                    schema.setMimeType("application/x-pem-file");
                }
            } else {
                schema.setType(AttrSchemaType.String);
            }
        }

        modal.size(schema.getType() == AttrSchemaType.Binary ? Modal.Size.Extra_large : Modal.Size.Default);

        form = new ParametersWizardPanel.ParametersForm(schema, param);
        add(new ParametersWizardPanel(form, confParamOps, pageRef).build("parametersCreateWizardPanel", mode));
    }

    @Override
    public final ConfParam getItem() {
        return form.getParam();
    }
}
