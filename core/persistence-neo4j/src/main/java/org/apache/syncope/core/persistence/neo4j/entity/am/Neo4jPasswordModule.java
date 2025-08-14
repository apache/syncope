package org.apache.syncope.core.persistence.neo4j.entity.am;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.password.PasswordModuleConf;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.core.persistence.api.entity.am.PasswordModule;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractProvidedKeyNode;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.PostLoad;

@Node(Neo4jPasswordModule.NODE)
public class Neo4jPasswordModule extends AbstractProvidedKeyNode implements PasswordModule {
    private static final long serialVersionUID = 5457779846065079998L;

    public static final String NODE = "PasswordModule";

    protected static final TypeReference<List<Item>> TYPEREF = new TypeReference<List<Item>>() {
    };

    private String description;

    @NotNull
    private Integer authModuleOrder = 0;

    private String items;

    @Transient
    private final List<Item> itemList = new ArrayList<>();

    private String jsonConf;

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public int getOrder() {
        return Optional.ofNullable(authModuleOrder).orElse(0);
    }

    @Override
    public void setOrder(final int order) {
        this.authModuleOrder = order;
    }

    @Override
    public List<Item> getItems() {
        return itemList;
    }

    @Override
    public PasswordModuleConf getConf() {
        PasswordModuleConf conf = null;
        if (!StringUtils.isBlank(jsonConf)) {
            conf = POJOHelper.deserialize(jsonConf, PasswordModuleConf.class);
        }

        return conf;
    }

    @Override
    public void setConf(final PasswordModuleConf conf) {
        jsonConf = POJOHelper.serialize(conf);
    }

    protected void json2list(final boolean clearFirst) {
        if (clearFirst) {
            getItems().clear();
        }
        if (items != null) {
            getItems().addAll(POJOHelper.deserialize(items, TYPEREF));
        }
    }

    @PostLoad
    public void postLoad() {
        json2list(false);
    }

    public void postSave() {
        json2list(true);
    }

    public void list2json() {
        items = POJOHelper.serialize(getItems());
    }
}
