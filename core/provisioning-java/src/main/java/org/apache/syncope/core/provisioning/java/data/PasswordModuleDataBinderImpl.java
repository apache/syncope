package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.PasswordModuleTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.PasswordModule;
import org.apache.syncope.core.provisioning.api.data.PasswordModuleDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordModuleDataBinderImpl implements PasswordModuleDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(PasswordModuleDataBinder.class);

    protected final EntityFactory entityFactory;

    public PasswordModuleDataBinderImpl(final EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    protected void populateItems(final PasswordModuleTO passwordModuleTO, final PasswordModule passwordModule) {
        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();
        SyncopeClientException invalidMapping =
                SyncopeClientException.build(ClientExceptionType.InvalidMapping);
        SyncopeClientException requiredValuesMissing =
                SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        passwordModuleTO.getItems().forEach(itemTO -> {
            if (itemTO == null) {
                LOG.error("Null {}", Item.class.getSimpleName());
                invalidMapping.getElements().add("Null " + Item.class.getSimpleName());
            } else if (itemTO.getIntAttrName() == null) {
                requiredValuesMissing.getElements().add("intAttrName");
                scce.addException(requiredValuesMissing);
            } else {
                // no mandatory condition implies mandatory condition false
                if (!JexlUtils.isExpressionValid(itemTO.getMandatoryCondition() == null
                        ? "false" : itemTO.getMandatoryCondition())) {

                    SyncopeClientException invalidMandatoryCondition =
                            SyncopeClientException.build(ClientExceptionType.InvalidValues);
                    invalidMandatoryCondition.getElements().add(itemTO.getMandatoryCondition());
                    scce.addException(invalidMandatoryCondition);
                }

                Item item = new Item();
                item.setIntAttrName(itemTO.getIntAttrName());
                item.setExtAttrName(itemTO.getExtAttrName());
                item.setMandatoryCondition(itemTO.getMandatoryCondition());
                item.setConnObjectKey(itemTO.isConnObjectKey());
                item.setPassword(itemTO.isPassword());
                item.setPropagationJEXLTransformer(itemTO.getPropagationJEXLTransformer());
                item.setPullJEXLTransformer(itemTO.getPullJEXLTransformer());
                passwordModule.getItems().add(item);
            }
        });

        if (!invalidMapping.getElements().isEmpty()) {
            scce.addException(invalidMapping);
        }
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    protected void populateItems(final PasswordModule passwordModule, final PasswordModuleTO passwordModuleTO) {
        passwordModule.getItems().forEach(item -> {
            Item itemTO = new Item();
            itemTO.setIntAttrName(item.getIntAttrName());
            itemTO.setExtAttrName(item.getExtAttrName());
            itemTO.setMandatoryCondition(item.getMandatoryCondition());
            itemTO.setConnObjectKey(item.isConnObjectKey());
            itemTO.setPassword(item.isPassword());
            itemTO.setPropagationJEXLTransformer(item.getPropagationJEXLTransformer());
            itemTO.setPullJEXLTransformer(item.getPullJEXLTransformer());
            itemTO.setPurpose(MappingPurpose.NONE);

            passwordModuleTO.getItems().add(itemTO);
        });
    }

    @Override
    public PasswordModule create(final PasswordModuleTO passwordModuleTO) {
        PasswordModule passwordModule = entityFactory.newEntity(PasswordModule.class);
        passwordModule.setKey(passwordModuleTO.getKey());
        return update(passwordModule, passwordModuleTO);
    }

    @Override
    public PasswordModule update(final PasswordModule passwordModule, final PasswordModuleTO passwordModuleTO) {
        passwordModule.setDescription(passwordModuleTO.getDescription());
        passwordModule.setState(passwordModuleTO.getState());
        passwordModule.setConf(passwordModuleTO.getConf());

        passwordModule.getItems().clear();
        populateItems(passwordModuleTO, passwordModule);

        return passwordModule;
    }

    @Override
    public PasswordModuleTO getPasswordModuleTO(final PasswordModule passwordModule) {
        PasswordModuleTO passwordModuleTO = new PasswordModuleTO();

        passwordModuleTO.setKey(passwordModule.getKey());
        passwordModuleTO.setDescription(passwordModule.getDescription());
        passwordModuleTO.setState(passwordModule.getState());
        passwordModuleTO.setConf(passwordModule.getConf());

        populateItems(passwordModule, passwordModuleTO);

        return passwordModuleTO;
    }
}
