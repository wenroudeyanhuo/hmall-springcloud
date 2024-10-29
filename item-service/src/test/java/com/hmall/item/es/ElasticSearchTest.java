package com.hmall.item.es;

import cn.hutool.json.JSONUtil;
import com.hmall.item.domain.po.ItemDoc;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author 温柔的烟火
 * @date 2024/10/29-10:22
 */
public class ElasticSearchTest {
    private RestHighLevelClient client;

    @Test
    void  testMarchAll() throws IOException {
        //创建request对像
        SearchRequest items = new SearchRequest("items");
        //2配置request参数
        items.source()
                .query(QueryBuilders.matchAllQuery());

        //发送请求
        SearchResponse search = client.search(items, RequestOptions.DEFAULT);
//        System.out.println("search = " + search);
        //解析结果
        parseResponseResult(search);
    }
    @Test
    void  testSearch() throws IOException {
        //创建request对像
        SearchRequest items = new SearchRequest("items");
        //2配置request参数
        items.source()
                .query(QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("name","脱脂牛奶"))
                        .filter(QueryBuilders.termQuery("brand.keyword","德亚"))
                        .filter(QueryBuilders.rangeQuery("price").lt(30000))

                );

        //发送请求
        SearchResponse search = client.search(items, RequestOptions.DEFAULT);
//        System.out.println("search = " + search);
        //解析结果
        parseResponseResult(search);
    }
    @Test
    void  testSortAndPage() throws IOException {
//        前段传递的参数
        int pageNo=2,pageSize=5;

        //创建request对像
        SearchRequest items = new SearchRequest("items");
        //2配置request参数
        items.source().query(QueryBuilders.matchAllQuery());
        //分页
        items.source().from((pageNo-1)*pageSize).size(pageSize);
        //排序
        items.source().sort("sold", SortOrder.DESC)
                  .sort("price",SortOrder.ASC);
        //发送请求
        SearchResponse search = client.search(items, RequestOptions.DEFAULT);
//        System.out.println("search = " + search);
        //解析结果
        parseResponseResult(search);
    }
    @Test
    void  testHighLight() throws IOException {
        //创建request对像
        SearchRequest items = new SearchRequest("items");
        //2配置request参数
        items.source().query(QueryBuilders.matchQuery("name","脱脂牛奶"));
        //高亮条件
        items.source().highlighter(new HighlightBuilder().field("name"));
        //发送请求
        SearchResponse search = client.search(items, RequestOptions.DEFAULT);
//        System.out.println("search = " + search);
        //解析结果
        //注意这里的高亮

        parseResponseResult(search);
    }
    @Test
    void  testAgg() throws IOException {
        //创建request对像
        SearchRequest items = new SearchRequest("items");
        //2配置request参数
        String brandAggName="brandAgg";
        items.source().aggregation(
                AggregationBuilders.terms(brandAggName).field("brand.keyword").size(10)
        );
        //发送请求
        SearchResponse search = client.search(items, RequestOptions.DEFAULT);
//        System.out.println("search = " + search);

//        解析结果
        Aggregations aggregations = search.getAggregations();
        //根据聚合名称找聚合
        Terms terms = aggregations.get(brandAggName);
        //获取桶
        //这里是term聚合注意
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        //遍历桶
        for (Terms.Bucket bucket : buckets) {
            System.out.println("bucket.getKeyAsString() = " + bucket.getKeyAsString());
            System.out.println("bucket.getDocCount() = " + bucket.getDocCount());

        }
    }

    private void parseResponseResult(SearchResponse search) {
        SearchHits hits = search.getHits();
        long value = hits.getTotalHits().value;//总条数
        System.out.println("value = " + value);
        //命中数据
        SearchHit[] hits1 = hits.getHits();
        for (SearchHit documentFields : hits1) {
        //获取source 结果
            String json=documentFields.getSourceAsString();
            //装成ItemDoc
            ItemDoc itemDoc = JSONUtil.toBean(json, ItemDoc.class);
            //处理高亮结果
            Map<String, HighlightField> highlightFields = documentFields.getHighlightFields();

            if (highlightFields!=null&&highlightFields.isEmpty()){
                //根据高亮字段名取得高亮结果
                HighlightField name = highlightFields.get("name");
                //获取高亮结果
                String string = name.getFragments()[0].string();
                itemDoc.setName(string);
            }
            System.out.println("itemDoc = " + itemDoc);
        }
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
}
