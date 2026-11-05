package com.nexplm.platform.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexplm.platform.iam.domain.SysRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("SELECT permission_code FROM sys_role_permission WHERE role_id = #{roleId}")
    List<String> selectPermissionCodes(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int deletePermissions(@Param("roleId") Long roleId);

    @Insert("""
            <script>
            INSERT INTO sys_role_permission (role_id, permission_code) VALUES
            <foreach collection='codes' item='code' separator=','>
              (#{roleId}, #{code})
            </foreach>
            </script>
            """)
    int insertPermissions(@Param("roleId") Long roleId, @Param("codes") List<String> codes);

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUser(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteUserRoles(@Param("userId") Long userId);

    @Insert("""
            <script>
            INSERT INTO sys_user_role (user_id, role_id) VALUES
            <foreach collection='roleIds' item='roleId' separator=','>
              (#{userId}, #{roleId})
            </foreach>
            </script>
            """)
    int insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    @Select("SELECT COUNT(1) FROM sys_user_role WHERE role_id = #{roleId}")
    int countUsers(@Param("roleId") Long roleId);
}
