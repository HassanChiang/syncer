package com.github.zzt93.syncer.consumer.output.channel.elastic;

import com.github.zzt93.syncer.common.data.SyncData;
import com.github.zzt93.syncer.common.data.SyncDataTestUtil;
import com.github.zzt93.syncer.config.consumer.output.elastic.Elasticsearch;
import com.google.common.collect.Lists;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.support.AbstractClient;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.github.zzt93.syncer.common.data.ESScriptUpdate.BY_ID_SUFFIX;
import static org.junit.Assert.*;

/**
 * @author zzt
 */
public class ESRequestMapperTest {

  public static void mergeToListRemote() throws Exception {
    AbstractClient client = ElasticTestUtil.getDevClient();
    remoteCheck(client, innerMergeToList());
  }

  private static List<Object> innerMergeToList() throws Exception {
    List<Object> res = new ArrayList<>();

    AbstractClient client = ElasticTestUtil.getDevClient();
    Elasticsearch elasticsearch = new Elasticsearch();
    ESRequestMapper mapper = new ESRequestMapper(client, elasticsearch.getRequestMapping());

    SyncData data = SyncDataTestUtil.write("list", "list");
    data.addField("roles", new ArrayList<>());
    Object builder = mapper.map(data);
    assertEquals("", "index {[list][list][1234], source[{\"roles\":[]}]}",
        ((IndexRequestBuilder) builder).request().toString());
    res.add(builder);


    data = SyncDataTestUtil.write("list", "list");
    data.addField("role", 1381034L);
    data.addField("test_id", 1234L);
    data.esScriptUpdate().mergeToList("roles", "test_id", "role");

    builder = mapper.map(data);
    assertEquals("", "update {[list][list][1234], script[Script{type=inline, lang='painless', idOrCode='ctx._source.roles.add(params.roles);', options={}, params={roles=1381034}}], detect_noop[true]}",
        ElasticsearchChannel.toString(((UpdateRequestBuilder) builder).request()));
    res.add(builder);


    data = SyncDataTestUtil.delete("list", "list");
    data.addField("role", 1381034L);
    data.addField("test_id", 1234L);
    data.esScriptUpdate().mergeToList("roles", "test_id", "role");

    builder = mapper.map(data);
    assertEquals("", "update {[list][list][1234], script[Script{type=inline, lang='painless', idOrCode='ctx._source.roles.removeIf(Predicate.isEqual(params.roles));', options={}, params={roles=1381034}}], detect_noop[true]}",
        ElasticsearchChannel.toString(((UpdateRequestBuilder) builder).request()));
    res.add(builder);

    return res;
  }

  @Test
  public void mergeToList() throws Exception {
    innerMergeToList();
  }

  public static void mergeToListByIdRemote() throws Exception {
    AbstractClient client = ElasticTestUtil.getDevClient();
    remoteCheck(client, innerMergeToListById());
  }

  private static List<Object> innerMergeToListById() throws Exception {
    List<Object> res = new ArrayList<>();

    AbstractClient client = ElasticTestUtil.getDevClient();
    Elasticsearch elasticsearch = new Elasticsearch();
    ESRequestMapper mapper = new ESRequestMapper(client, elasticsearch.getRequestMapping());

    SyncData data = SyncDataTestUtil.write();
    data.addField("roles" + BY_ID_SUFFIX, new ArrayList<>());
    data.addField("roles", new ArrayList<>());
    Object builder = mapper.map(data);
    res.add(builder);

    data = SyncDataTestUtil.write();
    data.addField("role", 1381034L);
    data.addField("test_id", 1234L);
    data.esScriptUpdate().mergeToListById("roles", "test_id", "role");

    builder = mapper.map(data);
    assertEquals("", "update {[test][test][1234], script[Script{type=inline, lang='painless', idOrCode='if (!ctx._source.roles_id.contains(params.roles_id)) {ctx._source.roles_id.add(params.roles_id); ctx._source.roles.add(params.roles); }', options={}, params={roles_id=1234, roles=1381034}}], detect_noop[true]}",
        ElasticsearchChannel.toString(((UpdateRequestBuilder) builder).request()));
    res.add(builder);

    data = SyncDataTestUtil.delete();
    data.addField("role", 13276746L);
    data.addField("test_id", 1234L);
    data.esScriptUpdate().mergeToListById("roles", "test_id", "role");

    builder = mapper.map(data);
    assertEquals("", "update {[test][test][1234], script[Script{type=inline, lang='painless', idOrCode='if (ctx._source.roles_id.removeIf(Predicate.isEqual(params.roles_id))) {ctx._source.roles.removeIf(Predicate.isEqual(params.roles)); }', options={}, params={roles_id=1234, roles=13276746}}], detect_noop[true]}",
        ElasticsearchChannel.toString(((UpdateRequestBuilder) builder).request()));
    res.add(builder);

    return res;
  }

