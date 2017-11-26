package com.mmall.service;

import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;

import javax.servlet.http.HttpSession;

public interface IUserService {

   /**
    * 用户登录
    * @param username
    * @param password
    * @return
    */
   ServerResponse<User> login(String username, String password);

   /**
    * 用户注册
    * @param user
    * @return
    */
   ServerResponse<String> register(User user);


   ServerResponse<String> checkValid(String str,String type);

   ServerResponse<User> getUserInfo(HttpSession session);

   ServerResponse<String> forgetGetQuestion(String username);

   ServerResponse<String> forgetCheckAnswer(String username,String question,String answer);

   ServerResponse<String> resetPassword(String username,String passwordNew,String forgetToken);

   ServerResponse<String> resetPasswordLogging(String passwordOld,String passwordNew,HttpSession session);

   ServerResponse<String> updateInformation(User user,HttpSession session);

   ServerResponse<User> getCurrentUser(HttpSession session);


}
