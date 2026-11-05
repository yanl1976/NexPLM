package com.nexplm.platform.ws.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexplm.platform.ws.domain.WsFolderContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WsFolderContentMapper extends BaseMapper<WsFolderContent> {

    @Select("SELECT COUNT(1) FROM ws_folder_content WHERE folder_id = #{folderId}")
    int countByFolder(@Param("folderId") Long folderId);

    @Select("SELECT COUNT(1) FROM ws_folder_content WHERE folder_id = #{folderId} AND object_id = #{objectId}")
    int countByFolderAndObject(@Param("folderId") Long folderId, @Param("objectId") Long objectId);
}
