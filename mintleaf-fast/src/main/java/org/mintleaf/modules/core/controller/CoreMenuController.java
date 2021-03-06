package org.mintleaf.modules.core.controller;

import org.beetl.sql.core.engine.PageQuery;
import org.mintleaf.modules.core.dao.CoreMenuDao;
import org.mintleaf.modules.core.entity.CoreMenu;
import org.mintleaf.vo.PageFrame;
import org.mintleaf.vo.ResultMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;
import java.util.List;
/**
 * 菜单相关控制器
 * @Author: MengchuZhang
 * @Date: 2018/8/22 11:15
 * @Version 1.0
 */

@Controller
@RequestMapping("coreMenu")
public class CoreMenuController {

    @Autowired
    CoreMenuDao coreMenuDao;

    /**
     * 进入列表页面
     * @return
     */
    @RequestMapping(value="index.html")
    public ModelAndView index(){
        ModelAndView view =new ModelAndView("modules/core/coremenu/index.html");
        return view;
    }

    /**
     * 进入编辑页面
     * @return
     */
    @RequestMapping(value="edit.html")
    public ModelAndView edit(){
        ModelAndView view =new ModelAndView("modules/core/coremenu/edit.html");
        return view;
    }

    /**
     * 进入新增页面
     * @return
     */
    @RequestMapping(value="add.html")
    public ModelAndView add(){
        ModelAndView view =new ModelAndView("modules/core/coremenu/add.html");
        return view;
    }

    /**
     * 添加操作
     * @param coreMenu
     * @return
     */
    @RequestMapping(value = "add.do",method = {RequestMethod.POST,RequestMethod.GET})
    @ResponseBody
    public ResultMsg add(CoreMenu coreMenu) {
        coreMenuDao.insertTemplate(coreMenu);
        ResultMsg result=new ResultMsg();
        result.setData(coreMenu);
        return result;
    }

    /**
     * 删除操作
     * @param coreMenu
     * @return
     */
    @RequestMapping(value = "delete.do",method = {RequestMethod.POST,RequestMethod.GET})
    @ResponseBody
    public ResultMsg delete(CoreMenu coreMenu) {
        coreMenuDao.deleteById(coreMenu.getId());
        ResultMsg result=new ResultMsg();
        return result;
    }

    /**
     * 批量删除操作
     * @param id
     * @return
     */
    @RequestMapping(value = "deleteBatch.do",method = {RequestMethod.POST,RequestMethod.GET})
    @ResponseBody
    public ResultMsg deleteBatch(String id) {
        coreMenuDao.deleteByIds(id.split(","));
        ResultMsg result=new ResultMsg();
        return result;
    }

    /**
     * 编辑操作
     * @param coreMenu
     * @return
     */
    @RequestMapping(value = "edit.do",method = {RequestMethod.POST,RequestMethod.GET})
    @ResponseBody
    public ResultMsg update(CoreMenu coreMenu) {
        coreMenu.setUpdatetime(new Date());
        coreMenuDao.updateTemplateById(coreMenu);
        ResultMsg result=new ResultMsg();
        result.setData(coreMenu);
        return result;
    }

    /**
     * 查询单条数据操作
     * @param coreMenu
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "findById.json",method = {RequestMethod.POST,RequestMethod.GET}) //请求类型
    @ResponseBody
    public ResultMsg updateCoreMenu(CoreMenu coreMenu) throws Exception{
        CoreMenu menu=coreMenuDao.single(coreMenu.getId());
        ResultMsg result=new ResultMsg();
        result.setData(menu);
        return result;

    }

    /**
     * 查询全部数据操作
     * @return
     */
    @RequestMapping(value="all.json")
    @ResponseBody
    public ResultMsg all(){
        List<CoreMenu> coreMenus =coreMenuDao.all();
        CoreMenu coreMenu=new CoreMenu();
        coreMenu.setId(0);
        coreMenu.setName("/根目录");
        //向菜单list第一个位置插入根目录
        coreMenus.add(0,coreMenu);
        ResultMsg result=new ResultMsg();
        result.setData(coreMenus);
        return  result;
    }

    /**
     * 查询分页数据操作
     * @param coreMenu
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping(value="page.json",method = {RequestMethod.POST,RequestMethod.GET})
    @ResponseBody
    public ResultMsg page(@ModelAttribute CoreMenu coreMenu, Long pageNum, Long pageSize){
        PageQuery<CoreMenu> pageQuery=new PageQuery<CoreMenu>();
        pageQuery.setPageSize(Long.valueOf(pageSize));
        pageQuery.setPageNumber(Long.valueOf(pageNum));
        pageQuery.setParas(coreMenu);
        coreMenuDao.templatePage(pageQuery);
        PageFrame pageFrame=new PageFrame();
        pageFrame.setList(pageQuery.getList());
        pageFrame.setPageNum(Long.valueOf(pageNum));
        pageFrame.setPageSize(Long.valueOf(pageSize));
        pageFrame.setPages(pageQuery.getTotalPage());
        pageFrame.setTotal(pageQuery.getTotalRow());
        ResultMsg result=new ResultMsg();
        result.setData(pageFrame);
        return  result;
    }

}
