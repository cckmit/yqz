package org.mintleaf.modules.core.controller;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.mintleaf.modules.core.dao.CoreMenuDao;
import org.mintleaf.modules.core.dao.CoreUserDao;
import org.mintleaf.modules.core.entity.CoreMenu;
import org.mintleaf.modules.core.entity.CoreUser;
import org.mintleaf.utils.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.mintleaf.vo.ResultMsg;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

import static org.mintleaf.vo.ResultMsg.fail;
import static org.mintleaf.vo.ResultMsg.ok;

/**
 * 后台管理主控制器
 * @Author: MengchuZhang
 * @Date: 2018/8/15 8:24
 * @Version 1.0
 */
@Controller
public class IndexController {

    @Autowired
    HttpSession session;

    @Autowired
    CoreUserDao coreUserDao;

    @Autowired
    CoreMenuDao coreMenuDao;

    /**
     * 进入后台首页
     * @return
     */
//  @RequiresRoles("超级用户")
    @RequestMapping(value="index.html")
    public ModelAndView home() {
        ModelAndView view = new ModelAndView("index.html");
        String username = (String) SecurityUtils.getSubject().getPrincipal();
        System.out.print(username);
        CoreUser coreUser=coreUserDao.sample(username);
//        Subject subject = SecurityUtils.getSubject();
//        try {
//            subject.checkPermission("超级用户");
//        } catch (UnauthorizedException exception) {
//            System.out.println("没有足够的权限");
//            return login();
//        }
        view.addObject("user", coreUser);
        return view;
    }

    /**
     * 进入登陆页面
     * @return
     */
    @RequestMapping(value="login.html")
    public ModelAndView login(){
        ModelAndView view =new ModelAndView("login.html");
        return view;
    }

    /**
     * 进入403未授权页面
     * @return
     */
    @RequestMapping(value="403.html")
    public ModelAndView error403() {
        ModelAndView view =new ModelAndView("403.html");
        return view;
    }

    /**
     * 进入404未定义页面
     * @return
     */
    @RequestMapping(value="404.html")
    public ModelAndView error404() {
        ModelAndView view =new ModelAndView("404.html");
        return view;
    }

    /**
     * 进入后台首页面
     * @return
     */
    @RequestMapping(value="home.html")
    public ModelAndView test(){
        ModelAndView view =new ModelAndView("home.html");
        return view;
    }

    /**
     * 登陆操作
     * @param request
     * @return
     */
    @RequestMapping(value = "login.do", method = RequestMethod.POST)
    @ResponseBody
    public ResultMsg loginDo(HttpServletRequest request){
        //验证码校验
//        if (!CaptchaUtil.checkVerifyCode(request)) {
//            return fail("验证码有误！");
//        }
        String name = request.getParameter("name");
        String password = request.getParameter("password");
//        CoreUser coreUser = new CoreUser();
//        coreUser.setUsercode(usercode);
////      coreUser.setPassword(password);
//        CoreUser end=coreUserDao.sample(coreUser.getUsercode());
//        if (end!=null) {
//            session.setAttribute("user", coreUser.getUsercode());
//            return ResultMsg.ok(url);
//        } else {
//            return ResultMsg.fail();
//        }
        //用户名密码校验
        UsernamePasswordToken token = new UsernamePasswordToken(name, MD5Util.MD5(password));
        Subject subject = SecurityUtils.getSubject();
        try {
            subject.login(token);
        } catch (Exception e) {
            e.printStackTrace();
            return fail("用户名或密码错误！");
        }
        return ok();
    }

    /**
     * 登出操作
     * @return
     */
    @RequestMapping(value = "loginOut.do", method ={RequestMethod.GET,RequestMethod.POST})
    public ModelAndView loginOut() {
        ModelAndView view = new ModelAndView();
        view.setViewName("/login.html");
        SecurityUtils.getSubject().logout();
        return view;
    }

    /**
     * 获取用户菜单数据
     * @return
     */
    @RequestMapping(value="getMenus.json", method ={RequestMethod.GET,RequestMethod.POST})
    @ResponseBody
    public ResultMsg getMenus(){
        String username = (String) SecurityUtils.getSubject().getPrincipal();
        List<CoreMenu> coreMenus =coreMenuDao.findMenuByNameSample(username);
        ResultMsg result=new ResultMsg();
        result.setData(coreMenus);
        return  result;
    }

}
