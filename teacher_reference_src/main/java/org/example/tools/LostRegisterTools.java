package org.example.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.example.enitity.LostRegisterEntity;
import org.example.repository.LostRegisterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class LostRegisterTools {

    @Autowired
    private LostRegisterRepository lostRegisterRepository;

    @Tool("根据手机号码查询丢失信息")
    public List<LostRegisterEntity> queryLostRegisterByPhone(@P("用户手机号") String phone){
        log.info("根据手机号码查询丢失信息,手机号：{}",phone);
        return lostRegisterRepository.findAllByPhone(phone);
    }

}
