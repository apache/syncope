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
package org.syncope.core.workflow;

import com.opensymphony.module.propertyset.AbstractPropertySet;
import com.opensymphony.module.propertyset.PropertyException;
import com.opensymphony.module.propertyset.PropertySet;
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
import org.syncope.core.persistence.beans.OSWorkflowProperty;
import org.syncope.core.persistence.dao.OSWorkflowPropertyDAO;
import org.w3c.dom.Document;

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
    private OSWorkflowPropertyDAO osWorkflowPropertyDAO;
    private long workflowEntryId;

    @Override
    public void init(Map config, Map args) {
        super.init(config, args);

        workflowEntryId = (Long) args.get("entryId");
        osWorkflowPropertyDAO =
                (OSWorkflowPropertyDAO) args.get("osWorkflowPropertyDAO");
    }

    @Override
    public Collection getKeys(String prefix, int i)
            throws PropertyException {

        List<OSWorkflowProperty> properties =
                osWorkflowPropertyDAO.findAll(workflowEntryId);
        Set<String> keys = new HashSet<String>();
        for (OSWorkflowProperty property : properties) {
            if (prefix != null
                    && property.getPropertyKey().startsWith(prefix)) {

                keys.add(property.getPropertyKey());
            }
        }
        return keys;
    }

    @Override
    public boolean exists(String key) throws PropertyException {
        return osWorkflowPropertyDAO.find(workflowEntryId, key) != null;
    }

    @Override
    @Transactional
    public void remove(String string) throws PropertyException {
        osWorkflowPropertyDAO.delete(workflowEntryId, string);
    }

    @Override
    protected void setImpl(int type, String key, Object value)
            throws PropertyException {

        OSWorkflowProperty property = new OSWorkflowProperty();
        property.setPropertyKey(key);
        property.setWorkflowEntryId(workflowEntryId);

        try {
            switch (type) {
                case BOOLEAN:
                    property.setBooleanValue((Boolean) value);
                    break;

                case INT:
                    property.setIntValue((Integer) value);
                    break;

                case LONG:
                    property.setLongValue((Long) value);
                    break;

                case DOUBLE:
                    property.setDoubleValue((Double) value);
                    break;

                case STRING:
                    property.setStringValue((String) value);
                    break;

                case TEXT:
                    property.setTextValue((String) value);
                    break;

                case DATE:
                    property.setDateValue((Date) value);
                    break;

                case XML:
                    break;
                case DATA:
                    break;
                case PROPERTIES:
                    break;
                case OBJECT:
                    break;
            }
        } catch (Throwable t) {
            LOG.error("While setting property value", t);
            throw new PropertyException(t.getMessage());
        }

        osWorkflowPropertyDAO.save(property);
    }

    @Override
    protected Object get(int type, String key) throws PropertyException {
        OSWorkflowProperty property =
                osWorkflowPropertyDAO.find(workflowEntryId, key);

        Object result = null;
        switch (type) {
            case BOOLEAN:
                result = property.getBooleanValue();
                break;

            case INT:
                result = property.getIntValue();
                break;

            case LONG:
                result = property.getLongValue();
                break;

            case DOUBLE:
                result = property.getDoubleValue();
                break;

            case STRING:
                result = property.getStringValue();
                break;

            case TEXT:
                result = property.getTextValue();
                break;

            case DATE:
                result = property.getDateValue();
                break;

            case XML:
                break;
            case DATA:
                break;
            case PROPERTIES:
                break;
            case OBJECT:
                break;
        }

        return result;
    }

    @Override
    public int getType(String key) throws PropertyException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getObject(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Properties getProperties(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Document getXML(String key) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
