package org.apache.syncope.common.lib.password;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
import org.apache.syncope.common.lib.BaseBean;
import org.apache.syncope.common.lib.to.PasswordModuleTO;

@FunctionalInterface
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "_class")
public interface PasswordModuleConf extends BaseBean {

    interface Mapper {

        Map<String, Object> map(PasswordModuleTO passwordModuleTO, SyncopePasswordModuleConf conf);

        Map<String, Object> map(PasswordModuleTO passwordModuleTO, LDAPPasswordModuleConf conf);

    }

    Map<String, Object> map(PasswordModuleTO passwordModuleTO, Mapper mapper);
}
