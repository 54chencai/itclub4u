package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.common.TokenCache;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.UUID;

@Service("iUserService")
public class UserServiceImpl implements IUserService{

    @Autowired
    private UserMapper userMapper;

    @Override
    public ServerResponse<User> login(String username, String password) {
        int resultCount = userMapper.checkUsername(username);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("用户名不存在");
        }

        //密码登录MD5
        String MD5Password = DigestUtils.md5Hex(password);
        User user = userMapper.selectLogin(username,MD5Password);
        if (user == null) {
            return ServerResponse.createByErrorMessage("用户名或密码错误");
        }

        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("登录成功",user);

    }

    @Override
    public ServerResponse<String> register(User user) {
        //校验用户名是否已存在
        int resultCount = userMapper.checkUsername(user.getUsername());
        if (resultCount > 0) {
            return ServerResponse.createByErrorMessage("用户名已存在");
        }
        //校验email
        resultCount = userMapper.checkEmail(user.getEmail());
        if (resultCount > 0) {
            return ServerResponse.createByErrorMessage("email已存在");
        }

        //设置用户角色
        user.setRole(Const.Role.ROLE_CUSTOMER);
        //设置密码(MD5加密)
        user.setPassword(DigestUtils.md5Hex(user.getPassword()));

        //讲用户信息存入数据库
        resultCount = userMapper.insert(user);
        if (resultCount == 0) {
            return ServerResponse.createBySuccessMessage("注册失败");
        }
        return ServerResponse.createBySuccessMessage("注册成功");
    }

    @Override
    public ServerResponse<String> checkValid(String str,String type) {
        if (StringUtils.isNotBlank(type)) {
            if (Const.USERNAME.equals(type)) {
                //校验用户名是否已存在
                int resultCount = userMapper.checkUsername(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("用户名已存在");
                }
                return ServerResponse.createBySuccess("用户名校验成功");
            }

            if (Const.EMAIL.equals(type)) {
                //校验email
                int resultCount = userMapper.checkEmail(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("email已存在");
                }
                return ServerResponse.createBySuccess("email校验成功");
            }



        } else {
            return ServerResponse.createByErrorMessage("参数不能为空");
        }

        return ServerResponse.createByErrorMessage("参数错误");
    }

    public ServerResponse<User> getUserInfo(HttpSession session) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user != null) {
            user.setPassword(null);
            return ServerResponse.createBySuccess(user);
        }
        return  ServerResponse.createByErrorMessage("用户为登录,无法获取当前用户信息");
    }

    public ServerResponse<String> forgetGetQuestion(String username) {
        ServerResponse validResponse = checkValid(username,Const.USERNAME);
        if (validResponse.isSuccess()) {
            return ServerResponse.createByErrorMessage("用户名不存在");
        }
        String question = userMapper.checkQuestionByUsername(username);
        if (StringUtils.isNotBlank(question)) {
            return ServerResponse.createBySuccessMessage(question);
        }
        return ServerResponse.createByErrorMessage("该用户未设置找回密码问题");
    }


    public ServerResponse<String> forgetCheckAnswer(String username,String question,String answer) {
        String checkanswer = userMapper.checkAnswerByUsernameAndQuestion(username,question);
        if (checkanswer.equals(answer)) {
            String forgetToken = UUID.randomUUID().toString();
            TokenCache.setKey("token_" + username,forgetToken);
            return ServerResponse.createBySuccess(forgetToken);
        }
        return ServerResponse.createByErrorMessage("问题的答案错误");

    }

    public ServerResponse<String> resetPassword(String username,String passwordNew,String forgetToken) {
        String token = TokenCache.getKey("token_"+username);
        if (token == null) {
            return ServerResponse.createByErrorMessage("token已经过期,修改密码失败");
        }
        if (token.equals(forgetToken)) {
            //修改密码
            int result = userMapper.updatePasswordByUsername(username,passwordNew);
            if (result > 0) {
                return ServerResponse.createBySuccessMessage("修改密码成功");
            }
            return ServerResponse.createByErrorMessage("修改密码失败");
        }
        return ServerResponse.createByErrorMessage("token错误,修改密码失败");
    }

    public ServerResponse<String> resetPasswordLogging(String passwordOld,String passwordNew,HttpSession session) {
        //获取当前登录用户
       User user = (User)session.getAttribute(Const.CURRENT_USER);
       String username = user.getUsername();
       //验证旧密码
       int result = userMapper.checkPasswordByOldPassword(username,passwordOld);
       if (result > 0) {
           userMapper.updatePasswordByUsername(username,passwordNew);
           return ServerResponse.createBySuccessMessage("修改密码成功");
       }
       return ServerResponse.createByErrorMessage("旧密码输入错误");
    }

    public ServerResponse<String> updateInformation(User user,HttpSession session) {
        //获取当前登录用户
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if (currentUser == null) {
            return ServerResponse.createByErrorMessage("用户未登录");
        }
        //查询userId
        Integer id = currentUser.getId();
        user.setId(id);

       int result = userMapper.updateByPrimaryKeySelective(user);
       if (result > 0) {
           return ServerResponse.createBySuccessMessage("更新个人信息成功");
       }
       return ServerResponse.createByErrorMessage("更新个人信息失败");
    }

    public ServerResponse<User> getCurrentUser(HttpSession session) {
        //获取当前登录用户
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if (currentUser != null) {
           return ServerResponse.createBySuccess(currentUser);
        }
        return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录,需要强制登录");
    }

}
