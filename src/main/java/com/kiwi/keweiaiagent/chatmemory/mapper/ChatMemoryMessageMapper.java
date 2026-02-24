package com.kiwi.keweiaiagent.chatmemory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kiwi.keweiaiagent.chatmemory.entity.ChatMemoryMessageDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMemoryMessageMapper extends BaseMapper<ChatMemoryMessageDO> {
}
