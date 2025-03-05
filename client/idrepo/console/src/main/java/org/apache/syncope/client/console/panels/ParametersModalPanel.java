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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Base64;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.wicket.PageReference;
import org.bouncycastle.util.io.pem.PemReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ParametersModalPanel extends AbstractModalPanel<ConfParam> {

    private static final long serialVersionUID = 4024126489500665435L;

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private static final SAXParserFactory SAX_PARSER_FACTORY = SAXParserFactory.newInstance();

    private static boolean isDate(final String value) {
        try {
            DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.parse(value);
            return true;
        } catch (ParseException pe) {
            return false;
        }
    }

    private static boolean isBase64(final String value) {
        try {
            Base64.getDecoder().decode(value);
            return value.length() % 4 == 0;
        } catch (IllegalArgumentException iae) {
            return false;
        }
    }

    private static boolean isJSON(final String value) {
        try {
            JSON_MAPPER.readTree(value);
            return true;
        } catch (JsonProcessingException jpe) {
            return false;
        }
    }

    private static boolean isXML(final String value) {
        try {
            SAX_PARSER_FACTORY.newSAXParser().getXMLReader().parse(new InputSource(new StringReader(value)));
            return true;
        } catch (IOException | ParserConfigurationException | SAXException xmle) {
            return false;
        }
    }

    private static boolean isPEM(final String value) {
        try (PemReader reader = new PemReader(new StringReader(value))) {
            return reader.readPemObject() != null;
        } catch (IOException e) {
            return false;
        }
    }

    private final ParametersWizardPanel.ParametersForm form;

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
        if (param.getSchema() != null) {
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
        }

        if (schema.getType() == AttrSchemaType.Binary) {
            modal.size(Modal.Size.Extra_large);
        } else {
            modal.size(Modal.Size.Default);
        }

        form = new ParametersWizardPanel.ParametersForm(schema, param);
        add(new ParametersWizardPanel(form, confParamOps, pageRef).build("parametersCreateWizardPanel", mode));
    }

    @Override
    public final ConfParam getItem() {
        return form.getParam();
    }
}
