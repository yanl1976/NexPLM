package com.nexplm.platform.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexplm.platform.iam.domain.PermissionRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionRuleMapper extends BaseMapper<PermissionRule> {

    @Select("""
            SELECT * FROM sys_permission_rule
             WHERE object_type = #{objectType} AND enabled = true
             ORDER BY priority ASC, id ASC
            """)
    List<PermissionRule> selectEnabledByType(@Param("objectType") String objectType);
}
