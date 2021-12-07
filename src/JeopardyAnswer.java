package src;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/*
 * Author: Isabel Campos
 * CSC 583, Term Project
 * December Fall 2021
 * 
 * Objective: create a index that best answers Jeopardy questions, idea being
 * that the answers to the questions are titles of wikipedia pages
 * provided in wiki-subset folder.
 * Computes error analysis and measures performance based on the ranking of the answers
 * */

public class JeopardyAnswer {
	
/*Measure performance*/
	HashMap<Integer, Double> mrr = new HashMap<Integer, Double>();

/*Error analysis*/	
	double good ;
	double bad;
	
	
/*INDEX portion*/
	 static StandardAnalyzer lyzer = new StandardAnalyzer();
	 static EnglishAnalyzer stem = new EnglishAnalyzer();
	 static Directory index ;
	 
	 /*Constructor: opens index directory and creates the index if necessary*/
	public JeopardyAnswer(String name, Analyzer type, boolean changeSim, boolean lemma) {
		//classify data (wiki pages)
		File f = new File(name);
    	boolean check = Files.exists(f.toPath());
    	try {
			index = FSDirectory.open(f.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (check) {
			return;
		}
		classifyPages(type, changeSim, lemma);
    	
		
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
	        File file = new File(classLoader.getResource("src/wiki-subset").getFile());
	        File[] allText = file.listFiles();
	        int len  = allText.length;
	        for ( int i = 0; i < len; i++) {
	        	if (allText[i].isFile()) {
	        		readFile(allText[i], w, lemma, type);
	        	}// if a folder contains files that don't give us anything
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
			BufferedReader sc = new BufferedReader(new FileReader(info));
			String title = "";
			String text = "";
			String read;
			String find = "[[";
			String before;
			while ((read = sc.readLine()) != null) {
	
				if (read.length()<2) {
					//ignore just blank 
				}else if ( find.equals(read.substring(0,2))) {
					if ( !title.contentEquals("")) {		
						wikiPage(title, text, w, lemma, type);
						text = "";
					}
					
					title = read.substring(2, read.length());
					title = title.replace("]", "");
				}else {
					before = cleanInput(read.trim());
					text += before + " ";
				}
			}
			if (!title.equals("")) {
				wikiPage(title, text, w, lemma, type);
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
		
		if (lemma) {
			text = lemmatizeQuery(text, type );
		}
		
    	Document doc = new Document();
    	doc.add(new StringField("title", title, Field.Store.YES));
    	doc.add(new TextField("toks", text, Field.Store.NO));
    	w.addDocument(doc);	
	}
	
	/*Cleans the input for the index removing some words that don't help towards searching*/
	public String cleanInput(String given) {
		String clean = "";
		given = given.toLowerCase();
		if (given.contains("==")) {
			given = given.replaceAll("=", "");
			if (given.contains("see also") || given.contains("notes")|| given.contains("external links")||
					given.contains("further reading") ||given.contains("references")|| given.contains("other")) {
				
				return "";
			}
			clean = given;
			return clean;
		}
		clean = given;
		clean = clean.replace("(", "");
		clean = clean.replace(")", "");
		clean = clean.replace("-", " ");
		clean = clean.replace("'", "");
		clean = clean.replace(",", "");
		clean = clean.replace("|", " ");
		clean = clean.replace(";", "");
		clean = clean.replace(":", "");
		clean = clean.replace("[", "");
		clean = clean.replace("]", "");
		clean = clean.replace("*", "");
		clean = clean.replace("/", "");
		clean = clean.replace("\"","");
		clean = clean.replace(".","");
		return clean;
	}
	
	
	
	
/*Testing an execution*/
	public static void main(String[] args) {
		/*
		System.out.println("Second best (no lemmas) yes stemming");
		JeopardyAnswer jp = new JeopardyAnswer("english.txt", stem, false, false);//false no score change, false no lemma
		System.out.println("\tDoes re ranking");
		jp.readQuestions(stem, false, false, true);
		double check2 = jp.errorA(null, null, true);
		double mrr = jp.measurePerformance();
		System.out.println("\tPercent correct "+check2*100 + " and performance "+mrr+"\n");
		*/
		System.out.println("Best version: English analyzer (stems) and lemmatization of index and queries");
		//Best one stems, lemmatizes
		JeopardyAnswer ja = new JeopardyAnswer("engLemma.txt", stem, false, true);
		System.out.println("Answers questions without re-ranking");
		ja.readQuestions(stem, false, true, false);
		double check =ja.errorA(null, null, true);
		double mr = ja.measurePerformance();
		System.out.println("Error analysis "+check*100+" Performance "+mr+" \n");
		System.out.println("Answers question with re-ranking");
		ja.readQuestions(stem, false, true, true);
		check = ja.errorA(null, null, true);
		mr = ja.measurePerformance();
		System.out.println("\tPercent correct "+check*100 + " and  performance "+mr);
	
		
		System.out.println("\nBest with the scoring function changed");
		//change scoring function
		JeopardyAnswer difSF = new JeopardyAnswer("bestDiff.txt", stem, true, true);
		System.out.println("\tNO re ranking");
		difSF.readQuestions(stem, true, true, false);
		double save = difSF.errorA(null, null,true);
		double m = difSF.measurePerformance();
		System.out.println("\t"+save*100+" percent correct and performance "+m);
		
	}
	
	
	
	
/*QUESTION handling*/	
	/*Opens text file with questions to answer
	 * Calls findingAnswer passing in the query info
	 */
	public void readQuestions(Analyzer type, boolean change, boolean lemma, boolean option) {
		good = 0;
		bad= 0;
		try {
			String cat;
			String clue ;
			String clue1;
			String given;
			int ques = 0;
			Scanner sc =new Scanner(new FileInputStream("src/questions.txt"));
			while (sc.hasNextLine()) {
				ques += 1;
				cat = sc.nextLine();
				cat = cat.trim();
				clue = sc.nextLine();
				given = sc.nextLine();
				given = given.trim();
				clue= clue.trim();
				clue1 = splitClue(clue,false);
				cat = cleanQuery(cat);
				clue1 = cleanQuery(clue1);
				clue = cat + " "+ clue1;
				if (lemma) {
					clue = lemmatizeQuery(clue, type);
					
				}
				
				findingAnswer(clue, type, change,  ques, given, option);
				
				sc.nextLine();// to avoid new line 
			}
			sc.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	
	/*
	 * cleans the given query string by removing characters*/
	public String cleanQuery(String given) {
		String clean = "";
		clean = given.replaceAll("!", "");
		clean = clean.toLowerCase();
		clean = clean.replaceAll("-", " ");
		clean = clean.replaceAll(":", " ");
		clean = clean.replaceAll("\n", "");
		clean = clean.replaceAll("\"", "");
		clean = clean.replaceAll("&", "");
		clean = clean.replaceAll(",", "");
		clean = clean.replaceAll("'", "");
		
		return clean;
	}
	
	/*
	 * Takes in the clue and based on boolean splits the clue in "half"
	 * based on the comma or semicolon character
	 * Also checks if the clue is "short" and if so only returns the first part
	 * */
	public String splitClue(String given, boolean half) {
	
		String split = given;
		int index =0;
		if (split.contains(",") ) {
			index = split.indexOf(",");
		}else if (split.contains(";")) {
			index = split.indexOf(";");
			
		}
		
		if (half) { //first half
			split = split.substring(0, index);
		}else {// second half
			split = split.substring(index+1, split.length());
		}
		return split;
	}
	
	//reorder top 2/3 results by looking at their score differences
	//return the "new" ranking
	public ScoreDoc[] reOrderResults(ScoreDoc[] results, IndexSearcher search, Analyzer type) {
		
	
			float first = results[0].score;
			float second = results[1].score;
			float diff = first -second;
			ScoreDoc s1 = results[0];
			ScoreDoc s2 = results[1];
			ScoreDoc s3 = results[2];
			
			if (first < 45.0) {// leave it alone if first score is above 45
				
				if ((first >40.0) && (diff< 10.0)&&(diff >1.9)) {//allows for up to 9.* difference
					//switch top two
					
					results[0] = s2;
					results[1] = s1;
					
				}else if ((first < 34.0) && (diff > 3.3 )&& (diff <5.0)) {
					
					//switch two
					results[0] = s2;
					results[1] = s1;
				}else if((first < 30.0) && (first >20.0)){
					
					
					if (((diff > 1.0) && (diff < 2.0))) {
						//switch top two
						
						results[0] = s2;
						results[1] = s1;
					}else if ((first > 25.0) &&(diff >3)) {
						//switch top 3
					
						results[0] = s3;
						results[1] = s1;
						results[2] = s2;
						
						
					}else if ((first <26.0) && ((diff >3) ||(diff < 1))){
						//switch top
				
						results[0] = s2;
						results[1] = s1;
					}
				}else if ((first<40.0) &&(first >30.0)&&(diff >1.3)&&(diff <2.6)) {//between 40.0 and 30.0
					//switch 2
				
					results[0]= s2;
					results[1] = s1;
					
				}
				
				
			}
			
			
			return results;
		
		
	}
	
	/*Takes in the top 100 results and searches through them for the position/rank
	 * of the correct answer and saves it in hashmap mrr mapped to the question number*/
	public void addToMeasure(ScoreDoc[] results, int question, IndexSearcher s, String given) {
		
		double topR;
		try {
			String saveName ="";
			int len = results.length;
			double found = -1;
			for (int i = 0; i < len; i++) {
				saveName= s.doc(results[i].doc).get("title");
				if (given.contains(saveName) && given.contains("|")) {
						String first = given.substring(0, given.indexOf('|'));
						String sec = given.substring(given.indexOf('|')+1, given.length());
						if (first.equals(saveName)|| sec.equals(saveName)) {
							found = i;
							break;
						}
						
				}else if (given.equals(saveName)) {
					found = i;
					break;
				}
			}
			topR = found+1.0;
			
			mrr.put(question, topR);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Measures performance by mean reciprocal rank
	 * Looks at top 100 ranks for each questions and has the rank position saved
	 * in mrr HashMap (this is done in addTomeasuere method as it checks where the 
	 * correct answer is ranked)
	 * Takes the inverse of the rank, adds up each and divides by number of questions
	 * Counts how many fall within top 2, 10 and 100
	 * */
	public double measurePerformance() {
		// MRR
		double result = 0.0;
		double ques = 100;
		int count = 0;
		double num;
		double temp;
		double count2 = 0;
		double count3 = 0;
		for (int each : mrr.keySet()) {
		
			num = mrr.get(each);
			if (num != 0) {
			
				temp = 1/num;
				if (num < 11) {
					count +=1;
				}
				if (num < 3) {
				count2 +=1;	
					
				}
				if (num < 101) {
					
					count3 +=1;
				}
			}else {
				temp = 1/100;
			}
		
			result += temp;
			
		}
		System.out.println("Correct answers that fall within: top 10->"+count  + " \ttop 2->"+count2 +" \ttop 100 ->"+count3 );
		
		
		result = result /ques;
		
		return result;
	}
	
	/*counts the number of correctly/incorrectly answered questions
	 * based on boolean variable done, calculates the percentage
	 * of accurate answers and return number*/
	public double errorA(String answer, String found, boolean done) {
		// correct versus incorrect answers
		int val =0;
		if (!done) {
			
			if (answer.equals(found)) {
				good +=1;
			
			}else if (answer.contains("|")){
				String first  = answer.substring(0, answer.indexOf('|'));
				String sec = answer.substring(answer.indexOf('|')+1, answer.length());
				if (first.equals(found) || sec.equals(found)) {
					
					good+=1;
				}else {
					bad +=1;
					val = -1;
				}
			}else {
				bad +=1;
				val = -1;
			}
		}
		double per = 0;
		if (done) {
			per = good/(good+ bad);
			
		}else {
			per = val;
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
			e.printStackTrace();
		}
	
		
		return temp;
	}
	
	
	/*Opens the given index and uses the analyzer indicated
	 * changes scoring function if needed based on variable change
	 * variable option determines if we'll do re-ranking*/
	public void  findingAnswer(String query, Analyzer type, boolean change, int ques, String ans, boolean option) throws IOException  {
		IndexReader reader= DirectoryReader.open(index);
    	IndexSearcher search = new IndexSearcher(reader);
    	if (change) {
    		search.setSimilarity(new ClassicSimilarity());
    	}
    	int hits = 100;
    	Query q;
		try {
			
			q = new QueryParser("toks", type).parse(query);
			TopDocs doc = search.search(q, hits);
			ScoreDoc[] scores = doc.scoreDocs;
			
			if (option) {
				scores = reOrderResults(scores, search, type );
			}
			String found = search.doc(scores[0].doc).get("title");
			errorA(ans, found, false);
			addToMeasure(scores, ques, search, ans);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
	}

}
