package src;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


public class JeopardyAnswer {
	
/*Measure preformance*/
	HashMap<Integer, ArrayList<Integer>> j1 = new HashMap<Integer, ArrayList<Integer>>();
	HashMap<Integer, ArrayList<Integer>> j2 = new HashMap<Integer, ArrayList<Integer>>();
	HashMap<Integer, Double> kvalues = new HashMap<Integer, Double>();
	int total= 0;

/*Error analysis*/	
	double good ;
	double bad;
	
	
/*INDEX portion*/
	 static StandardAnalyzer lyzer = new StandardAnalyzer();
	 static EnglishAnalyzer stem = new EnglishAnalyzer();
	   
	 static Directory index ;
	
	 /*Constructor: opens index directory and creates the index if necessary*/
	public JeopardyAnswer(String directoryName, Analyzer type, boolean changeSim, boolean lemma) {
		//classify data (wiki pages)
    	judgeSetUp(directoryName, type, changeSim, lemma);
		
	}
	
	/*Calls to open another directory, if needed sets it up*/
	public void judgeSetUp(String name, Analyzer type, boolean changeSim, boolean lemma) {
		boolean check = changeIndex(name);
		if (check) {
			return;
		}
		classifyPages(type, changeSim, lemma);
	}
	
	/*opens another directory setting the variable index to the one asked for
	 * return boolean for if the index already exists
	 * */
	public boolean changeIndex(String name) {
		total = 0;
		//add option to delete file to reset....?
		File f = new File(name);
    	boolean check = Files.exists(f.toPath());
    	try {
			index = FSDirectory.open(f.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return check;
	}
	
	/*
	 * Opens wiki-pages directory and opens each text file, calls readfile to process
	 * Passes in the analyzer we're using (Standard or English)
	 * Boolean variable change indicates if the scoring function should be changed
	 * Boolean variable lemma indicates if lemmatization should be done
	 * */
	public void classifyPages(Analyzer type, boolean change, boolean lemma) {
		 
        IndexWriterConfig config = new IndexWriterConfig(type);
        if (change ) {
        	config.setSimilarity(new ClassicSimilarity());
        }
        IndexWriter w;
        try {
        	w = new IndexWriter(index, config);
		
		 ClassLoader classLoader = getClass().getClassLoader();
	        File file = new File(classLoader.getResource("src.resources/wiki-subset").getFile());
	        File[] allText = file.listFiles();
	        int len  = allText.length;
	        for ( int i = 0; i < len; i++) {
	        	if (allText[i].isFile()) {
	        		readFile(allText[i], w, lemma, type);
	        	}else {// folder with more files
	        		File[] more = allText[i].listFiles();
	        		for ( int j = 0; j < more.length; j++) {
	        			readFile(more[j], w, lemma, type);
	        			
	        		}
	        	
	        	}
	        }
	        w.close();
	        
        } catch (IOException e) {
			e.printStackTrace();
		}
	
	}
	
	
	/*
	 * Takes in a file, opens it to find each wiki page by looking for the title of each page
	 * encased in double brackets the rest is concatinated as the text of the wiki page
	 * Calls wikipage function to create a document with the infomation and add to index
	 * */
	public void readFile(File info, IndexWriter w, boolean lemma, Analyzer type) throws IOException {
		// title is first, encased in double brackets
		try {
			Scanner sc = new Scanner(info);
			String title = "";
			String text = "";
			String read;
			String find = "[[";
			while (sc.hasNext()) {
				read = sc.next();
				if ( find.equals(read.substring(0,2))) {
					if ( !title.contentEquals("")) {
						wikiPage(title, text, w, lemma, type);
						text = "";
					}
					
					title = read.substring(2, read.length());
					title = title.replace("]", "");
				}else {
					text = read + " ";
				}
				
			}
			
			
			sc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	
	}
	
	/*
	 * Creates a Document and adds the given title and text of a wiki page
	 * lemmatizes text based on lemma variable
	 * */
	public void wikiPage(String title, String text, IndexWriter w, boolean lemma, Analyzer type) throws IOException {
		total += 0;
		if (lemma) {
			text = lemmatizeQuery(text, type );
		}
		
		
    	Document doc = new Document();
    	doc.add(new StringField("title", title, Field.Store.YES));
    	doc.add(new TextField("toks", text, Field.Store.YES));
    	w.addDocument(doc);	
	
	}
	
/*Testing an execution*/
	public static void main(String[] args) {
		// lyzer is the StandardAnalyzer --> no stemming
		// false for changing the scoring function and false for lemmatizing
		JeopardyAnswer ja = new JeopardyAnswer("standard.txt", lyzer, false , false);
		// constructor builds the index if needed
		ja.readQuestions(lyzer, false, false, true); // true is for judge 1
		double percent = ja.errorA(null, null, true); // true says return percent correctly answered
		// first two parameters only relevant when we have the answers (ours and given)
		System.out.println("Percent correct for judge1, "+percent);
		
		//set up another index, second judge
		ja.judgeSetUp("english.txt", stem, false, false);//false no score change, false no lemma
		ja.readQuestions(stem, false, false, false);// false for judge2
		double check2 = ja.errorA(null, null, true);
		System.out.println("percent correct for judge2, "+check2);
		
		// measuring performance given two judges...
		int kCount = ja.measurePerformance();
		System.out.println("the number of questions that have a k value greater than 2/3 out of 100 Qs, "+kCount);
		
		
	}
	
	
/*QUESTION handling*/	
	/*Opens text file with questions to answer
	 * Calls findingAnswer passing in the query info
	 */
	public void readQuestions(Analyzer type, boolean change, boolean lemma, boolean judge) {
		good = 0;
		bad= 0;
		//ClassLoader classLoader = getClass().getClassLoader();
	      //File file = new File(classLoader.getResource("questions.txt").getPath());
		try {
			String cat;
			String clue ;
			String given;
			String answer;
			int ques = 0;
			Scanner sc =new Scanner(new FileInputStream("src/questions.txt"));
			while (sc.hasNextLine()) {
				ques += 1;
				cat = sc.nextLine();
				clue = sc.nextLine();
				given = sc.nextLine();
				//lemmatize clue....add category ?
				if (lemma) {
					clue = lemmatizeQuery(clue, type);
				}
				answer = findingAnswer(clue, type, change, judge, ques);
				errorA(given, answer, false);
				sc.nextLine();// to avoid new line 
			}
			sc.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	public void addToJudge(boolean judge, ScoreDoc[] results, int question) {
		ArrayList<Integer> relevant = new ArrayList<Integer>();
		int len = results.length;
		for (int i = 0; i < len; i++) {
			if (results[i].score >= 0.5) {
				relevant.add(results[i].doc);
			}else {
				System.out.print(i + ", ");
				break;
			}
		}
		
		if (judge) {
			j1.put(question, relevant);
		}else {
			j2.put(question, relevant);
		}
		
		
	}
	
	/*
	 * Uses arraylist of relevant documents from two judges to compute kappa measure
	 * out of 100 questions, counts how many produce a k value greater than 2/3
	 * counts the agreement (yes and no), disagreement (no1 and no2) 
	 * total num of wiki pages was counted when creating the index
	 * Returns count of accepted k values ( how many questions were handled well, relevance)
	 * */
	public int measurePerformance() {
		// NDCG
		// computes kappa measure using judge 1 (j1) and judge 2 (j2)
		int count = 0;
		ArrayList<Integer> first;
		ArrayList<Integer> second ;
		double yes;
		double no ;
		double no1; // judge 1 said no 
		double no2 ; // judge 2 said no
		double flen;
		double slen;
		double PA;
		double PE;
		double k;
		for (int each : j1.keySet()) {// should be the same as j2 keyset
			
			yes = 0;
			no = 0;
			no1= 0;
			 no2= 0;
			 
			if (j1.get(each).size()> j1.get(each).size()) {
				first = j1.get(each);
				
				second = j2.get(each);
			}else {
				first = j2.get(each);
				second = j1.get(each);
			}
			flen = first.size();
			slen = second.size();
			
			for (int y = 0; y < flen; y++) {
				if ( second.contains(first.get(y))) {
					yes +=1;
				}else {
					no2 +=1;// judge 1 says yes judge 2 said no
				}
			}
			no1 = slen -yes;// judge1 did not ask so didn't say yes, judge 2 said yes
			no = total - (yes+no1 + no2);
			PA = (yes+no)/total;
			PE = (((yes + no2)/total )* ((yes + no1)/total));
			PE += (((no +no2)/total) *((no + no1)/total));
			
			k = PA -PE;
			k = k /(1-PE);
			kvalues.put(each, k);
			if (k > (2/3)) {
				count +=1;
			}
		}
		
		return count;
	}
	
	/*counts the number of correctly/incorrectly answered questions
	 * based on boolean variable done, calculates the percentage
	 * of accurate answers and return number*/
	public double errorA(String answer, String found, boolean done) {
		// correct versus incorrect answers
		if (!done) {
			if (answer.equals(found)) {
				good +=1;
			
			}else {
				bad +=1;
			}
		}
		double per = 0;
		if (done) {
			per = good/(good+ bad);
			
		}
		return per;
	
	}
	
	/*
	 * Takes the given text and lemmatizes it returns a string of concatenated lemmas
	 * */
	public String lemmatizeQuery(String text, Analyzer type)  {
		String temp = "";
		TokenStream stream = type.tokenStream("toks", text);
		
		try {
			stream.reset();//?
			while (stream.incrementToken()) {
				temp += stream.getAttribute(CharTermAttribute.class).toString();
				temp += " ";
				
			}
			stream.end();
			stream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		
		return temp;
	}
	
	
	/*Opens index and changes scoring function if needed*/
	public String findingAnswer(String query, Analyzer type, boolean change, boolean judge, int ques) throws IOException  {
		IndexReader reader= DirectoryReader.open(index);
    	IndexSearcher search = new IndexSearcher(reader);
    	if (change) {
    		search.setSimilarity(new ClassicSimilarity());
    	}
    	int hits = 1000; // only want the one with the highest score
    	Query q;
		try {
			q = new QueryParser("toks", type).parse(query);
			TopDocs doc = search.search(q, hits);
			ScoreDoc[] scores = doc.scoreDocs;
			addToJudge(judge, scores, ques);
			return search.doc(scores[0].doc).get("title");
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return "";
		
	}

}