  @Test
  public void mergeToListById() throws Exception {
    innerMergeToListById();
  }

  /**
   * should PUT template first:
   * <pre>
   * PUT _template/nested_template
   * {
   *   "template": "nested*",
   *   "mappings": {
   *     "nested": {
   *       "properties": {
   *         "roles": {
   *           "type": "nested"
   *         }
   *       }
   *     }
   *   }
   * }
   * </pre>
   */
  public static void nestedByIdRemote() throws Exception {
    AbstractClient client = ElasticTestUtil.getDevClient();
    remoteCheck(client, innerNestedById());
  }

  @Test
  public void nestedById() throws Exception {
    innerNestedById();
  }

  private static List<Object> innerNestedById() throws Exception {
    List<Object> res = new ArrayList<>();

    AbstractClient client = ElasticTestUtil.getDevClient();
    Elasticsearch elasticsearch = new Elasticsearch();
    ESRequestMapper mapper = new ESRequestMapper(client, elasticsearch.getRequestMapping());

    SyncData data = SyncDataTestUtil.write("nested", "nested");
    data.setId(1L);
    data.addField("roles", new ArrayList<>());
    Object builder = mapper.map(data);
    res.add(builder);

    data = SyncDataTestUtil.write("nested", "nested");
    data.addField("role", 1381034L);
    data.addField("ann_id", 1L);
    data.esScriptUpdate().mergeToNestedById("roles", "ann_id", "role");

    builder = mapper.map(data);
    assertEquals("", "update {[nested][nested][1], script[Script{type=inline, lang='painless', idOrCode='if (ctx._source.roles.find(e -> e.id.equals(params.roles_id)) == null) {  ctx._source.roles.add(params.roles);}', options={}, params={roles_id=1234, roles={role=1381034, id=1234}}}], detect_noop[true]}",
        ElasticsearchChannel.toString(((UpdateRequestBuilder) builder).request()));
    res.add(builder);

    data = SyncDataTestUtil.write("nested", "nested");
    data.addField("role", 2381034L).addField("ann_id", 1L).setId(2345);
    data.esScriptUpdate().mergeToNestedById("roles", "ann_id", "role");

    builder = mapper.map(data);
    assertEquals("", "update {[nested][nested][1], script[Script{type=inline, lang='painless', idOrCode='if (ctx._source.roles.find(e -> e.id.equals(params.roles_id)) == null) {  ctx._source.roles.add(params.roles);}', options={}, params={roles_id=2345, roles={role=2381034, id=2345}}}], detect_noop[true]}",
        ElasticsearchChannel.toString(((UpdateRequestBuilder) builder).request()));
    res.add(builder);

    data = SyncDataTestUtil.update("nested", "nested");
    data.getBefore().put("role", 1381034L);
    data.addField("role", 13276746L);
    data.addField("ann_id", 1L);
    data.esScriptUpdate().mergeToNestedById("roles", "ann_id", "role");

    builder = mapper.map(data);
    assertEquals("", "update {[nested][nested][1], script[Script{type=inline, lang='painless', idOrCode='def target = ctx._source.roles.find(e -> e.id.equals(params.roles_id));if (target != null) { target.role = params.role;target.id = params.id;}', options={}, params={role=13276746, id=1234, roles_id=1234}}], detect_noop[true]}",
        ElasticsearchChannel.toString(((UpdateRequestBuilder) builder).request()));
    res.add(builder);

    data = SyncDataTestUtil.delete("nested", "nested");
    data.addField("role", 13276746L).addField("ann_id", 1L).setId(2345L);
    data.esScriptUpdate().mergeToNestedById("roles", "ann_id", "role");

    builder = mapper.map(data);
    assertEquals("", "update {[nested][nested][1], script[Script{type=inline, lang='painless', idOrCode='ctx._source.roles.removeIf(e -> e.id.equals(params.roles_id)); ', options={}, params={roles_id=2345}}], detect_noop[true]}",
        ElasticsearchChannel.toString(((UpdateRequestBuilder) builder).request()));
    res.add(builder);

    return res;
  }

