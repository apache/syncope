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
package jpasymphony.module.propertyset.jpa;

import com.opensymphony.module.propertyset.AbstractPropertySet;
import com.opensymphony.module.propertyset.PropertyException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;
import jpasymphony.beans.JPAPropertySetItem;
import jpasymphony.dao.JPAPropertySetItemDAO;
import jpasymphony.workflow.spi.jpa.JPAPropertySetDelegate;

/**
 * The JPAPropertySet is a PropertySet implementation that
 * will store any primitive or object via JPA.
 *
 * @see com.opensymphony.module.propertyset.PropertySet
 */
public class JPAPropertySet extends AbstractPropertySet {

    /**
     * Logger.
     */
    protected static final Logger LOG =
            LoggerFactory.getLogger(JPAPropertySet.class);

    private JPAPropertySetItemDAO propertySetItemDAO;

    private long workflowEntryId;

    @Override
    public void init(final Map config, final Map args) {
        super.init(config, args);

        workflowEntryId = (Long) args.get(JPAPropertySetDelegate.ENTRY_ID);
        propertySetItemDAO =
                (JPAPropertySetItemDAO) args.get(JPAPropertySetDelegate.DAO);
    }

    @Override
    public Collection getKeys()
            throws PropertyException {

        return getKeys(null, Integer.MIN_VALUE);
    }

    @Override
    public Collection getKeys(final int type)
            throws PropertyException {

        return getKeys(null, type);
    }

    @Override
    public Collection getKeys(final String prefix)
            throws PropertyException {

        return getKeys(prefix, Integer.MIN_VALUE);
    }

    @Override
    public Collection getKeys(final String prefix, final int type)
            throws PropertyException {

        List<JPAPropertySetItem> properties =
                propertySetItemDAO.findAll(workflowEntryId);

        Set<String> keys = new HashSet<String>();
        for (JPAPropertySetItem property : properties) {
            if (prefix != null
                    && property.getPropertyKey().startsWith(prefix)) {
                keys.add(property.getPropertyKey());
            }
            if (type != Integer.MIN_VALUE && property.getType() == type) {
                keys.add(property.getPropertyKey());
            }
        }

        return keys;
    }

    @Override
    public boolean exists(final String key)
            throws PropertyException {

        return propertySetItemDAO.find(workflowEntryId, key) != null;
    }

    @Override
    @Transactional
    public void remove(final String key)
            throws PropertyException {

        propertySetItemDAO.delete(workflowEntryId, key);
    }

    @Override
    protected void setImpl(final int type, final String key, final Object value)
            throws PropertyException {

        JPAPropertySetItem propertySet = new JPAPropertySetItem();
        propertySet.setPropertyKey(key);
        propertySet.setWorkflowEntryId(workflowEntryId);
        propertySet.setType(type);

        try {
            switch (type) {
                case BOOLEAN:
                    propertySet.setBooleanValue((Boolean) value);
                    break;

                case INT:
                    propertySet.setIntValue((Integer) value);
                    break;

                case LONG:
                    propertySet.setLongValue((Long) value);
                    break;

                case DOUBLE:
                    propertySet.setDoubleValue((Double) value);
                    break;

                case STRING:
                    propertySet.setStringValue((String) value);
                    break;

                case TEXT:
                    propertySet.setTextValue((String) value);
                    break;

                case DATE:
                    propertySet.setDateValue((Date) value);
                    break;

                case XML:
                    DOMImplementationRegistry domir =
                            DOMImplementationRegistry.newInstance();
                    DOMImplementationLS domils =
                            (DOMImplementationLS) domir.getDOMImplementation(
                            "LS");
                    LSSerializer lss = domils.createLSSerializer();

                    propertySet.setTextValue(lss.writeToString(
                            (Document) value));
                    break;

                case PROPERTIES:
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ((Properties) value).storeToXML(baos, "");
                    baos.close();

                    propertySet.setDataValue(baos.toByteArray());
                    break;

                case DATA:
                    propertySet.setDataValue((byte[]) value);
                    break;

                case OBJECT:
                    baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(value);
                    oos.close();

                    propertySet.setDataValue(baos.toByteArray());
                    break;
            }
        } catch (Throwable t) {
            LOG.error("While setting property value", t);
            throw new PropertyException(t.getMessage());
        }

        propertySetItemDAO.save(propertySet);
    }

    @Override
    protected Object get(int type, String key)
            throws PropertyException {
        JPAPropertySetItem propertySet =
                propertySetItemDAO.find(workflowEntryId, key);

        Object result = null;
        try {
            switch (type) {
                case BOOLEAN:
                    result = propertySet.getBooleanValue();
                    break;

                case INT:
                    result = propertySet.getIntValue();
                    break;

                case LONG:
                    result = propertySet.getLongValue();
                    break;

                case DOUBLE:
                    result = propertySet.getDoubleValue();
                    break;

                case STRING:
                    result = propertySet.getStringValue();
                    break;

                case TEXT:
                    result = propertySet.getTextValue();
                    break;

                case DATE:
                    result = propertySet.getDateValue();
                    break;

                case XML:
                    DOMImplementationRegistry domir =
                            DOMImplementationRegistry.newInstance();
                    DOMImplementationLS domils =
                            (DOMImplementationLS) domir.getDOMImplementation(
                            "LS");
                    LSInput lsi = domils.createLSInput();
                    lsi.setStringData(propertySet.getTextValue());
                    LSParser lsp = domils.createLSParser(
                            DOMImplementationLS.MODE_SYNCHRONOUS, null);

                    result = lsp.parse(lsi);
                    break;

                case PROPERTIES:
                    ByteArrayInputStream bais = new ByteArrayInputStream(
                            propertySet.getDataValue());
                    Properties props = new Properties();
                    props.loadFromXML(bais);

                    result = props;
                    break;

                case DATA:
                    result = propertySet.getDataValue();
                    break;

                case OBJECT:
                    bais = new ByteArrayInputStream(propertySet.getDataValue());
                    ObjectInputStream ois = new ObjectInputStream(bais);

                    result = ois.readObject();
                    break;
            }
        } catch (Throwable t) {
            LOG.error("While fetching property value", t);
            throw new PropertyException(t.getMessage());
        }

        return result;
    }

    @Override
    public int getType(String key)
            throws PropertyException {
        JPAPropertySetItem propertySet =
                propertySetItemDAO.find(workflowEntryId, key);
        if (propertySet == null) {
            throw new PropertyException("Key '" + key + "' "
                    + "not found for workflow entry id " + workflowEntryId);
        }

        return propertySet.getType();
    }
}
