package com.hmall.item;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.common.utils.BeanUtils;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * ClassName: ElasticSearchTest
 * Package: com.hmall.item
 * Description:
 *
 * @Author liang
 * @Create 2024/10/26 12:13
 * @Version jdk17.0
 */
/*@SpringBootTest(properties = "spring.profiles.active=local")*/
public class ElasticSearchTest {
    @Autowired
    private IItemService iItemService;
    private RestHighLevelClient restHighLevelClient;
    @Test
    public void test(){
        System.out.println(restHighLevelClient);
    }
    @Test
    public void test1() throws IOException {
        CreateIndexRequest request = new CreateIndexRequest("items");
        request.source(MAPPING_TEMPLATE, XContentType.JSON);
        restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
    
    }
    @Test
    public void test3() throws IOException {
        int pageNo=1; int pageSize= 500;
      while (true){
          Page<Item> itemPage = new Page<>(pageNo, pageSize);
          LambdaQueryWrapper<Item> itemLambdaQueryWrapper = new LambdaQueryWrapper<>();
          itemLambdaQueryWrapper.eq(Item::getStatus,1);
          Page<Item> page = iItemService.page(itemPage, itemLambdaQueryWrapper);
          List<Item> records = page.getRecords();
          if(records == null || records.isEmpty()) {
              return;
          }
          BulkRequest request =new BulkRequest();
          for (Item item : records) {
              ItemDoc doc = BeanUtils.copyProperties(item, ItemDoc.class);
              request.add(new IndexRequest("items").id(item.getId().toString()).source(JSONUtil.toJsonStr(doc),XContentType.JSON));
          }
          restHighLevelClient.bulk(request,RequestOptions.DEFAULT);
          pageNo++;
      }
    }

    /**
     * DSL查询操作
     */
    @Test
    public void search() throws IOException {
        SearchRequest request = new SearchRequest("items");
        request.source().query(QueryBuilders.matchAllQuery());

            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            SearchHits hits = response.getHits();
            SearchHit[] hits1 = hits.getHits();
            for (SearchHit hit : hits1) {
                String sourceAsString = hit.getSourceAsString();
                System.out.println(sourceAsString);
            }
    }

    /**
     * 高亮 DSL
     */
    @Test
    public void test4() throws IOException {
        SearchRequest request = new SearchRequest("items");
        request.source().query(QueryBuilders.matchQuery("name","脱脂牛奶"));
        request.source().highlighter(new HighlightBuilder().field("name").preTags("<em>").postTags("</em>"));
        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        parseResult(response);

    }

    /**
     * 聚合DSl
     */
    @Test
    public void test5() throws IOException {
        String aggName ="brandAgg";
        SearchRequest request = new SearchRequest("items");
        request.source().size(0);
        request.source().aggregation(AggregationBuilders.terms(aggName).field("brand").size(10));
        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        Terms aggregation = response.getAggregations().get(aggName);
        List<? extends Terms.Bucket> buckets = aggregation.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("品牌"+keyAsString);
            long docCount = bucket.getDocCount();
            System.out.println("数量"+docCount);

        }


    }




    @BeforeEach
    public void setUp() {
        restHighLevelClient = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.40.103:9200")
        ));

    }
    @AfterEach
    public void tearDown() throws Exception {
      if(restHighLevelClient != null){
          restHighLevelClient.close();
      }
    }
    private final  static  String MAPPING_TEMPLATE ="{\n" +
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
            "        \"type\": \"binary\"\n" +
            "        , \"index\": false\n" +
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
            "        \"type\": \"integer\"\n" +
            "         , \"index\": false\n" +
            "      },\n" +
            "      \"isAD\":{\n" +
            "        \"type\": \"boolean\"\n" +
            "      },\n" +
            "      \"updateTime\":{\n" +
            "        \"type\": \"date\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";


    public void parseResult(SearchResponse response){
        SearchHits hits = response.getHits();
        SearchHit[] hits1 = hits.getHits();
        for (SearchHit hit : hits1) {
            String sourceAsString = hit.getSourceAsString();
            ItemDoc itemDoc = JSONUtil.toBean(sourceAsString, ItemDoc.class);
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if(highlightFields !=null && !highlightFields.isEmpty()){
                //取出高亮结果
                HighlightField hf = highlightFields.get("name");
                String highName = hf.getFragments()[0].toString();
                //替换itemDoc中name
                itemDoc.setName(highName);
                System.out.println(itemDoc.toString());
            }

        }
    }

}
