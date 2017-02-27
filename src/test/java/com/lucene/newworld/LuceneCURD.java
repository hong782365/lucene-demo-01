package com.lucene.newworld;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by derek on 2016/11/2.
 */
public class LuceneCURD {

    //测试数据，模拟数据库表结构
    private static String[] ids={"1","2","3"}; //用户ID
    private static String [] titles={"kl","wn","sb"};
    private static String [] contents={"shi yi ge mei nan zi test","Don't know test","Is an idiot\n"};
    //索引存储地址
    private static String indexDir="E:\\study\\LuceneCURD";



    private static Directory directory;
    private static Analyzer analyzer;

    private static IndexWriter indexWriter;
    private static IndexReader indexReader;

    static {
        try {
            directory = FSDirectory.open(Paths.get(indexDir));
            analyzer = new StandardAnalyzer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取 IndexWriter 对象
     * @author derek
     * @Date 2016/11/2 16:04
     */
    public static IndexWriter getIndexWriter(IndexWriterConfig.OpenMode createAppend) throws Exception{
        if(indexWriter == null) {
            IndexWriterConfig conf = new IndexWriterConfig(analyzer);
            if (createAppend == null) {
                // 默认策略为新建索引
                conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            } else {
                conf.setOpenMode(createAppend);
            }
            indexWriter = new IndexWriter(directory, conf);
        }
        return indexWriter;
    }

    /**
     * 获取 IndexReader 对象
     * @author derek
     * @Date 2016/11/2 16:09
     */
    public static IndexReader getIndexReader() throws Exception{
        DirectoryReader newReader = null;
        if(indexReader == null){
            indexReader = DirectoryReader.open(directory);
        }else{
            // 若不为空 查看索引文件是否发生改变 如果发生改变就重新创建 reader
            newReader = DirectoryReader.openIfChanged((DirectoryReader) indexReader);
        }
        if(newReader != null) indexReader = newReader;
        return indexReader;
    }

    /**
     * 获取 IndexSearch 对象
     * @author derek
     * @Date 2016/11/2 16:10
     */
    public static IndexSearcher getIndexSearch() throws Exception{
        return new IndexSearcher(getIndexReader());
    }

    /**
     * 初始化数据
     * @author derek
     * @Date 2016/11/2 16:11
     */
    @Test
    public void testInit() throws Exception{
        indexWriter = getIndexWriter(IndexWriterConfig.OpenMode.CREATE);
        Document doc = null;
        for (int i = 0; i < ids.length; i++) {
            doc = new Document();
            doc.add(new StringField("id", ids[i], Field.Store.YES));
            doc.add(new StringField("title", titles[i], Field.Store.YES));
            doc.add(new TextField("content", contents[i], Field.Store.YES));

            if(indexWriter.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE){
                indexWriter.addDocument(doc);
            }else{
                //indexWriter.updateDocument(new Term(), doc);
            }
        }
        if(indexWriter != null) indexWriter.close();
    }
    
    /**
     * 搜索
     * @author derek
     * @Date 2016/11/2 20:22
     */
    @Test
    public void testSearch() throws Exception{
        String queryStr = "test";
        int num = 100;
        QueryParser queryParser = new QueryParser("content", analyzer);
        IndexSearcher indexSearcher = getIndexSearch();

        Query query = queryParser.parse(queryStr);
        TopDocs docs = indexSearcher.search(query, num);
        System.out.println("一共搜索到结果：" + docs.totalHits + "条\n");
        for (ScoreDoc scoreDoc : docs.scoreDocs) {
            System.out.println("序号为：" + scoreDoc.doc);
            System.out.println("评分为：" + scoreDoc.score);
            Document doc = indexSearcher.doc(scoreDoc.doc);
            System.out.println(doc.get("title") + "[" + doc.get("content") + "]-->" + doc.get("id"));
            System.out.println();
        }
    }
}
