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
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import tools.jackson.databind.MapperFeature;
import tools.jackson.dataformat.xml.XmlFactory;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.XmlReadFeature;
import tools.jackson.dataformat.xml.XmlWriteFeature;

public class SyncopeXmlMapper extends XmlMapper {

    private static final long serialVersionUID = 1022020055828974308L;

    public SyncopeXmlMapper() {
        super(new Builder(new XmlFactory() {

            private static final long serialVersionUID = 1022020055828974306L;

            @Override
            protected void _initFactories(final XMLInputFactory xmlIn, final XMLOutputFactory xmlOut) {
                super._initFactories(xmlIn, xmlOut);
                xmlOut.setProperty(WstxOutputFactory.P_AUTOMATIC_EMPTY_ELEMENTS, Boolean.FALSE);
            }
        }).findAndAddModules().
                enable(MapperFeature.USE_GETTERS_AS_SETTERS).
                enable(XmlWriteFeature.WRITE_XML_DECLARATION).
                enable(XmlReadFeature.EMPTY_ELEMENT_AS_NULL));
    }
}
