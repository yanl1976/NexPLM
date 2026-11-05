package com.nexplm.platform.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexplm.platform.file.domain.FileLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FileLinkMapper extends BaseMapper<FileLink> {

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
