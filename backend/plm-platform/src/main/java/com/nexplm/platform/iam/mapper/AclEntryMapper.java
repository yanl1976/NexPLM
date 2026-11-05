package com.nexplm.platform.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexplm.platform.iam.domain.AclEntry;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AclEntryMapper extends BaseMapper<AclEntry> {

    @Delete("DELETE FROM sys_acl_entry WHERE object_id = #{objectId} AND source = 'MANUAL'")
    int deleteManualByObject(@Param("objectId") Long objectId);
}
