package com.portal.service.impl;

import com.portal.daoAuthoriza.CheckUserMapper;
import com.portal.domain.RegisterUserInfo;
import com.portal.service.CheckUserService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author cozi
 * @date 2019-05-29
 * 用于判断用户信息中主账号，邮箱，手机号不能发生重复(同一个公司)
 * 0-没有发生重复
 * 1-主账号重复
 * 2-邮箱重复
 * 3-手机号重复
 */
@Service
public class CheckUserServiceImpl implements CheckUserService {

    @Autowired
    private CheckUserMapper checkUserMapper;

    @Override
    public Integer checkUserInfo(String companyId,String accountNumber,String mail,String telephone) {
        if(StringUtils.isNotEmpty(accountNumber)){
            RegisterUserInfo registerUserInfo = checkUserMapper.userByAccountNumber(accountNumber,companyId);
            if(registerUserInfo!=null){
                return 1;
            }
        }
        if(StringUtils.isNotEmpty(mail)){
            RegisterUserInfo registerUserInfo = checkUserMapper.userByMail(mail,companyId);
            if(registerUserInfo!=null){
                return 2;
            }
        }
        if(StringUtils.isNotEmpty(telephone)){
            RegisterUserInfo registerUserInfo = checkUserMapper.userByTelephone(telephone,companyId);
            if(registerUserInfo!=null){
                return 3;
            }
        }
        return 0;
    }

    @Override
    public Integer checkUserInfoUpdate(String companyId,String uuid, String accountNumber, String mail, String telephone) {
        if(StringUtils.isNotEmpty(accountNumber)){
            RegisterUserInfo registerUserInfo = checkUserMapper.updateUserByAccountNumber(accountNumber,companyId,uuid);
            if(registerUserInfo!=null){
                return 1;
            }
        }
        if(StringUtils.isNotEmpty(mail)){
            RegisterUserInfo registerUserInfo = checkUserMapper.updateUserByMail(mail,companyId,uuid);
            if(registerUserInfo!=null){
                return 2;
            }
        }
        if(StringUtils.isNotEmpty(telephone)){
            RegisterUserInfo registerUserInfo = checkUserMapper.updateUserByTelephone(telephone,companyId,uuid);
            if(registerUserInfo!=null){
                return 3;
            }
        }
        return 0;
    }
}
