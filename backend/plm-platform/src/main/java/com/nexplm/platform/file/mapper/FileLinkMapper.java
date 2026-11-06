package com.nexplm.platform.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexplm.platform.file.domain.FileLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FileLinkMapper extends BaseMapper<FileLink> {

    /** 物理文件被哪些对象引用（内容寻址共享同一份物理文件时可能有多个）。 */
    @Select("SELECT DISTINCT owner_object_id FROM file_link WHERE file_id = #{fileId}")
    List<Long> ownerIds(@Param("fileId") Long fileId);

    @Update("""
            UPDATE file_link SET is_latest = false
             WHERE owner_object_id = #{ownerObjectId} AND file_role = #{fileRole} AND is_latest = true
            """)
    int markNotLatest(@Param("ownerObjectId") Long ownerObjectId, @Param("fileRole") String fileRole);

    /** 同一对象同一角色下的下一个版本号（首个版本为 1）。 */
    @Select("""
            SELECT COALESCE(MAX(version_no), 0) + 1 FROM file_link
             WHERE owner_object_id = #{ownerObjectId} AND file_role = #{fileRole}
            """)
    int nextVersionNo(@Param("ownerObjectId") Long ownerObjectId, @Param("fileRole") String fileRole);

    /** 删除最新版本后，把剩余的最大版本重新标记为最新。 */
    @Update("""
            UPDATE file_link SET is_latest = true
             WHERE id = (SELECT id FROM file_link
                          WHERE owner_object_id = #{ownerObjectId} AND file_role = #{fileRole}
                          ORDER BY version_no DESC LIMIT 1)
            """)
    int promoteLatest(@Param("ownerObjectId") Long ownerObjectId, @Param("fileRole") String fileRole);
}
