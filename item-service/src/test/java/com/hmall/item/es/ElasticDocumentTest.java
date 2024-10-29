package com.hmall.item.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

/**
 * @author 温柔的烟火
 * @date 2024/10/28-18:32
 */
@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticDocumentTest {

    private RestHighLevelClient client;

    @Autowired
    private IItemService iItemService;

    @Test
    void testConnection(){
        System.out.println("client:"+client);
    }
    @Test
    void testIndexDoc() throws IOException {
        //准备文档数据
        Item item = iItemService.getById(317578L);
        System.out.println(item);
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        //准备request
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        //2准备请求参数
        String doc = JSONUtil.toJsonStr(itemDoc);

        request.source(doc, XContentType.JSON);
        //3发送请求
        client.index(request, RequestOptions.DEFAULT);

    }

}
