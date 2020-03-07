package com.portal.daoAuthoriza;
import com.portal.domain.GroupUserMapDomain;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: zt
 * @Date: 2018/5/30 17:02
 */
public interface UserGroupMapper {

    /**
     * 插入用户和组的关联关系
     * @param groupUserMapDomain
     */
    public void insertUserGroupMap(GroupUserMapDomain groupUserMapDomain);

}