  @Test
  public void setFieldNull() throws Exception {
    innerSetFieldNull();
  }

  private static List<Object> innerSetFieldNull() throws Exception {
    List<Object> res = new ArrayList<>();

    AbstractClient client = ElasticTestUtil.getDevClient();
    Elasticsearch elasticsearch = new Elasticsearch();
    ESRequestMapper mapper = new ESRequestMapper(client, elasticsearch.getRequestMapping());

    SyncData data = SyncDataTestUtil.write();
    data.addField("list", Lists.newArrayList(1)).addField("int", 1).addField("str", "1");
    Object builder = mapper.map(data);
    assertEquals("", "index {[test][test][1234], source[{\"str\":\"1\",\"list\":[1],\"int\":1}]}",
        ((IndexRequestBuilder) builder).request().toString());
    res.add(builder);

    data = SyncDataTestUtil.update();
    data.setFieldNull("int").setFieldNull("str").setFieldNull("list");

    builder = mapper.map(data);
    assertEquals("", "update {[test][test][1234], doc[index {[null][null][null], source[{\"str\":null,\"list\":null,\"int\":null}]}], detect_noop[true]}",
        ElasticsearchChannel.toString(((UpdateRequestBuilder) builder).request()));
    res.add(builder);

    data = SyncDataTestUtil.update();
    data.addField("int", 1381034L).addField("str", "1234").addField("list", Lists.newArrayList(2));

    builder = mapper.map(data);
    assertEquals("", "update {[test][test][1234], doc[index {[null][null][null], source[{\"str\":\"1234\",\"list\":[2],\"int\":1381034}]}], detect_noop[true]}",
        ElasticsearchChannel.toString(((UpdateRequestBuilder) builder).request()));
    res.add(builder);


    data = SyncDataTestUtil.update();
    data.setFieldNull("int").setFieldNull("str").setFieldNull("list");

    builder = mapper.map(data);
    assertEquals("", "update {[test][test][1234], doc[index {[null][null][null], source[{\"str\":null,\"list\":null,\"int\":null}]}], detect_noop[true]}",
        ElasticsearchChannel.toString(((UpdateRequestBuilder) builder).request()));
    res.add(builder);

    return res;
  }

  private static void setFieldNullRemote() throws Exception {
    AbstractClient client = ElasticTestUtil.getDevClient();
    remoteCheck(client, innerSetFieldNull());
  }

  private static void remoteCheck(AbstractClient client, List<Object> builderList) throws ExecutionException, InterruptedException {
    for (Object builder : builderList) {
      BulkRequestBuilder bulkRequestBuilder = null;
      if (builder instanceof IndexRequestBuilder) {
        bulkRequestBuilder = client.prepareBulk().add((IndexRequestBuilder) builder);
      } else if (builder instanceof UpdateRequestBuilder) {
        bulkRequestBuilder = client.prepareBulk().add((UpdateRequestBuilder) builder);
      }  else if (builder instanceof DeleteRequestBuilder) {
        bulkRequestBuilder = client.prepareBulk().add((DeleteRequestBuilder) builder);
      } else {
        fail();
      }
      BulkResponse bulkItemResponses = bulkRequestBuilder.execute().get();
      assertFalse(Arrays.stream(bulkItemResponses.getItems()).anyMatch(BulkItemResponse::isFailed));
    }
  }

  public static void main(String[] args) throws Exception {
//    nestedByIdRemote();
//    mergeToListRemote();
    setFieldNullRemote();
  }
}