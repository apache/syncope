package org.apache.syncope.core.persistence.api.entity.am;

import java.util.List;
import org.apache.syncope.common.lib.password.PasswordModuleConf;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.core.persistence.api.entity.ProvidedKeyEntity;

public interface PasswordModule extends ProvidedKeyEntity {

    String getDescription();

    void setDescription(String description);

    int getOrder();

    void setOrder(int order);

    PasswordModuleConf getConf();

    void setConf(PasswordModuleConf conf);

    List<Item> getItems();
}
