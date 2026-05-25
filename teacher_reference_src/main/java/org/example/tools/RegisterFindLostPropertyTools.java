package org.example.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;
import org.example.enitity.LostPropertyEntity;
import org.example.repository.LostPropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RegisterFindLostPropertyTools {

    @Autowired
    private LostPropertyRepository lostPropertyRepository;

    @Tool("获取根据手机号查询物品登记信息")
    public List<LostPropertyEntity> queryItemInfoByPhoneNumber(@P(value="用户手机号")String phone){
        log.info("根据手机号查询物品登记信息 手机号：{}",phone);
        List<LostPropertyEntity> allByPhone = lostPropertyRepository.findAllByPhone(phone);
        log.info("========根据手机号查询物品登记信息：数据：" + allByPhone);
        return allByPhone;
    }
}
