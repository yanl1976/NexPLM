package com.nexplm.platform.security.scope;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.nexplm.platform.iam.domain.SysOrg;
import com.nexplm.platform.object.domain.PlmObject;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScopeApplierTest {

    private final ScopeApplier applier = new ScopeApplier();

    @BeforeAll
    static void initTableInfo() {
        // 纯单元测试没有 MyBatis 上下文，需手动初始化实体与列的映射缓存
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysOrg.class);
        TableInfoHelper.initTableInfo(assistant, PlmObject.class);
    }

    @AfterEach
    void tearDown() {
        DataScopeContext.clear();
    }

    @Test
    void allScopeAddsNoCondition() {
        DataScopeContext.set(new DataScopeContext.Scope(DataScopeType.ALL, 1L, 10L, List.of()));
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        applier.apply(wrapper, SysOrg::getCompanyId, SysOrg::getId, null);
        assertThat(wrapper.getSqlSegment()).isEmpty();
    }

    @Test
    void companyScopeOnlyFiltersCompany() {
        DataScopeContext.set(new DataScopeContext.Scope(DataScopeType.COMPANY, 1L, 10L, List.of()));
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        applier.apply(wrapper, SysOrg::getCompanyId, SysOrg::getId, null);
        assertThat(wrapper.getSqlSegment()).contains("company_id").doesNotContain("IN");
    }

    @Test
    void orgScopeFiltersOrgSubtree() {
        DataScopeContext.set(new DataScopeContext.Scope(DataScopeType.ORG, 1L, 10L, List.of(12L, 13L)));
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        applier.apply(wrapper, SysOrg::getCompanyId, SysOrg::getId, null);
        assertThat(wrapper.getSqlSegment()).contains("company_id").contains("IN");
    }

    @Test
    void emptyOrgSubtreeDeniesAll() {
        DataScopeContext.set(new DataScopeContext.Scope(DataScopeType.ORG, 1L, 10L, List.of()));
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        applier.apply(wrapper, SysOrg::getCompanyId, SysOrg::getId, null);
        assertThat(wrapper.getSqlSegment()).contains("1 = 0");
    }

    @Test
    void selfScopeUsesOwnerColumn() {
        DataScopeContext.set(new DataScopeContext.Scope(DataScopeType.SELF, 1L, 10L, List.of()));
        LambdaQueryWrapper<PlmObject> wrapper = new LambdaQueryWrapper<>();
        applier.apply(wrapper, PlmObject::getCompanyId, PlmObject::getOrgId, PlmObject::getOwnerId);
        assertThat(wrapper.getSqlSegment()).contains("owner_id");
    }

    @Test
    void missingColumnDeniesInsteadOfLeaking() {
        DataScopeContext.set(new DataScopeContext.Scope(DataScopeType.SELF, 1L, 10L, List.of()));
        LambdaQueryWrapper<SysOrg> wrapper = new LambdaQueryWrapper<>();
        applier.apply(wrapper, SysOrg::getCompanyId, SysOrg::getId, null);
        assertThat(wrapper.getSqlSegment()).contains("1 = 0");
    }
}
