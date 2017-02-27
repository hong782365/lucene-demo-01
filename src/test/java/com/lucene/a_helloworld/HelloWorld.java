package com.lucene.a_helloworld;

import com.lucene.domain.Article;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by derek on 2016/11/2.
 */
public class HelloWorld {

    private static Directory directory; // 索引库目录
    private static Analyzer analyzer; // 分词器

    static {
        try {
            directory = FSDirectory.open(new File("./indexDir").toPath());
            analyzer = new StandardAnalyzer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 建立索引
    @Test
    public void testCreateIndex() throws Exception{
        // 准备数据
        Article article = new Article();
        article.setId(1);
        article.setTitle("准备Lucene的开发环境");
        article.setContent("如果信息检索系统在用户发出了检索请求后再去互联网上找答案，根本无法在有限的时间内返回结果。");

        // 放到索引库中
        // 1, 把 Article 转为 Document
        Document doc = new Document();
        Field field = new IntField("id", article.getId(), Field.Store.YES);
        doc.add(field);
        Field field2 = new StringField("title", article.getTitle(), Field.Store.YES);
        doc.add(field2);
        /**
         * 问题二：用 Store.YES ，就可以取出来；用 Store.NO 就取不出来
         * 答： Field.Store.YES或者NO(存储域选项)
         *      设置为YES表示或把这个域中的内容完全存储到文件中，方便进行文本的还原
         *      设置为NO表示把这个域的内容不存储到文件中，但是可以被索引，此时内容无法完全还原(doc.get)
         */
        Field field3 = new TextField("content", article.getContent(), Field.Store.NO);
        doc.add(field3);
//        String idStr = article.getId().toString();
//        doc.add(new Field("id", idStr, Field.Store.YES, Field.Index.ANALYZED));
//        doc.add(new Field("title", article.getTitle(), Field.Store.YES, Field.Index.ANALYZED));
//        doc.add(new Field("content", article.getContent(), Field.Store.NO, Field.Index.ANALYZED));

        // 2, 把 Document 放到索引库中
        IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig(analyzer)); // 默认是添加，不是重建索引，多次执行会有多个；下面的是新建索引，删除以前的重新建，不会重复
        //IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        //conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        //IndexWriter indexWriter = new IndexWriter(directory, conf);
        indexWriter.addDocument(doc);
        indexWriter.close();
    }

    // 搜索
    @Test
    public void testSearch() throws Exception{
        // 准备查询备件
        String queryString = "lucene";
        //String queryString = "开发环境";
        ///String queryString = "互联网";

        // 执行搜索
        List<Article> list =  new ArrayList<Article>();

        // ==================================================================================

        // 1, 把查询字符串转为 Query 对象（默认只从 title 中查询）
        QueryParser queryParser = new QueryParser("title", analyzer); // 问题一： 用 content 就可以搜索到，用 title 就搜索不到
        //QueryParser queryParser = new QueryParser("content", analyzer);
        Query query = queryParser.parse(queryString);

        // 2, 执行查询，得到中间结果
        IndexSearcher indexSearcher = new IndexSearcher(DirectoryReader.open(directory)); // 指定所用的索引库
        TopDocs topDocs = indexSearcher.search(query, 100);// 最多返回前 n 条数据

        int count = topDocs.totalHits;
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;

        // 3, 处理结果
        for (int i = 0; i < scoreDocs.length; i++) {
            ScoreDoc scoreDoc = scoreDocs[i];
            float score = scoreDoc.score;// 相关度得分
            int docId = scoreDoc.doc;// Document 的内部编号

            // 根据编号拿到 Document 数据
            Document doc = indexSearcher.doc(docId);

            // 把Document转为Article
            String idStr = doc.get("id"); //
            String title = doc.get("title");
            String content = doc.get("content"); // 等价于 doc.getField("content").stringValue();

            Article article = new Article();
            article.setId(Integer.parseInt(idStr));
            article.setTitle(title);
            article.setContent(content);

            list.add(article);
        }

        // ==========================================================================================

        // 显示结果
        System.out.println("总结果数：" + list.size());
        for (Article a : list) {
            System.out.println("------------------------------");
            System.out.println("id = " + a.getId());
            System.out.println("title = " + a.getTitle());
            System.out.println("content = " + a.getContent());
        }
    }
}
