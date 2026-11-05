package com.nexplm.platform.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexplm.platform.file.domain.FileUploadSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FileUploadSessionMapper extends BaseMapper<FileUploadSession> {

    @Update("UPDATE file_upload_session SET status = #{status}, updated_at = now() WHERE id = #{id}")
    int updateStatus(@Param("id") String id, @Param("status") String status);

    /** 刷新心跳时间，供超时未完成会话的清理任务识别。 */
    @Update("UPDATE file_upload_session SET updated_at = now() WHERE id = #{id}")
    int touch(@Param("id") String id);
}
