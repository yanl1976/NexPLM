package com.nexplm.platform.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexplm.platform.iam.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT DISTINCT p.code
              FROM sys_permission p
              JOIN sys_role_permission rp ON rp.permission_code = p.code
              JOIN sys_user_role ur ON ur.role_id = rp.role_id
             WHERE ur.user_id = #{userId}
            """)
    List<String> selectPermissionCodes(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT r.code
              FROM sys_role r
              JOIN sys_user_role ur ON ur.role_id = r.id
             WHERE ur.user_id = #{userId} AND r.status = 'ACTIVE'
            """)
    List<String> selectRoleCodes(@Param("userId") Long userId);
}
