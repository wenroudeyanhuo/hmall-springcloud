package com.hmall.item.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

/**
 * @author 温柔的烟火
 * @date 2024/10/28-13:16
 */
@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticTest {

    @Autowired
    private IItemService iItemService;
    private RestHighLevelClient client;
    private static final String MAPPING_TEMPLATE="{\n" +
            "  \"info\": \"不好你好我是丁真\",\n" +
            "  \"email\": \"wenroudeyanhuo@gmail.com\",\n" +
            "  \"name\": {\n" +
            "    \"firstname\": \"理塘\",\n" +
            "    \"lastname\": \"丁真\"\n" +
            "  }\n" +
            "}\n" +
            "\n" +
            "#局部修改\n" +
            "POST /heima/_update/1\n" +
            "{\n" +
            "  \"doc\":{\n" +
            "    \"email\":\"dingzhen@gmail.com\"\n" +
            "  }\n" +
            "}\n" +
            "\n" +
            "#批量新增\n" +
            "POST /_bulk\n" +
            "{\"index\":{\"_index\":\"heima\",\"_id\":3}}\n" +
            "{\"info\": \"你好我是丁真\",\"email\": \"wenroudeyanhuo@gmail.com\",\"name\": {\"firstname\": \"理塘\",\"lastname\": \"丁真\"}}\n" +
            "{\"index\":{\"_index\":\"heima\",\"_id\":4}}\n" +
            "{\"info\": \"妈妈生的\",\"email\": \"wenroudeyanhuo@gmail.com\",\"name\": {\"firstname\": \"理塘\",\"lastname\": \"丁真\"}}\n" +
            "\n" +
            "#批量删除\n" +
            "POST /_bulk\n" +
            "{\"delete\":{\"_index\":\"heima\",\"_id\":3}}\n" +
            "{\"delete\":{\"_index\":\"heima\",\"_id\":4}}\n" +
            "\n" +
            "#商品索引库\n" +
            "PUT /hmall\n" +
            "{\n" +
            "  \"mappings\": {\n" +
            "    \"properties\": {\n" +
            "      \"id\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"name\":{\n" +
            "        \"type\": \"text\",\n" +
            "        \"analyzer\": \"ik_smart\"\n" +
            "      },\n" +
            "      \"price\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"image\":{\n" +
            "        \"type\": \"keyword\",\n" +
            "        \"index\": false\n" +
            "      },\n" +
            "      \"category\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"brand\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"sold\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"commentCount\":{\n" +
            "        \"type\": \"integer\",\n" +
            "        \"index\": false\n" +
            "      },\n" +
            "      \"isAD\":{\n" +
            "        \"type\": \"boolean\",\n" +
            "      },\n" +
            "      \"updateTime\":{\n" +
            "        \"type\": \"date\"\n" +
            "      },\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @Test
    void testConnection(){
        System.out.println("client:"+client);
    }
    @Test
    void testCreateIndex() throws IOException {

        //1.准备request对象
        CreateIndexRequest request=new CreateIndexRequest("items");
        //2.准备请求参数
        request.source(MAPPING_TEMPLATE, XContentType.JSON);
        //3.发送请求
        client.indices().create(request, RequestOptions.DEFAULT);

    }
    @Test
    void testGetIndex() throws IOException {

        //1.准备request对象
        GetIndexRequest request= new GetIndexRequest("items");
//        CreateIndexRequest request=new CreateIndexRequest("items");

        //3.发送请求
        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
        System.out.println("------exists：--------"+exists);

    }
    @Test
    void testDeleteIndex() throws IOException {

        //1.准备request对象
        DeleteIndexRequest request=new DeleteIndexRequest("items");

        //3.发送请求
        client.indices().delete(request, RequestOptions.DEFAULT);

    }
    @BeforeEach
    void setup(){
        client=new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.1.144:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (client!=null){
            client.close();
        }
    }

    //新增数据
    @Test
    void testIndexDoc() throws IOException {
        //准备文档数据
        Item item = iItemService.getById(317578L);
//        System.out.println(item);
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        //准备request
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        //2准备请求参数
        String doc = JSONUtil.toJsonStr(itemDoc);

        request.source(doc, XContentType.JSON);
        //3发送请求
        client.index(request, RequestOptions.DEFAULT);

    }
    @Test
    void testGetDoc() throws IOException {
        //准备request
        GetRequest request = new GetRequest("items","317578");

        //3发送请求
        GetResponse documentFields = client.get(request, RequestOptions.DEFAULT);
        //解析响应结果
        String sourceAsString = documentFields.getSourceAsString();
        ItemDoc itemDoc = JSONUtil.toBean(sourceAsString, ItemDoc.class);
        System.out.println("itemDoc = " + itemDoc);

    }
    @Test
    void testDeleteDoc() throws IOException {
        //准备request
        DeleteRequest request = new DeleteRequest("items","317578");
        //3发送请求
    client.delete(request, RequestOptions.DEFAULT);
    }
    @Test
    void testUpdateDoc() throws IOException {
        //准备request
       UpdateRequest request = new UpdateRequest("items","317578");
       request.doc(
               "price",25600
       );
        //3发送请求
        client.update(request, RequestOptions.DEFAULT);
    }

    //批量处理
    @Test
    void testBulkDoc() throws IOException {
        //准备数据
        int pageNo=1,pageSize=500;
        while (true){
            Page<Item> page= iItemService.lambdaQuery()
                    .eq(Item::getStatus, 1)
                    .page(Page.of(pageNo, pageSize));
            List<Item> records=page.getRecords();
            if (records==null||records.isEmpty()){
                return;
            }
            //准备request
            BulkRequest request = new BulkRequest();
            for (Item item:records){
                request.add(new IndexRequest("items")
                        .id(item.getId().toString())
                        .source(JSONUtil.toJsonStr(BeanUtil.copyProperties(item,ItemDoc.class)),XContentType.JSON));

            }
            client.bulk(request, RequestOptions.DEFAULT);
            pageNo++;
        }

    }
}
