package org.example.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class QueryLostPropertyTools {

    @Autowired
    private ContentRetriever contentRetriever;

    @Tool("根据物品及物品特征查询物品登记信息")
    public List<String> queryLostProperty(@P(value = "物品名称和特性") String lostProperty){
        log.info("根据物品及物品特征查询物品信息登记查询条件,{}",lostProperty);
        List<String> list = contentRetriever.retrieve(new Query(lostProperty)).stream().map(x -> x.textSegment().text()).toList();
        log.info("根据物品及物品特征查询物品信息登记结果,{}",list);
        return list;
    }
}
