package cipher.console.oidc.controller.web;

import cipher.console.oidc.common.CacheKey;
import cipher.console.oidc.common.ConstantsCMP;
import cipher.console.oidc.domain.NullCacheObject;
import cipher.console.oidc.domain.web.GroupInfoDomain;
import cipher.console.oidc.domain.web.PathInfo;
import cipher.console.oidc.domain.web.TreeNodesDomain;

import cipher.console.oidc.redis.RedisClient;
import cipher.console.oidc.service.GroupService;
import cipher.console.oidc.service.NewUserService;
import cipher.console.oidc.service.OrganitionTreeSerive;
import cipher.console.oidc.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 获取组织结构树
 */

@Controller
@RequestMapping(value = "/cipher/organization")
@EnableAutoConfiguration
public class OrganitionTreeController {

     private static final Logger logger = LoggerFactory.getLogger(OrganitionTreeController.class.getSimpleName());

     @Autowired
     private OrganitionTreeSerive organitionTreeSerive;

     @Autowired
     private RedisClient<String,Object> redisClient;

     @Autowired
     private  GroupService groupService;

     @Autowired
     private NewUserService newUserService;

     /*
    * 获取用户组织结构树
    * */
    @RequestMapping(value = "/list")
    @ResponseBody
    public List<TreeNodesDomain> queryData(HttpServletRequest request, @RequestParam(value = "companyUUid") String companyId) {
        long start =System.currentTimeMillis();
        logger.debug("Enter OrganitionTreeController.queryData param:"+ companyId);
        Object obj = redisClient.get(CacheKey.getCacheOrganitionTreeList(companyId));
        long middle=System.currentTimeMillis();
        System.out.println("从缓存中获取耗时:"+(middle-start));
        if (obj == null) {
            obj = organitionTreeSerive.getAllUserList(companyId);
            if (obj == null) {
                obj = new NullCacheObject();
            }
            redisClient.put(CacheKey.getCacheOrganitionTreeList(companyId), obj);
        }
        if (obj == null || obj instanceof NullCacheObject) {
            return null;
        }
        long end=System.currentTimeMillis();
        System.out.println("总耗时:"+(end-start));

        return (List<TreeNodesDomain>) obj;
    }



    /* *
     *  @param groupId
     * 根据部门Id获取用户组织结构树
     * */
    @RequestMapping(value = "/getListByGroupId")
    @ResponseBody
    public List<TreeNodesDomain> queryGroupListByGroupId(Integer groupId,HttpServletRequest request) {
        String companyId= ConstantsCMP.getSessionCompanyId(request);
        List<TreeNodesDomain> list=new ArrayList<>();
        List<TreeNodesDomain> groupList=groupService.getGroupListByParentId(companyId,groupId);
        if(null!=groupList&&groupList.size()>0) {
            for (TreeNodesDomain treeNodesDomain : groupList) {
                treeNodesDomain.setHref("/cipher/newUser/userlist?json");
            }
            list.addAll(groupList);
        }

        List<TreeNodesDomain> userList = newUserService.getUserListByGroupId(companyId,groupId);
        if(null!=userList&&userList.size()>0){
            for(TreeNodesDomain treeNodesDomain:userList){
                treeNodesDomain.setHref("/cipher/newUser/getlist?json");
            }
            list.addAll(userList);
        }
        return list;
    }





    /*
    * 获取临时用户组织结构树
    * */

    @RequestMapping(value = "/casuallist")
    @ResponseBody
    public List<TreeNodesDomain> queryCasual(HttpServletRequest request) {
        String companyId= ConstantsCMP.getSessionCompanyId(request);
        logger.debug("Enter OrganitionTreeController.queryCasual param:"+companyId);
        List<TreeNodesDomain> list = organitionTreeSerive.getCasualUserList(companyId);
        return list;
    }




}
