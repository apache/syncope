package org.syncope.identityconnectors.bundles.staticwebservice;

import java.util.ArrayList;
import org.syncope.identityconnectors.bundles.staticwebservice.provisioning.interfaces.Provisioning;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.ResolveUsernameOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.SyncOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.identityconnectors.bundles.staticwebservice.utilities.AttributeType;
import org.syncope.identityconnectors.bundles.staticwebservice.exceptions.ProvisioningException;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSAttribute;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSAttributeValue;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSChange;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSUser;
import org.syncope.identityconnectors.bundles.staticwebservice.utilities.Operand;

@ConnectorClass(displayNameKey = "STATICWEBSERVICE_CONNECTOR",
configurationClass = WebServiceConfiguration.class)
public class WebServiceConnector implements
        PoolableConnector,
        AuthenticateOp,
        CreateOp,
        DeleteOp,
        SchemaOp,
        SearchOp<Operand>,
        SyncOp,
        TestOp,
        UpdateOp,
        ResolveUsernameOp {

    /**
     * Setup logging for the {@link WebServiceConnector}.
     */
    private static final Logger log =
            LoggerFactory.getLogger(WebServiceConnector.class);

    /**
     * Place holder for the Connection created in the init method.
     */
    private WebServiceConnection connection;

    /**
     * Place holder for the {@link Configuration} passed into the init() method
     * {@link WebServiceConnector#init}.
     */
    private WebServiceConfiguration config;

    /**
     * Schema.
     */
    private Schema schema = null;

    /**
     * Web Service Attributes.
     */
    private HashMap<String, WSAttribute> wsAttributes = null;

    /**
     * Gets the Configuration context for this connector.
     */
    @Override
    public Configuration getConfiguration() {
        return this.config;
    }

    /**
     * Callback method to receive the {@link Configuration}.
     * 
     * @see Connector#init
     */
    @Override
    public void init(Configuration cfg) {

        if (log.isDebugEnabled()) {
            log.debug("Connector initialization");
        }

        this.config = (WebServiceConfiguration) cfg;
        this.connection = new WebServiceConnection(this.config);
    }

    /**
     * Disposes of the {@link WebServiceConnector}'s resources.
     * 
     * @see Connector#dispose()
     */
    @Override
    public void dispose() {

        if (log.isDebugEnabled()) {
            log.debug("Dispose connector resources");
        }

        config = null;

        if (connection != null) {
            connection.dispose();
        }

        connection = null;
    }

    /**
     * Checks if resource is alive.
     *
     * @see Connector#test()
     */
    @Override
    public void checkAlive() {

        if (log.isDebugEnabled()) {
            log.debug("Connection test");
        }

        connection.test();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Uid authenticate(
            final ObjectClass objectClass,
            final String username,
            final GuardedString password,
            final OperationOptions options) {

        if (log.isDebugEnabled()) {
            log.debug("User uthentication");
        }

        // check objectclass
        if (objectClass == null || (!objectClass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException("Invalid objectclass");
        }

        // check creadentials
        if (username == null || password == null) {
            throw new IllegalArgumentException("Invalid credentuals");
        }

        // get web service client
        Provisioning provisioning = connection.getProvisioning();
        if (provisioning == null) {
            throw new IllegalStateException("Web Service client not found");
        }

        String accountid = null;

        try {

            accountid =
                    provisioning.authenticate(username, password.toString());

        } catch (ProvisioningException e) {
            if (log.isErrorEnabled()) {
                log.error("Authentication failed", e);
            }

            throw new InvalidCredentialException("Authentication failed");
        }

        return new Uid(accountid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Uid create(
            final ObjectClass objClass,
            final Set<Attribute> attrs,
            final OperationOptions options) {

        if (log.isDebugEnabled()) {
            log.debug("Account creation");
        }

        // check objectclass
        if (objClass == null || (!objClass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException("Invalid objectclass");
        }

        // check attributes
        if (attrs == null || attrs.isEmpty()) {
            throw new IllegalArgumentException("No attribute specified");
        }

        // get web service client
        Provisioning provisioning = connection.getProvisioning();
        if (provisioning == null) {
            throw new IllegalStateException("Web Service client not found");
        }

        // get account name
        Name name = AttributeUtil.getNameFromAttributes(attrs);
        if (name == null) {
            throw new IllegalArgumentException("No name specified");
        }
        final String accountName = name.getNameValue();

        if (log.isDebugEnabled()) {
            log.debug("Account to be created: " + accountName);
        }

        // check schema
        if (schema == null || wsAttributes == null) {

            if (log.isDebugEnabled()) {
                log.debug("Reload schema");
            }

            schema();
        }

        // to be used in order to check for mandatory attributes
        Set<String> mandatoryAttributes = new HashSet<String>();

        for (WSAttribute wsAttr : wsAttributes.values()) {
            if (!wsAttr.isNullable()) {
                mandatoryAttributes.add(getAttributeName(wsAttr));
            }
        }

        // to be user in order to pass information to the web service
        List<WSAttributeValue> attributes =
                new ArrayList<WSAttributeValue>();

        WSAttributeValue wsAttributeValue = null;

        String attribute = null;

        // retrieve attributes
        for (Attribute attr : attrs) {
            attribute = attr.getName();

            if (log.isDebugEnabled()) {
                log.debug("Attribute name: " + attribute);
            }

            wsAttributeValue =
                    new WSAttributeValue(wsAttributes.get(attribute));

            Object value = AttributeUtil.getSingleValue(attr);

            if (value == null && !wsAttributeValue.isNullable()) {
                // TODO: provisioningexception
                throw new IllegalArgumentException(
                        "Missing required parameter");
            }

            if (value instanceof GuardedString || value instanceof GuardedByteArray) {

                wsAttributeValue.setValue(value.toString());
            } else {
                wsAttributeValue.setValue(value);
            }

            attributes.add(wsAttributeValue);

            if (!wsAttributeValue.isNullable()) {
                mandatoryAttributes.remove(attribute);
            }
        }

        // check for mandatory attributes
        if (!mandatoryAttributes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing required parameters: " + mandatoryAttributes);
        }

        if (log.isDebugEnabled()) {
            log.debug(
                    "\nUser " + accountName + "\n\tattributes: " + attributes.size());
        }

        try {

            // user creation
            provisioning.create(attributes);

        } catch (ProvisioningException e) {
            if (log.isErrorEnabled()) {
                log.error("Creation failed", e);
            }
        }

        // return Uid
        return new Uid(accountName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(
            final ObjectClass objClass,
            final Uid uid,
            final OperationOptions options) {

        if (log.isDebugEnabled()) {
            log.debug("Account deletion");
        }

        // check objectclass
        if (objClass == null || (!objClass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException("Invalid objectclass");
        }

        final String accountName = uid.getUidValue();

        // get web service client
        Provisioning provisioning = connection.getProvisioning();
        if (provisioning == null) {
            throw new IllegalStateException("Web Service client not found");
        }

        try {
            provisioning.delete(accountName);
        } catch (ProvisioningException e) {
            if (log.isErrorEnabled()) {
                log.error("Deletion failed", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema schema() {

        if (log.isDebugEnabled()) {
            log.debug("Schema retrieving");
        }

        Provisioning provisioning = connection.getProvisioning();

        if (provisioning == null) {
            throw new IllegalStateException("Web Service client not found");
        }

        List<WSAttribute> wsAttrs = provisioning.schema();

        Set<AttributeInfo> attributes = new HashSet<AttributeInfo>();

        if (wsAttributes != null) {
            wsAttributes.clear();
        }

        wsAttributes = new HashMap<String, WSAttribute>();

        for (WSAttribute attribute : wsAttrs) {

            wsAttributes.put(getAttributeName(attribute), attribute);

            if (log.isDebugEnabled()) {
                log.debug(
                        "\nAttribute: " + "\n\tName: " + attribute.getName() + "\n\tType: " + attribute.getType() + "\n\tIsKey: " + attribute.isKey() + "\n\tIsPassword: " + attribute.isPassword() + "\n\tIsNullable: " + attribute.isNullable());
            }

            try {

                attributes.add(buildAttribute(attribute));

            } catch (IllegalArgumentException ila) {

                if (log.isErrorEnabled()) {
                    log.error("Invalid attribute " + attribute.getName(), ila);
                }
            }

        }

        final SchemaBuilder schemaBld = new SchemaBuilder(getClass());

        final ObjectClassInfoBuilder objectclassInfoBuilder =
                new ObjectClassInfoBuilder();

        objectclassInfoBuilder.setType(ObjectClass.ACCOUNT_NAME);
        objectclassInfoBuilder.addAllAttributeInfo(attributes);

        final ObjectClassInfo objectclassInfo = objectclassInfoBuilder.build();
        schemaBld.defineObjectClass(objectclassInfo);

        /*
         * Note: AuthenticateOp, and all the 'SPIOperation'-s are by default
         * added by Reflection API to the Schema.
         *
         * See for details: FrameworkUtil.getDefaultSupportedOperations()
         * ReflectionUtil.getAllInterfaces(connector); is the line that *does*
         * acquire the implemented interfaces by the connector class.
         */
        if (!provisioning.isAuthenticationSupported()) {

            if (log.isDebugEnabled()) {
                log.info("Authentication is not supported.");
            }

            schemaBld.removeSupportedObjectClass(
                    AuthenticateOp.class, objectclassInfo);
        }

        if (!provisioning.isSyncSupported()) {

            if (log.isDebugEnabled()) {
                log.info("Synchronization is not supported.");
            }

            schemaBld.removeSupportedObjectClass(
                    SyncOp.class, objectclassInfo);
        }

        schema = schemaBld.build();
        return schema;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FilterTranslator<Operand> createFilterTranslator(
            ObjectClass oclass,
            OperationOptions options) {

        if (oclass == null || (!oclass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException("Invalid objectclass");
        }

        return new WebServiceFilterTranslator();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeQuery(
            ObjectClass objClass,
            Operand query,
            ResultsHandler handler,
            OperationOptions options) {

        if (log.isDebugEnabled()) {
            log.debug("Execute query");
        }

        // check objectclass
        if (objClass == null || (!objClass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException("Invalid objectclass");
        }

        // check handler
        if (handler == null) {
            throw new IllegalArgumentException("Invalid handler");
        }

        // get web service client
        Provisioning provisioning = connection.getProvisioning();
        if (provisioning == null) {
            throw new IllegalStateException("Web Service client not found");
        }

        try {

            List<WSUser> resultSet = provisioning.query(query);

            Iterator i = resultSet.iterator();

            WSUser user = null;
            boolean handle = true;

            while (i.hasNext() && handle) {
                user = (WSUser) i.next();

                if (log.isDebugEnabled()) {
                    log.debug("Found user: " + user.getAccountid());
                }

                handle = handler.handle(
                        buildConnectorObject(user.getAttributes()).build());

                if (log.isDebugEnabled()) {
                    log.debug("Handle:" + handle);
                }
            }

        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void test() {
        connection.test();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Uid update(ObjectClass objclass,
            Uid uid,
            Set<Attribute> replaceAttributes,
            OperationOptions options) {
        // check objectclass
        if (objclass == null || (!objclass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException("Invalid objectclass");
        }

        // check accountid
        if (uid == null) {
            throw new IllegalArgumentException("No uid specified");
        }

        // check attributes
        if (replaceAttributes == null || replaceAttributes.size() == 0) {
            throw new IllegalArgumentException("No attribute specified");
        }

        // get web service client
        Provisioning provisioning = connection.getProvisioning();
        if (provisioning == null) {
            throw new IllegalStateException("Web Service client not found");
        }

        // check schema
        if (schema == null || wsAttributes == null) {

            if (log.isDebugEnabled()) {
                log.debug("Reload schema");
            }

            schema();
        }

        // to be user in order to pass information to the web service
        List<WSAttributeValue> attributes =
                new ArrayList<WSAttributeValue>();

        WSAttributeValue wsAttributeValue = null;

        String attribute = null;

        // retrieve attributes
        for (Attribute attr : replaceAttributes) {
            attribute = attr.getName();

            if (log.isDebugEnabled()) {
                log.debug("Attribute name: " + attribute);
            }

            wsAttributeValue =
                    new WSAttributeValue(wsAttributes.get(attribute));

            Object value = AttributeUtil.getSingleValue(attr);

            if (value == null && !wsAttributeValue.isNullable()) {
                // TODO: provisioningexception
                throw new IllegalArgumentException(
                        "Missing required parameter");
            }

            if (value instanceof GuardedString || value instanceof GuardedByteArray) {

                wsAttributeValue.setValue(value.toString());
            } else {
                wsAttributeValue.setValue(value);
            }

            attributes.add(wsAttributeValue);
        }

        Uid uuid = null;

        try {

            // user creation
            uuid = new Uid(provisioning.update(uid.getUidValue(), attributes));

        } catch (ProvisioningException e) {
            if (log.isErrorEnabled()) {
                log.error("Creation failed", e);
            }
        }

        return uuid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sync(
            ObjectClass objClass,
            SyncToken token,
            SyncResultsHandler handler,
            final OperationOptions options) {

        // check objectclass
        if (objClass == null || (!objClass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException("Invalid objectclass");
        }

        // check objectclass
        if (handler == null) {
            throw new IllegalArgumentException("Invalid handler");
        }

        // get web service client
        Provisioning provisioning = connection.getProvisioning();
        if (provisioning == null) {
            throw new IllegalStateException("Web Service client not found");
        }

        try {

            List<WSChange> changes = provisioning.sync();

            SyncDeltaBuilder sdb = null;

            Iterator i = changes.iterator();
            boolean handle = true;

            while (i.hasNext() && handle) {

                sdb = buildSyncDelta((WSChange) i.next());
                handle = handler.handle(sdb.build());

            }

        } catch (ProvisioningException e) {
            if (log.isErrorEnabled()) {
                log.error("Synchronization failed");
            }

            throw new IllegalStateException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SyncToken getLatestSyncToken(ObjectClass objectClass) {

        // check objectclass
        if (objectClass == null || (!objectClass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException("Invalid objectclass");
        }

        // get web service client
        Provisioning provisioning = connection.getProvisioning();
        if (provisioning == null) {
            throw new IllegalStateException("Web Service client not found");
        }

        SyncToken token = null;

        try {

            token = new SyncToken(provisioning.getLatestChangeNumber());

        } catch (ProvisioningException e) {
            if (log.isErrorEnabled()) {
                log.error("Resolve username failed", e);
            }
        }

        return token;
    }

    @Override
    public Uid resolveUsername(ObjectClass objectClass, String username,
            OperationOptions options) {

        // check objectclass
        if (objectClass == null || (!objectClass.equals(ObjectClass.ACCOUNT))) {
            throw new IllegalArgumentException("Invalid objectclass");
        }

        // check accountid
        if (username == null) {
            throw new IllegalArgumentException("No username specified");
        }

        // get web service client
        Provisioning provisioning = connection.getProvisioning();
        if (provisioning == null) {
            throw new IllegalStateException("Web Service client not found");
        }

        Uid uuid = null;

        try {

            String uid = provisioning.resolve(username);
            uuid = new Uid(uid);

        } catch (ProvisioningException e) {
            if (log.isErrorEnabled()) {
                log.error("Resolve username failed", e);
            }
        }

        return uuid;

    }

    private AttributeInfo buildAttribute(WSAttribute attribute) {
        final AttributeInfoBuilder builder = new AttributeInfoBuilder();

        try {

            if (attribute.isPassword()) {
                return OperationalAttributeInfos.PASSWORD;
            }

            if (attribute.isKey()) {
                builder.setName(Name.NAME);
                builder.setReadable(true);

                return builder.build();
            }

            // Check the type
            Class.forName(
                    AttributeType.valueOf(attribute.getType()).getClassName());

            builder.setName(attribute.getName());
            builder.setRequired(attribute.isNullable());

            return builder.build();

        } catch (ClassNotFoundException e) {

            if (log.isErrorEnabled()) {
                log.error("Invalid data type", e);
            }

            throw new IllegalArgumentException(e);

        } catch (Throwable t) {

            if (log.isErrorEnabled()) {
                log.error("Unexpected exception", t);
            }

            throw new IllegalArgumentException(t);
        }
    }

    private ConnectorObjectBuilder buildConnectorObject(
            Set<WSAttributeValue> attributes) {

        ConnectorObjectBuilder bld = new ConnectorObjectBuilder();

        String uid = null;

        for (WSAttributeValue attribute : attributes) {

            if (attribute.isKey()) {
                uid = attribute.getStringValue();
                bld.setName(uid);
            }

            if (!attribute.isKey() && !attribute.isPassword()) {

                if (attribute.getValue() == null) {
                    bld.addAttribute(AttributeBuilder.build(
                            attribute.getName()));
                } else {
                    bld.addAttribute(AttributeBuilder.build(
                            attribute.getName(), attribute.getValue()));
                }

            }
        }

        // To be sure that uid and name are present
        if (uid == null) {
            throw new IllegalStateException("Invalid uid");
        }

        // Add Uid attribute to object
        bld.setUid(new Uid(uid));

        // Add objectclass
        bld.setObjectClass(ObjectClass.ACCOUNT);


        return bld;
    }

    private String getAttributeName(WSAttribute attribute) {
        String attributeName = null;

        if (attribute.isKey()) {
            attributeName = Name.NAME;
        }

        if (attribute.isPassword()) {
            attributeName = OperationalAttributeInfos.PASSWORD.getName();
        }

        if (!attribute.isKey() && !attribute.isPassword()) {
            attributeName = attribute.getName();
        }

        return attributeName;
    }

    private SyncDeltaBuilder buildSyncDelta(WSChange change) {
        SyncDeltaBuilder bld = new SyncDeltaBuilder();

        ConnectorObject object =
                buildConnectorObject(change.getAttributes()).build();

        bld.setToken(new SyncToken(change.getId()));
        bld.setObject(object);

        if ("CREATE_OR_UPDATE".equalsIgnoreCase(change.getType())) {
            bld.setDeltaType(SyncDeltaType.CREATE_OR_UPDATE);
        }

        if ("DELETE".equalsIgnoreCase(change.getType())) {
            bld.setDeltaType(SyncDeltaType.DELETE);
        }

        bld.setUid(object.getUid());

        return bld;
    }
}
