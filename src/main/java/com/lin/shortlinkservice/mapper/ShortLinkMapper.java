package com.lin.shortlinkservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lin.shortlinkservice.entity.ShortLink;
import org.apache.ibatis.annotations.Mapper;

/**
 * 短链接数据访问层
 */
@Mapper
public interface ShortLinkMapper extends BaseMapper<ShortLink> {
}
