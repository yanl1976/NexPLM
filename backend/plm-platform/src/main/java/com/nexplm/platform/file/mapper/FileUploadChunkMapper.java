package com.nexplm.platform.file.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 分片登记表（复合主键 session_id + chunk_index，故不用 BaseMapper）。
 */
@Mapper
public interface FileUploadChunkMapper {

    /** 分片登记幂等：重复上传同一序号只更新大小。 */
    @Insert("""
            INSERT INTO file_upload_chunk (session_id, chunk_index, size)
            VALUES (#{sessionId}, #{chunkIndex}, #{size})
            ON CONFLICT (session_id, chunk_index)
            DO UPDATE SET size = excluded.size, created_at = now()
            """)
    int upsert(@Param("sessionId") String sessionId,
               @Param("chunkIndex") int chunkIndex,
               @Param("size") int size);

    @Select("SELECT chunk_index FROM file_upload_chunk WHERE session_id = #{sessionId} ORDER BY chunk_index")
    List<Integer> listIndexes(@Param("sessionId") String sessionId);

    @Select("SELECT COUNT(*) FROM file_upload_chunk WHERE session_id = #{sessionId}")
    int countChunks(@Param("sessionId") String sessionId);

    @Delete("DELETE FROM file_upload_chunk WHERE session_id = #{sessionId}")
    int deleteAll(@Param("sessionId") String sessionId);
}
