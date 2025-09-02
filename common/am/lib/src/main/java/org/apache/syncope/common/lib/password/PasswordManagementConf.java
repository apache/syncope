package org.apache.syncope.common.lib.password;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;
import org.apache.syncope.common.lib.BaseBean;
import org.apache.syncope.common.lib.to.PasswordManagementTO;

@FunctionalInterface
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "_class")
public interface PasswordManagementConf extends BaseBean {

    interface Mapper {

        Map<String, Object> map(PasswordManagementTO passwordManagementTO, SyncopePasswordManagementConf conf);

        Map<String, Object> map(PasswordManagementTO passwordManagementTO, LDAPPasswordManagementConf conf);

        Map<String, Object> map(PasswordManagementTO passwordManagementTO, JDBCPasswordManagementConf conf);

    }

    Map<String, Object> map(PasswordManagementTO passwordManagementTO, Mapper mapper);
}
