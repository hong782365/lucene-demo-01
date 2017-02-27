package com.lucene.newworld;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * Lucene5.0 版本 创建 IndexWriter, IndexReader 时不再需要指定版本 同时 在底层敢更改了生成索引的方式
 * 如果需要读取之前版本创建的索引 必须引入 lucene-backward-codecs-5.0.0.jar 包
 * <p>
 * Created by derek on 2016/11/2.
 */
public class HelloLucene {

    private static Directory directory;
    private IndexWriter writer;
    private IndexReader reader;

    static {
        try {
            // 读取硬盘上的索引信息
            directory = FSDirectory.open(Paths.get("E://study//lucene"));
            // 读取内存中的索引信息 因为是在内存中 所以不需要指定索引文件夹
            // directory = new RAMDirectory();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public static Directory getDirectory() {
        return directory;
    }

    /**
     * 获取 IndexWriter 对象
     *
     * @author derek
     * @Date 2016/11/2 9:50
     */
    public IndexWriter getWriter(IndexWriterConfig.OpenMode createAppend) {
        if (writer != null) {
            return writer;
        }

        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        if (createAppend == null) {
            // 默认策略为新建索引
            conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        } else {
            conf.setOpenMode(createAppend);
        }

        try {
            writer = new IndexWriter(directory, conf);
            return writer;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取 IndexReader 对象
     *
     * @author derek
     * @Date 2016/11/2 10:32
     */
    public IndexReader getIndexReader() {
        try {
            DirectoryReader newReader = null;
            // 判断 reader 是否为空 若为空就创建一个新的 reader
            if (reader == null) {
                reader = DirectoryReader.open(directory);
            } else {
                // 若不为空 查看索引文件是否发生改变 如果发生改变就重新创建 reader
                newReader = DirectoryReader.openIfChanged((DirectoryReader) reader);
            }
            if (newReader != null) {
                reader = newReader;
            }
            return reader;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取 IndexSearch 对象
     *
     * @author derek
     * @Date 2016/11/2 10:32
     */
    public IndexSearcher getIndexSearch() {
        return new IndexSearcher(getIndexReader());
    }

    /**
     * 创建索引 有几个概念需要理解
     * 1, Directory 类似于数据库中的表
     * 2, Document 类似于数据库中的一条记录
     * 3, Field(域) 类似于数据库中一条记录的某一列
     * 4, 那 Token(词元) 类似于什么？
     * 5, 那 Term(词) 类似于什么？？
     *
     * @author derek
     * @Date 2016/11/2 10:33
     */
    @Test
    public void index() {
        Document document = null;
        writer = getWriter(IndexWriterConfig.OpenMode.CREATE);

        // 设置需要被索引文件的文件夹
        File file = new File("E://study//test");
        // 遍历需要被索引的文件夹
        for (File f : file.listFiles()) {
            document = new Document();
            try {
                /**
                 * 自 Lucene 4 开始 创建 field 对象使用不同的类型 只需要指定是否需要保存源数据 不需要指定分词类型
                 * 之前版本的写法如下：
                 * doc.Add(new Field("id", item.id.ToString(), Field.Store.YES, Field.Index.ANALYZED));
                 */
                Field field = new StringField("fileName", f.getName(), Field.Store.YES);
                document.add(field);
                //Field field2 = new LongField("fileSize", f.length(), Field.Store.NO);
                Field field2 = new LongField("fileSize", f.length(), Field.Store.YES);
                document.add(field2);
                //Field field3 = new LongField("fileLastModified", f.lastModified(), Field.Store.NO);
                Field field3 = new LongField("fileLastModified", f.lastModified(), Field.Store.YES);
                document.add(field3);
                //Field field4 = new TextField("content", new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)));
                Field field4 = new TextField("content", new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)));
                document.add(field4);

                if(writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE){
                    System.out.println("adding " + f);
                    writer.addDocument(document);
                }else{
                    System.out.println("updating " + f);
                    writer.updateDocument(new Term("path", f.toString()), document);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            // 如果不是时常创建索引 一定要记得关闭 writer 当然也可以将 writer 设计成单例的
            if (writer != null){
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    @Test
    public void search(){
        String queryStr = "mycat derek 包裹编号 顺丰单号";
        int num = 100;
        // 默认搜索 content 域 使用标准分词器
        QueryParser parser = new QueryParser("content", new StandardAnalyzer());
        IndexSearcher searcher = getIndexSearch();
        try {
            Query query = parser.parse(queryStr);
            TopDocs docs = searcher.search(query, num);
            System.out.println("一共搜索到结果：" + docs.totalHits + "条");
            for (ScoreDoc scoreDoc : docs.scoreDocs){
                System.out.println("序号为：" + scoreDoc.doc);
                System.out.println("评分为：" + scoreDoc.score);
                Document document = searcher.doc(scoreDoc.doc);
                System.out.println("文件名：" + document.get("fileName"));
                System.out.println("内容为：" + document.get("content"));
                System.out.println("文件大小：" + document.get("fileSize"));
                System.out.println("文件日期：" + document.get("fileLastModified"));
                System.out.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
