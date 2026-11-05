package com.nexplm.platform.metadata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexplm.platform.metadata.domain.MetaField;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MetaFieldMapper extends BaseMapper<MetaField> {

    @Delete("DELETE FROM meta_field WHERE entity_code = #{entityCode}")
    int deleteByEntity(@Param("entityCode") String entityCode);
}
