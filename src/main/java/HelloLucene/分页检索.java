package HelloLucene;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class 分页检索 {
	// 参考，然并卵
	public static final String[] QUERY_FIELD = { "name", "categoryNameArray",
			"brandName", "description" };
	// 参考，然并卵
	public static final BooleanClause.Occur[] QUERY_FLAGS = {
			BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD,
			BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD };

	public static void main(String[] args) throws IOException, ParseException {
		// 0. Specify the analyzer for tokenizing text.
		// The same analyzer should be used for indexing and searching
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);

		// 1. create the index
		Directory index = new RAMDirectory();

		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40,
				analyzer);

		IndexWriter w = new IndexWriter(index, config);
		addDoc(w, "Lucene in Action", "193398817");
		addDoc(w, "Lucene for Dummies", "55320055Z");
		addDoc(w, "Managing Gigabytes", "55063554A");
		addDoc(w, "The Art of Computer Science", "9900333X");

		w.close();

		// 2. query
		Query q = null;

		String query = "lucene";
		String[] fields = { "title", "isbn" };

		BooleanClause.Occur[] clauses = { BooleanClause.Occur.SHOULD,
				BooleanClause.Occur.SHOULD };
		try {
			q = MultiFieldQueryParser.parse(Version.LUCENE_40, query, fields,
					clauses, analyzer);
		} catch (org.apache.lucene.queryparser.classic.ParseException e) {
			e.printStackTrace();
		}

		// 3. search
		int hitsPerPage = 10;
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(
				hitsPerPage, true);
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		// 4. display results
		System.out.println("Found " + hits.length + " hits.");
		for (int i = 0; i < hits.length; ++i) {
			int docId = hits[i].doc;
			Document d = searcher.doc(docId);
			System.out.println((i + 1) + ". " + d.get("isbn") + "\t"
					+ d.get("title"));
		}

		// 分页(实现)-----start
		int currentpageNum = 2; // 当前页
		int pageSize = 1; // 每页显示多少条记录
		reader = DirectoryReader.open(index);
		searcher = new IndexSearcher(reader);
		collector = TopScoreDocCollector
				.create(currentpageNum * pageSize, true); // 根据当前页和每页多少，查询出结果
		searcher.search(q, collector);
		TopDocs docs = collector.topDocs();
		getResult(searcher, docs, currentpageNum, pageSize); // 分页显示
		// 分页(实现)-----end

		// reader can only be closed when there
		// is no need to access the documents any more.
		reader.close();
	}

	private static void addDoc(IndexWriter w, String title, String isbn)
			throws IOException {
		Document doc = new Document();
		doc.add(new TextField("title", title, Field.Store.YES));

		// use a string field for isbn because we don't want it tokenized
		doc.add(new StringField("isbn", isbn, Field.Store.YES));
		w.addDocument(doc);
	}

	// 分页显示
	public static void getResult(IndexSearcher searcher, TopDocs docs,
			int pageNo, int pageSize) throws IOException {
		ScoreDoc[] hits = docs.scoreDocs;
		int endIndex = pageNo * pageSize;
		int len = hits.length;
		if (endIndex > len) {
			endIndex = len;
		}
		for (int i = (pageNo - 1) * pageSize; i < endIndex; i++) {
			Document d = searcher.doc(hits[i].doc);
			System.out.println("分页如下:");
			System.out.println((i + 1) + ". " + d.get("isbn") + "\t"
					+ d.get("title") + "\t");
			IndexableField[] feilds = d.getFields("details");
			for (IndexableField indexableField : feilds) {
				System.out.println(indexableField.stringValue());
			}
		}
	}

	// 拷贝的例子(参考)，然并卵
	public static Query createQuery(String queryString, Long webId, Long ctgId,
			Date start, Date end, Analyzer analyzer)
			throws org.apache.lucene.queryparser.classic.ParseException {
		BooleanQuery bq = new BooleanQuery();

		if (!StringUtils.isBlank(queryString)) {
			Query q = MultiFieldQueryParser.parse(Version.LUCENE_40,
					queryString, QUERY_FIELD, QUERY_FLAGS, analyzer);
			bq.add(q, BooleanClause.Occur.MUST);
		}

		if (webId != null) {
			Query q = new TermQuery(new Term("websiteId", webId.toString()));
			bq.add(q, BooleanClause.Occur.MUST);
		}
		if (ctgId != null) {
			Query q = new TermQuery(new Term("categoryIdArray",
					ctgId.toString()));
			bq.add(q, BooleanClause.Occur.MUST);
		}
		return bq;
	}

}
