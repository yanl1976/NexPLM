package com.nexplm.platform.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexplm.platform.doc.domain.DocMaster;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DocMasterMapper extends BaseMapper<DocMaster> {

    /** 文件增删后刷新统计（文件数、主文件、扩展名）。 */
    @Update("""
            UPDATE doc_master
               SET file_count = #{fileCount}, main_file_id = #{mainFileId},
                   file_ext = #{fileExt}, updated_at = now()
             WHERE object_id = #{objectId}
            """)
    int updateFileStats(@Param("objectId") Long objectId,
                        @Param("fileCount") int fileCount,
                        @Param("mainFileId") Long mainFileId,
                        @Param("fileExt") String fileExt);
}
