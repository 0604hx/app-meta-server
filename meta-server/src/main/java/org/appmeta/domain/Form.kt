package org.appmeta.domain

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Select
import org.nerve.boot.annotation.CN
import org.nerve.boot.domain.IDLong


/*
 * @project app-meta-server
 * @file    org.appmeta.domain.Form
 * CREATE   2022年12月06日 13:30 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 *
 * 2023-01-18  修改
        ALTER TABLE `app-meta`.form ADD url varchar(255) NULL COMMENT '后端服务地址';
        ALTER TABLE `app-meta`.form CHANGE url url varchar(255) NULL COMMENT '后端服务地址' AFTER labelWidth;
        ALTER TABLE `app-meta`.form ADD items TEXT NULL COMMENT 'JSON格式的表单项目（_ 开头的属性为基本属性，其他为扩展属性）';
        ALTER TABLE `app-meta`.form ADD buttons TEXT NULL COMMENT 'JSON格式的额外按钮（text 为按钮文本，theme 为配色，type 为操作类型，code 为具体的脚本）';
 */

@CN("表单")
class Form : IDLong {
    var aid		    = ""        //关联应用ID
    var summary     = ""
    var size        = "medium"  //整体表单尺寸，可选值：small、medium、large
    var labelShow   = true
    var labelPlacement = "top"  //标签对齐方式，可选值：top、left
    var labelAlign  = "left"    //表单项标签位置，可选值：left、right
    var labelWidth  = 120       //表单项标签宽度，默认值 120
    var grid        = 3         //表单布局每行显示的列数
    var width       = "100%"    //表单宽度
    var onLoad      = ""        //`JS 代码`表单初始化后调用
    var onSubmit    = ""        //`JS 代码`表单提交前调用钩子，用于进行数据预处理，也可以中断表单（返回 Promise）
    var afterSubmit = ""        //`JS 代码`表单提交完成后调用钩子

    var url         = ""        //后端服务地址
    var submitText  = "提交数据" //表单提交按钮的文本内容
    var okText      = ""        //表单提交成功后显示的文本内容，默认显示「数据提交完成，感谢支持」

    var items       = ""
    var buttons     = ""
    var hides       = ""

    constructor()
    constructor(app: App){
        aid = app.id
    }
}

@Mapper
interface FormMapper:BaseMapper<Form> {

    @Select("SELECT * FROM form WHERE aid=#{0} LIMIT 1")
    fun getOneByAid(aid:String):Form?
}