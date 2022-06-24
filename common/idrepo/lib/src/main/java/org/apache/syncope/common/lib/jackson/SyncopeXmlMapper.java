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
package org.apache.syncope.common.lib.jackson;

import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

public class SyncopeXmlMapper extends XmlMapper {

    private static final long serialVersionUID = 1022020055828974308L;

    public SyncopeXmlMapper() {
        super(new XmlFactory() {

            private static final long serialVersionUID = 1022020055828974306L;

            @Override
            protected void _initFactories(final XMLInputFactory xmlIn, final XMLOutputFactory xmlOut) {
                super._initFactories(xmlIn, xmlOut);
                xmlOut.setProperty(WstxOutputFactory.P_AUTOMATIC_EMPTY_ELEMENTS, Boolean.FALSE);
            }
        });

        findAndRegisterModules();

        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);

        configOverride(List.class).setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY));
        configOverride(Set.class).setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY));
        configOverride(Map.class).setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY));

        enable(ToXmlGenerator.Feature.WRITE_XML_DECLARATION);
        enable(FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL);
    }
}
