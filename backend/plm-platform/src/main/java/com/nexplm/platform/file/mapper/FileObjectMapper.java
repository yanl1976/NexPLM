package com.nexplm.platform.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexplm.platform.file.domain.FileObject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FileObjectMapper extends BaseMapper<FileObject> {

    @Update("UPDATE file_object SET ref_count = ref_count + #{delta} WHERE id = #{id}")
    int addRefCount(@Param("id") Long id, @Param("delta") int delta);

    @Update("UPDATE file_object SET scan_status = #{status} WHERE id = #{id}")
    int updateScanStatus(@Param("id") Long id, @Param("status") String status);
}
