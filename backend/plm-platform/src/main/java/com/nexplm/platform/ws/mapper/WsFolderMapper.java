package com.nexplm.platform.ws.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexplm.platform.ws.domain.WsFolder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WsFolderMapper extends BaseMapper<WsFolder> {

    /**
     * 移动文件夹后按前缀重写子树路径。
     *
     * @param oldPath 原前缀（含自身，形如 /1/2/）
     * @param newPath 新前缀
     * @param cut     从原路径第几位开始截取（oldPath.length() + 1，PostgreSQL substring 从 1 开始）
     * @param selfId  当前文件夹本身（已单独更新，跳过）
     */
    @Update("""
            UPDATE ws_folder SET path = #{newPath} || substring(path from #{cut})
             WHERE path LIKE #{oldPath} || '%' AND id <> #{selfId} AND deleted = false
            """)
    int rebaseSubtree(@Param("oldPath") String oldPath,
                      @Param("newPath") String newPath,
                      @Param("cut") int cut,
                      @Param("selfId") Long selfId);
}
