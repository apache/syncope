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
package org.apache.syncope.core.util;

import java.lang.reflect.Field;

import org.identityconnectors.common.Base64;
import org.identityconnectors.common.security.EncryptorFactory;
import org.identityconnectors.common.security.GuardedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Help in XStream serialization of GuardedString by (de)serializing instances using the default Encryptor (which works
 * consistently across class loading) instead of a random Encryptor instance.
 *
 * @see XMLSerializer
 * @see GuardedString
 */
public class GuardedStringConverter implements Converter {

    private static final Logger LOG = LoggerFactory.getLogger(GuardedStringConverter.class);

    @Override
    public void marshal(final Object source, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        boolean readOnly = false;
        try {
            Field readOnlyField = GuardedString.class.getDeclaredField("_readOnly");
            readOnlyField.setAccessible(true);
            readOnly = readOnlyField.getBoolean(source);
        } catch (Exception e) {
            LOG.error("Could not get field value", e);
        }
        writer.startNode("readonly");
        writer.setValue(Boolean.toString(readOnly));
        writer.endNode();

        boolean disposed = false;
        try {
            Field disposedField = GuardedString.class.getDeclaredField("_disposed");
            disposedField.setAccessible(true);
            disposed = disposedField.getBoolean(source);
        } catch (Exception e) {
            LOG.error("Could not get field value", e);
        }
        writer.startNode("disposed");
        writer.setValue(Boolean.toString(disposed));
        writer.endNode();

        writer.startNode("encryptedBytes");
        final StringBuilder cleartext = new StringBuilder();
        ((GuardedString) source).access(new GuardedString.Accessor() {

            @Override
            public void access(final char[] clearChars) {
                cleartext.append(clearChars);
            }
        });
        final byte[] encryptedBytes =
                EncryptorFactory.getInstance().getDefaultEncryptor().encrypt(cleartext.toString().getBytes());
        writer.setValue(Base64.encode(encryptedBytes));
        writer.endNode();
    }

    @Override
    public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
        reader.moveDown();
        final boolean readOnly = Boolean.valueOf(reader.getValue());
        reader.moveUp();

        reader.moveDown();
        final boolean disposed = Boolean.valueOf(reader.getValue());
        reader.moveUp();

        reader.moveDown();
        final byte[] encryptedBytes = Base64.decode(reader.getValue());
        reader.moveUp();

        final byte[] clearBytes = EncryptorFactory.getInstance().getDefaultEncryptor().decrypt(encryptedBytes);

        GuardedString dest = new GuardedString(new String(clearBytes).toCharArray());

        try {
            Field readOnlyField = GuardedString.class.getDeclaredField("_readOnly");
            readOnlyField.setAccessible(true);
            readOnlyField.setBoolean(dest, readOnly);
        } catch (Exception e) {
            LOG.error("Could not set field value to {}", readOnly, e);
        }

        try {
            Field readOnlyField = GuardedString.class.getDeclaredField("_disposed");
            readOnlyField.setAccessible(true);
            readOnlyField.setBoolean(dest, disposed);
        } catch (Exception e) {
            LOG.error("Could not set field value to {}", disposed, e);
        }

        return dest;
    }

    @Override
    public boolean canConvert(final Class type) {
        return type.equals(GuardedString.class);
    }
}
