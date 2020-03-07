package cipher.console.oidc.service.impl;

import cipher.console.oidc.common.ConstantsCMP;
import cipher.console.oidc.common.DataGridModel;
import cipher.console.oidc.domain.web.*;
import cipher.console.oidc.enums.AdminBehaviorEnum;
import cipher.console.oidc.mapper.AdminBehaviorInfoMapper;
import cipher.console.oidc.mapper.AppMapper;
import cipher.console.oidc.mapper.ApplicationAuditInfoMapper;
import cipher.console.oidc.mapper.GroupMapper;
import cipher.console.oidc.model.*;
import cipher.console.oidc.publistener.AppBehaviorPublistener;
import cipher.console.oidc.service.GroupService;
import cipher.console.oidc.util.IpUtil;
import edu.hziee.common.lang.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class GroupServiceImpl implements GroupService {
    @Autowired
    private  AppServiceImpl appService;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private AppMapper appMapper;


    @Autowired
    private AdminBehaviorInfoMapper adminBehaviorInfoMapper;

    @Autowired
    private ApplicationAuditInfoMapper applicationAuditInfoMapper;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private AppBehaviorPublistener appBehaviorPublistener;

    @Override
    public List<TreeNodesDomain> getNodeTree(List<IdentityGroupMapDomain> list,String companyId) {
        List<GroupInfoDomain> group= groupMapper.getAllGroup(companyId);
        List<TreeNodesDomain> trees = new ArrayList<>();

//        System.out.println(groupApplicationModelList.toString());
         String parentIds = "";
         appService.constructorGroup(trees, 0, group, parentIds,"");
        List<GroupApplicationModel>  list1=new ArrayList<>();
        for (IdentityGroupMapDomain identity:list
             ) {
            GroupApplicationModel groupApplicationModel=new GroupApplicationModel();
            groupApplicationModel.setGroupId(identity.getGroupId());
            list1.add(groupApplicationModel);
        }

        appService.constructorSelectGroup(trees,list1);

        return trees;
    }

    @Override
    public List<GroupInfoDomain> getPath(List<TreeNodesDomain> groupTrees, List<GroupInfoDomain> groupList) {
        for (GroupInfoDomain groupInfo : groupList) {
            getTreePath(groupTrees, groupInfo,"");
             if (StringUtils.isNotEmpty(groupInfo.getPath())){
                 String[] parentPaths = groupInfo.getPath().substring(0, (groupInfo.getPath().length()-1)).split("/");
                 String path2="";
                 for ( String i:parentPaths
                 ) {
                   getParentPath(groupTrees,groupInfo,i, "");

                 }
                 groupInfo.setParentPathName(groupInfo.getParentPathName()+groupInfo.getGroupName());
             }else {
                 groupInfo.setParentPathName(groupInfo.getGroupName());
             }

        }
        return groupList;
    }

    public String getTreePath(List<TreeNodesDomain> groupTrees, GroupInfoDomain groupInfo,String path){
        for (TreeNodesDomain treeNodesDomain : groupTrees) {
            if (null != treeNodesDomain && null != treeNodesDomain.getNodes() && treeNodesDomain.getNodes().size() > 0) {
                // System.out.println(stack.size());

                getTreePath(treeNodesDomain.getNodes(), groupInfo,path);
            }


            if (!StringUtil.isEmpty(treeNodesDomain.getGroupId()) && treeNodesDomain.getGroupId().equals(String.valueOf(groupInfo.getGroupId()))) {
                 groupInfo.setPath(treeNodesDomain.getPath());
            }


        }
        return path;
    }




    /**
     * 遍历树获取某个节点的path
     * @param groupTrees
     * @param
     * @return
     */
    public String getParentPath(List<TreeNodesDomain> groupTrees,GroupInfoDomain groupList,String paths,String path){
        for (TreeNodesDomain treeNodesDomain : groupTrees) {
            if (null != treeNodesDomain && null != treeNodesDomain.getNodes() && treeNodesDomain.getNodes().size() > 0) {
                // System.out.println(stack.size());
                 getParentPath(treeNodesDomain.getNodes(),groupList,paths,path);
            }
            if (!StringUtil.isEmpty(treeNodesDomain.getGroupId()) && treeNodesDomain.getGroupId().equals(paths)) {
                       String groupName=StringUtils.isEmpty(groupList.getParentPathName())? "":groupList.getParentPathName();
                       groupList.setParentPathName(groupName+treeNodesDomain.getGroupName()+"/");
            }
        }
        return path;
    }

    @Override
    public List<GroupInfoDomain> getAllGroup(String companyId) {
        return groupMapper.getAllGroup(companyId);
    }

    @Override
    public GroupInfoDomain getGroupById(int groupId) {
        return groupMapper.getGroupById(groupId);
    }

    @Override
    public List<GroupInfoDomain> queryGroupSelect(String companyId) {
        List<GroupInfoDomain> groupList=groupMapper.queryGroupSelect(companyId);
        GroupInfoDomain groupInfoDomain=new GroupInfoDomain();
        groupInfoDomain.setGroupName("无部门");
        groupInfoDomain.setGroupId(0);
        groupInfoDomain.setCompanyId(companyId);
        groupList.add(groupInfoDomain);
        return groupList;

    }

    @Override
    public GroupInfoDomain queryGroupByAccountName(String uuid) {
        return groupMapper.queryGroupByAccountName(uuid);
    }

    @Override
    public Map<String, Object> queryAllGroupBaseInfo(GroupBaseModel groupBaseModel, DataGridModel dataGridModel) {
        groupBaseModel = (groupBaseModel == null ? new GroupBaseModel() : groupBaseModel);
        groupBaseModel.setPageData(dataGridModel);
        Map<String, Object> map = new HashMap<>();
        map.put("rows", groupMapper.queryAllGroupBaseInfo(groupBaseModel));
        map.put("total", groupMapper.countGroup());
        return map;
    }

    @Override
    public Map<String, Object> groupAuthorizedInfo(GroupAuthorizationDetailModel groupAuthorizationDetailModel, DataGridModel dataGridModel) {
        groupAuthorizationDetailModel = (groupAuthorizationDetailModel == null ? new GroupAuthorizationDetailModel() : groupAuthorizationDetailModel);
        groupAuthorizationDetailModel.setPageData(dataGridModel);
        Map<String, Object> map = new HashMap<>();
        map.put("total", groupMapper.countGroupTable(groupAuthorizationDetailModel));
        map.put("rows", groupMapper.groupAuthorizedInfo(groupAuthorizationDetailModel));
        System.out.println(map);
        return map;
    }

    @Override
    public List<GroupAuthorizedModel> queryGroupAuth(int groupId) {
        return groupMapper.queryGroupAuth(groupId);
    }

    @Override
    public int deleteGroupAuth(GroupAuthorizedModel groupAuthorizedModel) {
        try {
            String accountNumber=ConstantsCMP.getCipherUuidInfo(request);
            String companyId=ConstantsCMP.getSessionCompanyId(request);
            GroupInfoDomain groupInfoDomain = groupMapper.getGroupById(groupAuthorizedModel.getGroupId());
            ApplicationInfoDomain domain = new ApplicationInfoDomain();
            domain.setId(groupAuthorizedModel.getApplicationId());
            ApplicationInfoDomain applicationInfoDomain = appMapper.queryApplicationById(domain);
            ApplicationAuditInfo applicationAuditInfo = new ApplicationAuditInfo(groupAuthorizedModel.getApplicationId(), accountNumber, IpUtil.getIp(), ConstantsCMP.ApplicationConstant.AUTHORIZE, "取消授权", "授权管理", new Date(), "取消授权应用" + applicationInfoDomain.getApplicationName() + "授权组=" + groupInfoDomain.getGroupName(),companyId);
            applicationAuditInfoMapper.insertSelective(applicationAuditInfo);

            //1.5.6
            try {
                UserInfoDomain userInfoDomain = (UserInfoDomain) request.getSession().getAttribute(ConstantsCMP.CIPHER_CONSOLE_USER_SESSION_INFO);
                String userName = userInfoDomain.getUserName()+"("+userInfoDomain.getAccountNumber()+")";
                AppAuditInfo appAuditInfo = new AppAuditInfo(groupAuthorizedModel.getApplicationId(),userName,3,"取消授权应用:" + applicationInfoDomain.getApplicationName() + ",授权组:" + groupInfoDomain.getGroupName(),companyId);
                appBehaviorPublistener.publish(appAuditInfo);
            }catch (Exception e){
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return groupMapper.deleteGroupAuth(groupAuthorizedModel);
    }

    @Override
    public int insetGroupAuth(GroupAuthorizedModel groupAuthorizedModel) {
        //添加日志
        try {
            String accountNumber=ConstantsCMP.getSessionUser(request);
            String companyId=ConstantsCMP.getSessionCompanyId(request);
            GroupInfoDomain groupInfoDomain = groupMapper.getGroupById(groupAuthorizedModel.getGroupId());
            ApplicationInfoDomain domain = new ApplicationInfoDomain();
            domain.setId(groupAuthorizedModel.getApplicationId());
            ApplicationInfoDomain applicationInfoDomain = appMapper.queryApplicationById(domain);
            ApplicationAuditInfo applicationAuditInfo = new ApplicationAuditInfo(groupAuthorizedModel.getApplicationId(), accountNumber, IpUtil.getIp(), ConstantsCMP.ApplicationConstant.AUTHORIZE, "授权", "授权管理", new Date(), "授权应用" + applicationInfoDomain.getApplicationName() + "授权组=" + groupInfoDomain.getGroupName(),companyId);
            applicationAuditInfoMapper.insertSelective(applicationAuditInfo);
            //1.5.6
            try {
                UserInfoDomain userInfoDomain = (UserInfoDomain) request.getSession().getAttribute(ConstantsCMP.CIPHER_CONSOLE_USER_SESSION_INFO);
                String userName = userInfoDomain.getUserName()+"("+userInfoDomain.getAccountNumber()+")";
                AppAuditInfo appAuditInfo = new AppAuditInfo(groupAuthorizedModel.getApplicationId(),userName,3,"授权应用:" + applicationInfoDomain.getApplicationName() + ",授权组:" + groupInfoDomain.getGroupName(),companyId);
                appBehaviorPublistener.publish(appAuditInfo);
            }catch (Exception e){
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return groupMapper.insetGroupAuth(groupAuthorizedModel);
    }

    @Override
    public Map<String, Object> queryGroupMember(GroupMemberAddModel groupMemberAddModel, DataGridModel dataGridModel) {
        groupMemberAddModel = (groupMemberAddModel == null ? new GroupMemberAddModel() : groupMemberAddModel);
        groupMemberAddModel.setPageData(dataGridModel);
        Map<String, Object> map = new HashMap<>();
        map.put("rows", groupMapper.queryGroupMember(groupMemberAddModel));
        map.put("total", groupMapper.countGroupMember(groupMemberAddModel));
        return map;
    }

    @Override
    public int insertNewGroup(GroupInfoModel groupInfoModel) {

        return groupMapper.insertNewGroup(groupInfoModel);
    }

    @Override
    public List<GroupInfoDomain> getGoupInfoByName(GroupInfoDomain groupInfoDomain) {
        return groupMapper.selectGoupInfoByName(groupInfoDomain);
    }

    @Override
    public void updateGroup(GroupInfoModel groupInfoModel) {
        if(groupInfoModel!=null){
            groupMapper.updateGroup(groupInfoModel);
        }
    }

    @Override
    public GroupInfoModel geteditGroupEcho(Integer groupId) {
        return groupMapper.selectEditGroupEcho(groupId);
    }

    @Override
    public int updateGroupInfo(GroupInfoModel groupInfoModel) {
        return groupMapper.updateGroupInfo(groupInfoModel);
    }

    @Override
    public int insetIntoGroup(UserInfoModel userInfoModel) {
        return groupMapper.insetIntoGroup(userInfoModel);
    }

    @Override
    public int deleteGroupMap(UserInfoModel userInfoModel) {
        return groupMapper.deleteGroupMap(userInfoModel);
    }

    @Override
    public GroupInfoDomain queryGroupByGroupName(Map<String, Object> map) {
        return groupMapper.queryGroupByGroupName(map);
    }

    @Override
    public Integer checkUserInfoByGroupId(Integer groupId) {
        return groupMapper.checkUserInfoByGroupId(groupId);
    }

    @Override
    public Integer checkLowerLevelByGroupId(Integer groupId) {
        return groupMapper.checkLowerLevelByGroupId(groupId);
    }

    @Override
    public void deleteGroup(Integer groupId) throws Exception {
        groupMapper.deleteGroup(groupId);
        groupMapper.deleteGroupUser(groupId);
    }

    @Override
    public Map<String, Object> queryGroupMemberByGroupId(GroupMemberAddModel groupMemberAddModel, DataGridModel dataGridModel) {
        groupMemberAddModel = (groupMemberAddModel == null ? new GroupMemberAddModel():groupMemberAddModel);
        groupMemberAddModel.setPageData(dataGridModel);
        Map<String,Object> map = new HashMap<>();
        map.put("rows",groupMapper.queryGroupMemberByGroupId(groupMemberAddModel));
        map.put("total",groupMapper.countGroupMemberByGroupId(groupMemberAddModel));
        return map;
    }

    @Override
    public List<GroupInfoDomain> selectNoneGroupList(List<Integer> list) {
        return groupMapper.selectNoneGroupList(list);
    }

    @Override
    public void updateGroupName(GroupInfoModel groupInfoModel) {
        groupMapper.updateGroupName(groupInfoModel);
        return;
    }

    @Override
    public int countGroupMemberByGroupId(GroupMemberAddModel groupMemberAddModel) {
        return groupMapper.countGroupMemberByGroupId(groupMemberAddModel);
    }

    @Override
    public Integer countChildGroup(int parentGroupId) {
        return groupMapper.countChildGroup(parentGroupId);
    }

    @Override
    public Integer countGroupMembers(int groupId) {
        return groupMapper.countGroupMembers(groupId);
    }

    @Override
    public int getGroupStruct(List<TreeNodesDomain> result, List<TreeNodesDomain> list, int groupId) {
        for (TreeNodesDomain treeNodesDomain : list){
            if (Integer.valueOf(treeNodesDomain.getGroupId()) == groupId ){
                result.add(treeNodesDomain);
                return 1;
            }else {
                int d = getGroupStruct(result,treeNodesDomain.getNodes(),groupId);
                if (d == 1){
                    result.add(treeNodesDomain);
                    return 1;
                }
            }
        }
        return 0;
    }

    @Override
    public List<GroupInfoDomain> queryGoupInfoByName(GroupInfoDomain form) {
        return groupMapper.queryGoupInfoByName(form);
    }

    @Override
    public List<GroupInfoDomain> queryGoupName(String accountNumber, Integer groupId) {
        return groupMapper.queryGoupName(accountNumber,groupId);
    }

    @Override
    public GroupInfoDomain getGroupByParentId(int parentId) {
        return groupMapper.getGroupByParentId(parentId);
    }

    @Override
    public List<GroupInfoDomain> selectGroupInfoList(GroupAuthorizationMapDomain form) {
        return groupMapper.selectGroupInfoList(form);
    }


    @Override
    public Map<String, Object> getGroupTree(String companyId) {
        Map<String,Object> map=new HashMap<>();
        Map<String,Object> msg=new HashMap<>();
        try {
            List<GroupInfoDomain> groupInfoDomains = groupMapper.getAllGroup(companyId);
            List<TreeNodesDomain> trees = new ArrayList<>();
            String parentIds = "";
            appService.constructorGroup(trees, 0, groupInfoDomains, parentIds,"");
            map.put("code","0");
            msg.put("trees",trees);
            map.put("msg",msg);

        }catch (Exception e){
            map.put("code",1);
            map.put("msg","服务器错误");
            e.printStackTrace();
        }

        return map;
    }

    @Override
    public List<GroupInfoDomain> getGroupNamesById(String uuid) {
        return groupMapper.selectGroupNameList(uuid);
    }

    @Override
    public List<GroupAuthorizedModel> queryGroupAuthByConditon(String groupName) {
        return groupMapper.queryGroupAuthByConditon(groupName);
    }


    @Override
    public GroupInfoDomain queryGroupByName(String groupName) {
        return groupMapper.queryGoupByName(groupName);
    }

    @Override
    public GroupInfoDomain queryGroupByNameAndcompanyId(String companyId, String groupName) {
        return groupMapper.queryGroupByNameAndcompanyId(companyId,groupName);
    }

    @Override
    public Integer insertGroup(GroupInfoDomain groupInfoDomain) {
        return groupMapper.insertGroup(groupInfoDomain);
    }

    @Override
    public List<TreeNodesDomain> getGroupTreeList(List<Integer> groupId,String companyId) {
        List<GroupInfoDomain> groupList = groupMapper.getAllGroup(companyId);
        List<TreeNodesDomain> trees = new ArrayList<TreeNodesDomain>();
        String parentIds = "";
        constructTreeV2(trees,0,groupList);
        constructorSelect(trees, groupId);
        return trees;

    }

    @Override
    public void updateNewGroup(GroupInfoModel group) {
        groupMapper.updateNewGroup(group);
    }

    @Override
    public GroupInfoDomain getGroupPath(String companyId,int groupId) {
        List<GroupInfoDomain> list = groupMapper.getAllGroup(companyId);

        GroupInfoDomain groupInfoDomain = getGroupInfo(list,groupId);
        return getGroupInfo(list,groupInfoDomain);
    }

    @Override
    public GroupInfoDomain getGroupPath(String companyId, String userId, int applicationId) {
        GroupInfoDomain groupInfoDomain = groupMapper.selectAppGroup(userId,applicationId);
        if (groupInfoDomain == null){
            return groupInfoDomain;
        }
        return getGroupPath(companyId,groupInfoDomain.getGroupId());
    }

    @Override
    public List<TreeNodesDomain> getGroupListByParentId(String companyId, int parateGroupId) {
        return groupMapper.getGroupListByParentId(companyId,parateGroupId);
    }




    private GroupInfoDomain getGroupInfo(List<GroupInfoDomain> list,int groupId){
        if (list == null || list.size() == 0){
            return null;
        }

        if(groupId==0){
            return null;
        }

        for (GroupInfoDomain groupInfoDomain:list){
            if (groupInfoDomain.getGroupId() == groupId){
                return groupInfoDomain;
            }
        }
        return null;
    }

    private GroupInfoDomain getGroupInfo(List<GroupInfoDomain> list,GroupInfoDomain groupInfoDomain){
        if (groupInfoDomain == null){
            return groupInfoDomain;
        }

        if (groupInfoDomain.getParentGroupId() == 0){
            return groupInfoDomain;
        }

        GroupInfoDomain parent = getGroupInfo(list,groupInfoDomain.getParentGroupId());
        groupInfoDomain.setParentGroup(parent);
        parent.setChildGroup(groupInfoDomain);

        return getGroupInfo(list,parent);
    }


    private List<TreeNodesDomain> constructTreeV2(List<TreeNodesDomain> trees,int parentId,List<GroupInfoDomain> groupList) {
        TreeNodesDomain treeNode = new TreeNodesDomain();
        if(groupList.size()>0){
            for (GroupInfoDomain groupInfoDomain : groupList) {
                //查找根节点
                if (!StringUtils.isEmpty(String.valueOf(groupInfoDomain.getParentGroupId()))&&groupInfoDomain.getParentGroupId()== parentId) {
                    treeNode = new TreeNodesDomain(groupInfoDomain);
                    if (parentId == 0){
                        treeNode.setPath(groupInfoDomain.getGroupName());
                    }
                    treeNode.setGroupId(String.valueOf(groupInfoDomain.getGroupId()));
                    treeNode.setGroupName(groupInfoDomain.getGroupName());
                    treeNode.setParentGroupId(groupInfoDomain.getParentGroupId()+"");
                    treeNode.setNodes(constructTreeV2(treeNode.getNodes(),groupInfoDomain.getGroupId(),groupList));
                    if (treeNode.getNodes() == null || treeNode.getNodes().size() == 0){
                        treeNode.setNodes(null);
                    }
                    trees.add(treeNode);
                }
    }
}
        return trees;
    }





    public  List<TreeNodesDomain> constructorSelect (List < TreeNodesDomain > trees, List <Integer> groupList){
        for (TreeNodesDomain treeNodesDomain : trees) {
            if (null != treeNodesDomain && null != treeNodesDomain.getNodes() && treeNodesDomain.getNodes().size() > 0) {
                constructorSelect(treeNodesDomain.getNodes(), groupList);
            } else {
                if (groupList != null && groupList.size() > 0) {
                    for (Integer groupId : groupList) {
                        if (!StringUtil.isEmpty(treeNodesDomain.getGroupId()) && treeNodesDomain.getGroupId().equals(groupId)) {
                            treeNodesDomain.setState();
                        }
                    }
                } else {

                }

            }
        }
        return trees;
    }
}
