package entailment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.netlib.lapack.Sspcon;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.ibm.icu.util.StringTokenizer;

import ac.biu.nlp.normalization.BiuNormalizer;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import entailment.entityLinking.SimpleSpot;
import entailment.stringUtils.RelationString;
import entailment.vector.EntailGraphFactoryAggregator;
import entailment.vector.EntailGraphFactoryAggregator.TypeScheme;

public class Util {

	public static StanfordCoreNLP stanPipeline;
	public static StanfordCoreNLP stanPipelineSimple;// up to lemma
	public static StanfordCoreNLP stanPipelineSimple2;// up to ssplit
	static BiuNormalizer biuNormalizer;
	static String defaultEntTypesFName = "entTypes.txt";
	static String defaultGenTypesFName = "genTypes.txt";
	static String defaultEntToWikiFName = "entToWiki.txt";
	static String defaultEntToFigerType = "freebase_types/entity2Types.txt";
	static Map<String, String> stan2Figer;
	public static Map<String, String> entToType = null;
	public static Map<String, String> genToType = null;
	// public static Map<String, String> entToWiki = null;
	private static Map<String, String> entToFigerType = null;
	private static Map<String, Boolean> entToFigerONLYNE = null;

	static HashMap<String, String[]> predToLemma = new HashMap<>();;

	static HashSet<String> modals;
	static HashSet<String> prepositions;// I need a predefined list when extending the predArg extraction
	static Logger logger;

	static {

		stan2Figer = new HashMap<>();
		String[] stans = new String[] { "location", "organization", "date", "number", "person", "misc", "time",
				"ordinal", "o" };
		String[] figers = new String[] { "location", "organization", "time", "thing", "person", "thing", "time",
				"thing", "thing" };

		for (int i = 0; i < stans.length; i++) {
			stan2Figer.put(stans[i], figers[i]);
		}

		loadEntGenTypes(defaultEntTypesFName, defaultGenTypesFName);
		// try {
		// loadEntToWiki(0);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization,
		// NER, parsing, and coreference resolution
		Properties props = new Properties();
		// props.put("annotators",
		// "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		props.put("annotators", "tokenize,ssplit,pos,lemma,ner");
		// logger = Logger.getLogger(StanfordCoreNLP.class);
		// logger.setLevel(Level.OFF);
		// System.out.println("here111");
		stanPipeline = new StanfordCoreNLP(props);

		Properties props2 = new Properties();
		// props.put("annotators",
		// "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		props2.put("annotators", "tokenize,ssplit,pos,lemma");
		// logger = Logger.getLogger(StanfordCoreNLP.class);
		// logger.setLevel(Level.OFF);
		// System.out.println("here111");
		stanPipelineSimple = new StanfordCoreNLP(props2);

		Properties props3 = new Properties();
		// props.put("annotators",
		// "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		props3.put("annotators", "tokenize,ssplit");
		// logger = Logger.getLogger(StanfordCoreNLP.class);
		// logger.setLevel(Level.OFF);
		// System.out.println("here111");
		stanPipelineSimple2 = new StanfordCoreNLP(props3);

		modals = new HashSet<String>();
		String[] modalsList = new String[] { "can", "could", "may", "might", "must", "shall", "should", "will", "would",
				"ought" };
		for (String s : modalsList) {
			modals.add(s);
		}

		try {
			Scanner sc = new Scanner(new File("prepositions.txt"));
			prepositions = new HashSet<>();
			while (sc.hasNext()) {
				prepositions.add(sc.nextLine().toLowerCase());
			}
			sc.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		try {
			biuNormalizer = new BiuNormalizer(new File("lib_data/biu_string_rules.txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Map<String, String> getEntToFigerType() {
		if (entToFigerType == null) {
			try {
				loadFigerTypes(defaultEntToFigerType);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return entToFigerType;
	}

	// static String normalize1(String s) {
	// String ret = StringUtils.normalizeSpace(s);
	// ret = StringUtils.stripAccents(ret);
	// ret = Normalizer.normalize(ret, Normalizer.Form.NFD);
	// ret = ret.replaceAll("[^\\x00-\\x7F]", "");
	// return ret;
	// }

	// // To parse results of stanford parser...
	// static void convertToPArgFormat(String[] args) throws IOException {
	// String fname;
	// boolean shouldLink;
	// if (args != null && args.length > 0) {
	// fname = args[0];
	// shouldLink = Boolean.parseBoolean(args[1]);
	// } else {
	// fname = "stan.txt";
	// shouldLink = false;
	// }
	//
	// BufferedReader br = new BufferedReader(new InputStreamReader(new
	// FileInputStream(fname)));
	// JsonParser jsonParser = new JsonParser();
	// String line;
	// while ((line = br.readLine()) != null) {
	// if (line.equals("")) {
	// continue;
	// }
	// try {
	// JsonObject jo = jsonParser.parse(line).getAsJsonObject();
	// int i = 0;
	// String docS = "";
	// String articleId = jo.get("articleId").getAsString();
	// String date = jo.get("date").getAsString();
	//
	// JsonObject jObj = new JsonObject();
	//
	// JsonArray rels = new JsonArray();
	//
	// ArrayList<String> curPrArgs = new ArrayList<>();
	//
	// while (jo.has("" + i)) {
	// JsonArray jai = jo.get("" + i).getAsJsonArray();
	// String s = jai.get(0).getAsJsonObject().get("s").toString();
	//
	// // I must do POS tagging as well for GorE
	//
	// s = s.substring(1, s.length() - 1);
	// docS += s + " ";
	// // System.out.println("#line: " + s);
	// HashMap<String, String> posTags = new HashMap<>();
	// if (shouldLink) {
	// posTags = getAllPOSTags(s);
	// }
	// // for (String t:posTags.keySet()){
	// // System.out.println(t+" "+posTags.get(t));
	// // }
	//
	// for (int j = 1; j < jai.size(); j++) {
	// String r = jai.get(j).getAsJsonObject().get("r").toString();
	// r = r.substring(2, r.length() - 2);
	//
	// String[] parts = r.split(",");
	// boolean isGen[] = new boolean[2];
	//
	// for (int k = 0; k < parts.length; k++) {
	// String p = parts[k].trim();
	// p = simpleNormalize(p);
	//
	// if (k == 0) {
	// isGen[0] = isGeneric(p, posTags);
	// if (shouldLink && !isGen[0]) {
	// p = entToWiki.containsKey(p) ? entToWiki.get(p) : p;
	// }
	// } else if (k == 2) {
	// isGen[1] = isGeneric(p, posTags);
	// if (shouldLink && !isGen[1]) {
	// p = entToWiki.containsKey(p) ? entToWiki.get(p) : p;
	// }
	// }
	//
	// parts[k] = p;
	// }
	//
	// String GorE = (isGen[0] ? "G" : "E") + (isGen[1] ? "G" : "E");
	//
	// parts[1] = parts[1].replace(" ", "_");
	//
	// String prArg = "(" + parts[1] + "::" + parts[0] + "::" + parts[2] + "::" +
	// GorE + ")";
	// curPrArgs.add(prArg);
	//
	// // System.out.println(rs);
	// }
	// // System.out.println();
	// i++;
	// }
	// docS = docS.trim();
	// jObj.addProperty("s", docS);
	// jObj.addProperty("date", date);
	// jObj.addProperty("articleId", articleId);
	// // jObj.addProperty("lineId", lineId);
	//
	// for (int j = 0; j < curPrArgs.size(); j++) {
	// JsonObject rel = new JsonObject();
	// rel.addProperty("r", curPrArgs.get(j));
	// rels.add(rel);
	// }
	// jObj.add("rels", rels);
	//
	// System.out.println(jObj);
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	//
	// // JsonArray ja = jsonParser.parse(line).getAsJsonArray();
	// // for (int i=0; i<ja.size(); i++){
	// // System.out.println(ja.get(i));
	// // }
	//
	// }
	// br.close();
	// }

	public static String normalizeArg(String arg) {

		// number normalization
		try {
			arg = biuNormalizer.normalize(arg);
		} catch (Exception e) {
			// e.printStackTrace();
		}

		String[] ss = getPOSTaggedTokens(arg);
		String[] tokensList = ss[0].split(" ");
		String[] posList = ss[1].split(" ");
		arg = "";
		for (int i = 0; i < tokensList.length; i++) {
			if (posList[i].equals("DT")) {
				arg += "det ";
			} else if (posList[i].startsWith("PRP")) {
				arg += "pronoun ";
			} else {
				arg += tokensList[i] + " ";
			}
		}

		return arg.trim();

	}

	// (visited.As.2,visited.during.2) => [(visit.as.2,visit.during.2),False]
	// the second arg shows whether the name has been reversed
	public static String[] getPredicateLemma(String pred, boolean isCCG) {

		String[] ret = new String[2];
		if (!EntailGraphFactoryAggregator.lemmatizePredicate) {
			ret[0] = pred;
			ret[1] = "false";
			return ret;
		}

		String pred0 = pred;
		if (predToLemma.containsKey(pred)) {
			return predToLemma.get(pred);
		}

		if (!isCCG) {
			pred = pred.replace("_", " ");
			try {
				pred = RelationString.normalizePredicate(pred);
			} catch (Exception e) {

			}
			pred = getLemma(pred);
			pred = pred.replace(" ", "_");
			ret[0] = pred;
			ret[1] = "false";
			predToLemma.put(pred0, ret);
			return ret;
		}
		String prePred = "";
		if (pred.contains("__")) {
			int mainPredIdx = pred.indexOf("__") + 2;
			prePred = pred.substring(0, mainPredIdx);
			pred = pred.substring(mainPredIdx);
		}
		if (!pred.startsWith("(") || !pred.endsWith(")") || !pred.contains(",")) {
			ret[0] = pred;
			ret[1] = "false";
			predToLemma.put(pred0, ret);
			return ret;
		}
		pred = pred.substring(1, pred.length() - 1);
		pred = pred.toLowerCase();
		// String[] parts = pred.split(",");
		String[] parts = StringUtils.split(pred, ",");

		ArrayList<String> myParts = new ArrayList<String>();
		for (String part : parts) {

			myParts.add(getPartLemm(part));

		}
		String firstPart = myParts.get(0);
		Collections.sort(myParts);

		StringBuilder sb = new StringBuilder("(");

		// String retStr = "(";
		int ii = 0;
		for (String part : myParts) {
			if (ii < parts.length - 1) {
				sb.append(part + ",");
			} else {
				sb.append(part);
			}
			ii++;
		}

		sb.append(")");
		String s = prePred + sb.toString();
		// s = s.replace("` ","");
		ret[0] = s;
		if (firstPart.equals(myParts.get(0))) {
			ret[1] = "false";
		} else {
			ret[1] = "true";
		}
		predToLemma.put(pred0, ret);
		return ret;
	}

	// Gets a predicate, swaps its parts! Because we need it in EntGrah
	public static String swapParts(String pred) {
		String pred0 = pred;
		pred = pred.substring(1, pred.length() - 1);

		String[] parts = pred.split(",");
		if (parts.length < 2) {
			// System.err.println("weird pred: "+pred0);
			return pred0;
		}

		return "(" + parts[1] + "," + parts[0] + ")";

	}

	public static String getPredicateSimple(String pred) {
		if (!pred.startsWith("(") || !pred.endsWith(")") || !pred.contains(",")) {
			return pred;
		}
		pred = pred.substring(1, pred.length() - 1);
		pred = pred.toLowerCase();
		String[] parts = pred.split(",");
		String[] parts2 = parts[0].split("\\.");
		String ret = getPartLemm(parts2[0]);

		return ret;
	}

	// visited.As.2 => visit.as.2
	private static String getPartLemm(String s) {
		// String[] parts = s.split("\\.");
		String[] parts = StringUtils.split(s, ".");

		StringBuilder ret = new StringBuilder();
		int ii = 0;
		for (String part : parts) {
			if (modals.contains(part)) {
				ii++;
				continue;
			}
			String lemma;
			if (EntailGraphFactoryAggregator.lemmatizePredWords) {
				lemma = getLemma(part);
			} else {
				lemma = part;
			}
			if (ii < parts.length - 1) {
				ret.append(lemma + ".");
			} else {
				ret.append(lemma);
			}
			ii++;
		}

		return ret.toString();
	}

	public static String preprocess(String text) {
		text = text.replace("\"", "");
		text = removeHtmlTags(text);

		String[] exceptionStrs = new String[] { "|", ">", "<", "&gt", "&lt" };
		for (String es : exceptionStrs) {
			text = text.replace(es, "--");
		}

		text = removeHeader(text);

		return text;
	}

	public static String removeHeader(String text) {
		int maxAcceptableIdx = 40;
		String[] headerIdentifiers = new String[] { ": ", " - ", " -- " };

		int maxIdx = -1;
		String splitter = "";
		for (String s : headerIdentifiers) {
			int thisIdx = text.indexOf(s);
			if (thisIdx >= 0 && (thisIdx > maxIdx || maxIdx == -1)) {
				maxIdx = thisIdx + s.length();
				splitter = s;
			}
		}
		if (maxIdx >= 0 && maxIdx < maxAcceptableIdx) {
			// String header = text.substring(0, maxIdx);
			// System.out.println("header: "+header);
			// System.out.println(text);
			String candText = text.substring(maxIdx);

			// for cases like blah - blah - blah
			if (splitter.equals(" - ")) {
				int idx = candText.indexOf(" - ");
				if (idx > -1 && idx < maxAcceptableIdx) {
					return text;
				} else {
					text = candText;
				}
			}

		}
		return text;
	}

	public static String removeHtmlTags(String text) {
		return Jsoup.parse(text).text();
	}

	// public static String removeHtmlTags(String text) {
	// if (!text.contains("<a href")) {
	// return text;
	// }
	// try {
	// int htmlIdx = text.indexOf("<a href");
	// String ret = text.substring(0, htmlIdx);
	// String rest = text.substring(htmlIdx);
	// int h2 = rest.indexOf(">");
	// if (h2 == -1) {
	// return text;
	// }
	// rest = rest.substring(h2 + 1);
	// int h3 = rest.indexOf("</a>");
	// if (h3 == -1) {
	// return text;
	// }
	// ret += rest.substring(0, h3);
	// ret += rest.substring(h3 + 4);
	// return removeHtmlTags(ret);// because it might have more than one
	// // tag!
	// } catch (Exception e) {
	// return text;
	// }
	//
	// }

	// public static String getLemma(String text){
	//// System.out.println("getlemma");
	// Document doc = new Document(text);
	// StringBuilder sb = new StringBuilder();
	// for (Sentence sent : doc.sentences()) {
	// List<String> lemmas = sent.lemmas();
	// for (String lemma:lemmas){
	// sb.append(lemma+" ");
	// }
	// }
	// return sb.toString();
	// }

	public static String removeModals(String text) {
		String[] parts = text.split(" ");
		String ret = "";
		for (String s : parts) {
			if (modals.contains(s)) {
				continue;
			}
			ret += s + " ";
		}
		return ret.trim();
	}

	public static String getLemma(String text) {

		// if (1==1){
		// return text;
		// }

		StringBuilder ret = new StringBuilder();

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);

		// run all Annotators on this text
		stanPipelineSimple.annotate(document);

		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			// Iterate over all tokens in a sentence
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// Retrieve and add the lemma for each word into the list of
				// lemmas
				ret.append(token.get(LemmaAnnotation.class) + " ");
			}
		}

		return ret.toString().trim();
	}

	// token -> pos
	public static HashMap<String, String> getAllPOSTags(String text) {
		HashMap<String, String> ret = new HashMap<>();
		String[] posTaggedTokens = getPOSTaggedTokens(text);
		String[] tokensList = posTaggedTokens[0].split(" ");
		String[] posList = posTaggedTokens[1].split(" ");
		for (int i = 0; i < tokensList.length; i++) {
			ret.put(tokensList[i], posList[i]);
		}

		return ret;
	}

	public static void findFrequentSentences(String[] args) throws IOException {
		String fname;
		if (args.length == 0) {
			fname = "in2.txt";
		} else {
			fname = args[0];
		}

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fname));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String line;

		HashMap<String, Integer> lineCounts = new HashMap<>();
		int lineId = 0;

		while ((line = br.readLine()) != null) {
			Annotation document = new Annotation(line);
			if (lineId++ % 100000 == 0) {
				System.err.println(lineId);
			}

			// run all Annotators on this text
			stanPipelineSimple2.annotate(document);

			// Iterate over all of the sentences found
			List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			for (CoreMap sentence : sentences) {
				// System.out.println(sentence);
				String sent = sentence.toString();
				if (!lineCounts.containsKey(sent)) {
					lineCounts.put(sent, 1);
				} else {
					lineCounts.put(sent, lineCounts.get(sent) + 1);
				}
			}
		}

		ArrayList<SimpleSpot> sspots = new ArrayList<>();
		for (String s : lineCounts.keySet()) {
			sspots.add(new SimpleSpot(s, lineCounts.get(s)));
		}

		Collections.sort(sspots, Collections.reverseOrder());

		int i = 0;
		for (SimpleSpot ss : sspots) {
			if (i++ < 10000) {
				System.out.println(ss.spot + " " + ss.count);
			}
		}

	}

	// token -> pos
	public static String[] getPOSTaggedTokens(String text) {
		// create an empty Annotation just with the given text

		Annotation document = new Annotation(text);
		String tokenized = "";
		String posList = "";

		// run all Annotators on this text
		stanPipelineSimple.annotate(document);

		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			// Iterate over all tokens in a sentence
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// Retrieve and add the lemma for each word into the list of
				// lemmas
				String word = token.get(TextAnnotation.class);
				word = simpleNormalize(word);
				if (word.equals("")) {
					continue;
				}
				// ret.put(word, token.get(PartOfSpeechAnnotation.class));
				String pos = token.get(PartOfSpeechAnnotation.class);
				tokenized += word + " ";
				for (int i = 0; i < word.split(" ").length; i++) {
					posList += pos + " ";
				}
			}
		}
		String[] ret = new String[] { tokenized.trim(), posList.trim() };
		return ret;
	}

	public static boolean isGeneric(String s, HashMap<String, String> posTags) {
		if (posTags.containsKey(s)) {
			return !PredicateArgumentExtractor.isEntity(posTags.get(s));
		} else {
			String[] parts = s.split(" ");
			s = parts[parts.length - 1];
			return !posTags.containsKey(s) || !PredicateArgumentExtractor.isEntity(posTags.get(s));
		}
	}

	public static Map<String, String> getSimpleNERTypeSent(String text) {

		Map<String, String> tokenToType = new HashMap<>();

		// special case:
		String[] shortMonths = "jan feb mar apr may jun jul aug sep oct nov dec".split(" ");
		HashSet<String> shortMonthsSet = new HashSet<String>();
		for (String s : shortMonths) {
			shortMonthsSet.add(s);
		}

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);

		// run all Annotators on this text
		stanPipeline.annotate(document);

		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			// Iterate over all tokens in a sentence
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// Retrieve and add the lemma for each word into the list of
				// lemmas
				String currentNEType = token.get(NamedEntityTagAnnotation.class).toLowerCase();
				if (stan2Figer.containsKey(currentNEType)) {
					currentNEType = stan2Figer.get(currentNEType);
				} else {
					currentNEType = "thing";
				}
				String thisToken = simpleNormalize(token.originalText());
				if (currentNEType.equals("thing")) {
					if (shortMonthsSet.contains(thisToken) || (thisToken.endsWith(".")
							&& shortMonthsSet.contains(thisToken.substring(0, thisToken.length() - 1)))) {
						currentNEType = "time";
					}
				}
				System.out.println(token + " " + currentNEType);
				tokenToType.put(thisToken, currentNEType);
				// ret += token.get(LemmaAnnotation.class);
			}
		}

		return tokenToType;
	}

	public static String getSimpleNERType(String text) {

		// special case:
		String[] shortMonths = "jan feb mar apr may jun jul aug sep oct nov dec".split(" ");
		HashSet<String> shortMonthsSet = new HashSet<String>();
		for (String s : shortMonths) {
			shortMonthsSet.add(s);
		}
		String textLow = text.toLowerCase();
		// if (textLow.endsWith(".com")){
		// return "WEBSITE";
		// }
		// System.out.println("text low: "+textLow+"
		// "+textLow.substring(0,textLow.length()-1));
		if (shortMonthsSet.contains(textLow)
				|| (textLow.endsWith(".") && shortMonthsSet.contains(textLow.substring(0, textLow.length() - 1)))) {
			return "DATE";
		}

		String ret = "";

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);

		// run all Annotators on this text
		stanPipeline.annotate(document);

		String prevNEType = "O";
		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			// Iterate over all tokens in a sentence
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// Retrieve and add the lemma for each word into the list of
				// lemmas
				String currentNEType = token.get(NamedEntityTagAnnotation.class);
				// allTypes.add(currentNEType);
				System.out.println(token + " " + currentNEType);
				if (prevNEType.equals("O") && !prevNEType.equals(currentNEType)) {
					ret = currentNEType + " ";
				}
				prevNEType = currentNEType;
				// ret += token.get(LemmaAnnotation.class);
			}
		}
		ret = ret.trim();
		if (ret.equals("")) {
			ret = "None";
		}
		return ret.trim();
	}

	public static void convertReleaseToRawJson() throws ParseException, IOException {
		// Scanner sc = new Scanner(new File(
		// "/Users/hosseini/Desktop/D/research/release/crawl"));
		// BufferedReader br = new BufferedReader(new InputStreamReader(
		// new FileInputStream("data/release/crawl"), "UTF-8"));
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream("/Users/hosseini/Desktop/D/research/release/crawl"), "UTF-8"));
		JsonParser parser = new JsonParser();
		int lineId = 0;
		int lineNumber = 0;
		String line;
		while ((line = br.readLine()) != null) {
			JsonObject obj = null;
			if (lineNumber % 10000 == 0) {
				System.err.println(lineNumber);
			}
			lineNumber++;
			try {
				obj = parser.parse(line).getAsJsonObject();
				String text = obj.get("text").getAsString();
				String date = obj.get("date").getAsString();
				long articleId = obj.get("articleId").getAsLong();
				String[] lines = text.split("\\n");
				// System.out.println("text is: "+text);
				// System.out.println(text);
				for (String l : lines) {
					JsonObject myObj = new JsonObject();

					myObj.addProperty("s", l);
					myObj.addProperty("date", date);
					myObj.addProperty("articleId", articleId);
					myObj.addProperty("lineId", lineId);
					// System.out.println(myObj);
					System.out.println(l);
					lineId++;
					// if (lineId%100000==0){
					// System.err.println(lineId);
					// }
				}
				if (text.endsWith("\n")) {
					System.out.println();
				}
			} catch (Exception e) {
				// e.printStackTrace();
				continue;
			}
		}
		br.close();
	}

	public static String[] getLeftRightTimes(String timeInterval) {
		String[] ss = timeInterval.split(",");
		ss[0] = ss[0].substring(1);
		ss[1] = ss[1].substring(0, ss[1].length() - 1);
		return ss;
	}

	// backup: should we check genTypes if no entTypes? Mainly good for not
	// well-formed sentences!
	// arg must be simple-normalized
	public static String getType(String arg, boolean isEntity) {
		if (!EntailGraphFactoryAggregator.isTyped) {
			return "thing";
		}
		if (EntailGraphFactoryAggregator.figerTypes) {
			if (entToFigerType == null) {
				try {
					loadFigerTypes(defaultEntToFigerType);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			String type = entToFigerType.get(arg);
			if (type == null) {
				type = "thing";
			}

			if (!isEntity && entToFigerONLYNE.containsKey(arg) && entToFigerONLYNE.get(arg) == true) {
				type = "thing";
			}

			// System.out.println(arg+" "+type);
			return type;
		} else {
			String type;
			if (isEntity) {
				type = entToType.get(arg);
				if (type == null || type.equals("none")) {
					// System.err.println("no type for " + arg1);
					type = "thing";
				}
			} else {
				type = genToType.get(arg);
				if (type == null) {
					// System.err.println("no type for " + arg1);
					type = "thing";
				}
			}

			return type;
		}
		// return null;

	}

	// things like BROTHER, WINGs, etc. Because we might wrongly ground a noun
	// to these NEs
	static boolean shouldBeONLYNE(String s) {
		if (s.length() < 2) {
			return false;
		}
		for (int i = 0; i < s.length() - 1; i++) {
			if (!(s.charAt(i) + "").toUpperCase().equals(s.charAt(i) + "")) {
				return false;
			}
		}
		return true;
	}

	static void loadFigerTypes(String path) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(path));
		String line = null;
		Map<String, String> ret = new HashMap<>();
		entToFigerONLYNE = new HashMap<>();
		int idx = 0;
		while ((line = br.readLine()) != null) {
			String[] ss = line.split("\t");
			boolean onlyNE = shouldBeONLYNE(ss[1]);
			String ent = simpleNormalize(ss[1]);
			// ent = StringUtils.stripAccents(ent);// Changed on 6 OCT// Changed back on 15
			// Dec
			String type = ss[2];
			if (type.startsWith("/")) {
				type = type.substring(1);
			}
			int slashIdx = type.indexOf("/");
			if (slashIdx != -1) {
				type = type.substring(0, slashIdx);
			}

			if (type.equals("")) {
				type = "thing";
			}
			type = type.trim();
			// type = type.replace("/", "_");
			if (ret.containsKey(ent) && type.equals("thing")) {
				continue;
			}

			ret.put(ent, type);
			entToFigerONLYNE.put(ent, onlyNE);

			// System.out.println(ent+" "+type);
			if (idx % 1000000 == 0) {
				System.err.println("figer " + idx);
				// if (idx==1000000){
				// break;
				// }
			}
			idx++;

		}
		br.close();
		entToFigerType = ret;
	}

	public static void loadEntGenTypes(String entTypesFName, String genTypesFName) {
		try {
			entToType = new HashMap<String, String>();// I do this so that other
														// threads don't touch
														// this!
			entToType = loadEntTypes(entTypesFName, true);
			genToType = new HashMap<>();
			genToType = loadEntTypes(genTypesFName, false);

			// entToFigerType = loadFigerTypes(defaultEntToFigerType);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Map<String, String> loadEntTypes(String entTypesFName, boolean forEnts) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(entTypesFName));
		HashMap<String, String> entToType = new HashMap<String, String>();
		String line;
		while ((line = br.readLine()) != null) {
			try {
				String[] toks = line.split("::");
				String wikiEnt = toks[0];

				if (forEnts) {
					wikiEnt = simpleNormalize(wikiEnt);
				}
				String type = toks[1];
				if (type.equals("time_period")) {
					type = "date";
				}
				entToType.put(wikiEnt, type);
			} catch (Exception e) {
				System.err.println("bad ent type: " + line);
				//
			}
		}
		br.close();
		return entToType;
	}

	static HashMap<String, HashMap<String, String>> loadAidaLinked() throws IOException {
		String path = "aida/news_linked.json";
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
		JsonParser jsonParser = new JsonParser();
		String line;
		HashMap<String, HashMap<String, String>> artIdToEntToWiki = new HashMap<>();
		int lineNumber = 0;
		while ((line = br.readLine()) != null) {
			JsonObject jo = jsonParser.parse(line).getAsJsonObject();

			String articleId = jo.get("artId").getAsString();
			JsonArray ews = jo.get("ew").getAsJsonArray();
			HashMap<String, String> thisEntToWiki = null;
			if (ews.size() > 0) {
				thisEntToWiki = new HashMap<>();
				artIdToEntToWiki.put(articleId, thisEntToWiki);
			}
			for (int i = 0; i < ews.size(); i++) {
				JsonObject ew = ews.get(i).getAsJsonObject();
				String ent = ew.get("e").getAsString();
				ent = simpleNormalize(ent);
				String wiki = ew.get("w").getAsString();
				if (wiki.equals("--NME--")) {
					continue;
				}
				thisEntToWiki.put(ent, wiki);
			}
			if (lineNumber++ % 1000 == 0) {
				System.err.println(lineNumber);
			}
		}
		System.err.println("linked NEs loaded!");
		br.close();
		return artIdToEntToWiki;
	}

	// public static Map<String, String> loadEntToWiki(int cutoff) throws
	// IOException {
	// BufferedReader br = new BufferedReader(new InputStreamReader(new
	// FileInputStream(defaultEntToWikiFName)));
	// String line;
	// entToWiki = new HashMap<String, String>();
	//
	// // PrintStream op = new PrintStream(new File("wikiEnts.txt"));
	// ArrayList<SimpleSpot> spots = new ArrayList<SimpleSpot>();
	// HashMap<String, Integer> s2Count = new HashMap<String, Integer>();
	// HashMap<String, Integer> ent2Count = new HashMap<String, Integer>();
	//
	// while ((line = br.readLine()) != null) {
	// try {
	// StringTokenizer st = new StringTokenizer(line, "::");
	// String ent = st.nextToken();
	//
	// ent = simpleNormalize(ent);
	//
	// int count = Integer.parseInt(st.nextToken());
	// String wiki = st.nextToken();
	//
	// // in case normalization leads to the same thing, we wanna have
	// // sth that has more count!
	// if (entToWiki.containsKey(ent) && count <= ent2Count.get(ent)) {
	// continue;// we don't wanna replace something good!
	// }
	// if (count >= cutoff) {
	// entToWiki.put(ent, wiki);
	// }
	//
	// ent2Count.put(ent, count);
	//
	// if (!s2Count.containsKey(wiki)) {
	// s2Count.put(wiki, 0);
	// }
	// s2Count.replace(wiki, s2Count.get(wiki) + count);
	// } catch (Exception e) {
	//
	// }
	// }
	// br.close();
	// System.err.println("entToWiki size: " + entToWiki.size());
	// for (String s : s2Count.keySet()) {
	// spots.add(new SimpleSpot(s, s2Count.get(s)));
	// }
	// Collections.sort(spots, Collections.reverseOrder());
	// for (SimpleSpot s : spots) {
	// if (s.count == 1) {
	// break;
	// }
	// // op.println(s.spot + "::" + s.count);
	// }
	// return entToWiki;
	// }

	// args: fileName, shouldLink, useContext (aidalight), num to have in
	// memory. Note: you have optimize this for larger corpus!
	// works for NewsSpike
	static void convertPredArgsToJson(String[] args) throws IOException {
		if (args == null || args.length == 0) {
			args = new String[] { "predArgs_gen.txt", "true", "true", "12000000" };
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "UTF-8"));
		boolean shouldLink = Boolean.parseBoolean(args[1]);
		boolean useContext = Boolean.parseBoolean(args[2]);// aidalight
		HashMap<String, HashMap<String, String>> artIdToEntToWiki = null;
		if (useContext) {
			artIdToEntToWiki = loadAidaLinked();
		}
		System.err.println("useNamedEntities: " + shouldLink);
		int maxLines = Integer.parseInt(args[3]);

		// Map<String, String> entToWiki = loadEntToWiki(0);
		// System.err.println("testing " + entToWiki.get("mike smith"));

		String line;
		int lineNumbers = 0;
		String curLine = null;
		ArrayList<String> curPrArgs = new ArrayList<String>();

		// int firstIdxToPrint = 0;
		// int numPrintAtOnce = 1000000;
		// ArrayList<JsonObject> jObjsToPrint = new ArrayList<JsonObject>();

		JsonObject[] jObjsToPrint = new JsonObject[maxLines];
		int nextIdxToPrint = 0;
		int maxIdx = -1;
		int largestPossibleGap = 1000000;
		HashMap<String, Integer> gens = new HashMap<>();

		while ((line = br.readLine()) != null) {
			// System.err.println("line: " + line);
			if (line.equals("") || line.contains("e.s.nlp.pipeline")) {
				continue;
			}
			if (line.startsWith("#line:")) {
				curLine = line.substring(7);
				if (curLine != null) {
					try {
						// Add the current line
						// First, we should read other details!
						line = br.readLine();
						String lineId = line.substring(9);
						line = br.readLine();
						String articleId = line.substring(12);
						line = br.readLine();
						String date = line.substring(7);

						JsonObject jObj = new JsonObject();
						jObj.addProperty("s", curLine);
						jObj.addProperty("date", date);
						jObj.addProperty("articleId", articleId);
						jObj.addProperty("lineId", lineId);

						// Now, let's read all the pred_arg lines
						String prArgLine = null;
						curPrArgs = new ArrayList<String>();

						while ((prArgLine = br.readLine()) != null && !prArgLine.equals("")) {
							// System.out.println("pr arg line: " + prArgLine);
							String pred = null;
							String arg1 = null;
							String arg2 = null;
							int eventIdx, sentIdx;
							String GorNE = null;

							try {
								StringTokenizer st = new StringTokenizer(prArgLine);
								pred = st.nextToken();
								arg1 = st.nextToken();
								arg2 = st.nextToken();
								eventIdx = Integer.parseInt(st.nextToken());
								GorNE = st.nextToken();
								sentIdx = Integer.parseInt(st.nextToken());
								arg1 = simpleNormalize(arg1);
								arg2 = simpleNormalize(arg2);

								boolean[] isGens = new boolean[2];
								isGens[0] = GorNE.charAt(0) == 'G';
								isGens[1] = GorNE.charAt(1) == 'G';

								if (isGens[0]) {
									if (!gens.containsKey(arg1)) {
										gens.put(arg1, 0);
									}
									gens.put(arg1, gens.get(arg1) + 1);
								}
								if (isGens[1]) {
									if (!gens.containsKey(arg2)) {
										gens.put(arg2, 0);
									}
									gens.put(arg2, gens.get(arg2) + 1);
								}

								if (shouldLink) {
									if (!isGens[0]) {
										if (!useContext) {
											// arg1 = entToWiki.containsKey(arg1) ? entToWiki.get(arg1) : arg1;
										} else {
											if (artIdToEntToWiki.containsKey(articleId)) {
												HashMap<String, String> e2w = artIdToEntToWiki.get(articleId);
												arg1 = e2w.containsKey(arg1) ? e2w.get(arg1) : arg1;
											}
										}
									}
									if (!isGens[1]) {
										if (!useContext) {
											// arg2 = entToWiki.containsKey(arg2) ? entToWiki.get(arg2) : arg2;
										} else {
											if (artIdToEntToWiki.containsKey(articleId)) {
												HashMap<String, String> e2w = artIdToEntToWiki.get(articleId);
												arg2 = e2w.containsKey(arg2) ? e2w.get(arg2) : arg2;
											}
										}
									}
								}
							} catch (Exception e) {
								e.printStackTrace();
								System.err.println("exception for: " + line);
								continue;
							}

							String prArg = "(" + pred + "::" + arg1 + "::" + arg2 + "::" + GorNE + "::" + sentIdx + "::"
									+ eventIdx + ")";
							curPrArgs.add(prArg);
						}

						JsonArray rels = new JsonArray();
						for (int i = 0; i < curPrArgs.size(); i++) {
							JsonObject rel = new JsonObject();
							rel.addProperty("r", curPrArgs.get(i));
							rels.add(rel);
						}
						jObj.add("rels", rels);

						int lineIdInt = Integer.parseInt(lineId);
						if (lineIdInt > maxIdx) {
							maxIdx = lineIdInt;
						}
						jObjsToPrint[lineIdInt] = jObj;

						// System.out.println(jObj);

						// if (Integer.parseInt(lineId) < firstIdxToPrint) {
						// //
						// System.out.print("here: "+Integer.parseInt(lineId)+"
						// "+firstIdxToPrint);
						// System.out.println(jObj);
						// } else {
						// int lineIdInt = Integer.parseInt(lineId)
						// - firstIdxToPrint;
						//
						// if (lineIdInt >= numPrintAtOnce) {
						// // It's time to print everything you have up to now!
						// for (int i = 0; i < jObjsToPrint.size(); i++) {
						// JsonObject jo = jObjsToPrint.get(i);
						// if (jo != null) {
						// // System.out.print("flushing:");
						// System.out.println(jo);
						// }
						// }
						//
						// firstIdxToPrint += numPrintAtOnce;
						// lineIdInt = Integer.parseInt(lineId)
						// - firstIdxToPrint;
						// jObjsToPrint = new ArrayList<JsonObject>();
						// }
						//
						// if (lineIdInt > jObjsToPrint.size()) {
						// int cc = lineIdInt - jObjsToPrint.size();
						// for (int i = 0; i < cc; i++) {
						// jObjsToPrint.add(null);
						// }
						// }
						// if (lineIdInt == jObjsToPrint.size()) {
						// jObjsToPrint.add(lineIdInt, jObj);
						// } else {
						// jObjsToPrint.set(lineIdInt, jObj);
						// }
						// // System.out.println("adding: "+lineIdInt + "
						// "+lineId
						// // + " "+jObjsToPrint.size());
						//
						// }
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}
				}

				lineNumbers++;

				if (lineNumbers % 1000 == 0) {
					while (jObjsToPrint[nextIdxToPrint] != null && nextIdxToPrint < jObjsToPrint.length) {
						System.out.println(jObjsToPrint[nextIdxToPrint]);
						jObjsToPrint[nextIdxToPrint++] = null;
					}
				}

				// if (maxIdx-nextIdxToPrint>largestPossibleGap){
				// nextIdxToPrint++;
				// System.err.println("skipping: "+nextIdxToPrint+" "+maxIdx);
				// }

				if (lineNumbers % 10000 == 0) {
					System.err.println(lineNumbers);
				}
			}

			// Let's make the JSON
		}

		// flush
		while (nextIdxToPrint < maxIdx) {
			if (jObjsToPrint[nextIdxToPrint] != null) {
				System.out.println(jObjsToPrint[nextIdxToPrint]);
			}
			jObjsToPrint[nextIdxToPrint++] = null;
		}

		// System.out.println("all gens: ");
		PrintStream op = new PrintStream(new File("gens.txt"));
		ArrayList<SimpleSpot> genSpots = new ArrayList<>();
		for (String s : gens.keySet()) {
			genSpots.add(new SimpleSpot(s, gens.get(s)));
		}
		Collections.sort(genSpots, Collections.reverseOrder());
		for (SimpleSpot s : genSpots) {
			op.println(s.spot + "::" + s.count);
		}
		op.close();

		// Let's flush jObjsToPrint
		// for (JsonObject jo : jObjsToPrint) {
		// if (jo != null) {
		// System.out.println(jo);
		// }
		// }

		br.close();
	}

	public static void lemmatizePredsJson(String fname) throws JsonSyntaxException, IOException {
		BufferedReader br = new BufferedReader(new FileReader(fname));

		JsonParser jsonParser = new JsonParser();

		// long t0;
		// long sharedTime = 0;

		String line;
		while ((line = br.readLine()) != null) {

			if (line.startsWith("exception for") || line.contains("nlp.pipeline")) {
				continue;
			}
			JsonObject jObj = jsonParser.parse(line).getAsJsonObject();

			JsonArray jar = jObj.get("rels").getAsJsonArray();
			// t0 = 0;

			for (int i = 0; i < jar.size(); i++) {
				// if (lineNumbers%100==0){
				// t0 = System.currentTimeMillis();
				// }
				JsonObject relObj = jar.get(i).getAsJsonObject();
				String relStr = relObj.get("r").getAsString();
				relStr = relStr.substring(1, relStr.length() - 1);
				String[] parts = relStr.split("::");
				String pred = parts[0];

				// System.out.println("pred: "+pred);
				String[] predicateLemma = Util.getPredicateLemma(pred, true);

				pred = predicateLemma[0];
				// pred = Util.getPredicateSimple(pred);
				// System.out.println("pred lemma: " + pred);
				String arg1;
				String arg2;

				// false means args are reversed.
				if (predicateLemma[1].equals("false")) {
					arg1 = parts[1];
					arg2 = parts[2];// type1 and type2 are fine
				} else {
					arg1 = parts[2];
					arg2 = parts[1];
					parts[3] = "" + parts[3].charAt(1) + parts[3].charAt(0);
				}

				String lammatizedRel = "(" + pred + "::" + arg1 + "::" + arg2 + "::" + parts[3] + ")";
				relObj.remove("r");
				relObj.addProperty("r", lammatizedRel);

				// Now we have pred, arg1 and arg2
			}
			System.out.println(jObj);
		}
		br.close();
	}

	public static void readJSONSimple() throws FileNotFoundException, ParseException {
		// Scanner sc = new Scanner(new
		// File("/Users/hosseini/Desktop/D/research/release/crawl"));
		Scanner sc = new Scanner(new File("data/release/crawl"));
		JSONParser parser = new JSONParser();

		while (sc.hasNext()) {
			JSONObject obj = null;
			try {
				String line = sc.nextLine();
				// System.out.println(line);
				obj = (JSONObject) parser.parse(line);
			} catch (Exception e) {
				// Just ignore any bad line!
				continue;
			}

			String text = (String) obj.get("text");
			if (text != null && text.endsWith("\n")) {
				System.out.print(text);
			} else {
				System.out.println(text);
			}

		}
		sc.close();
	}

	public static boolean acceptablePredFormat(String pred, boolean isCCG) {
		if (!isCCG) {
			return true;
		}
		if (Util.isCoordination(pred)) {
			return false;
		}
		pred = pred.substring(1, pred.length() - 1);
		String[] parts = pred.split(",");
		if (parts.length != 2) {
			return false;
		}

		// System.out.println("parts: "+parts[0]+" "+parts[1]);
		// System.out.println(acceptablePredPartFormat(parts[0]));
		// System.out.println(acceptablePredPartFormat(parts[1]));

		return acceptablePredPartFormat(parts[0]) || acceptablePredPartFormat(parts[1]);
	}

	static String[] goodPart2s = new String[] { "1", "2", "3" };
	static HashSet<String> goodPart2sSet = new HashSet<String>();
	static {
		for (String s : goodPart2s) {
			goodPart2sSet.add(s);
		}
	}

	public static boolean acceptablePredPartFormat(String pred) {
		String[] parts = StringUtils.split(pred, ".");

		if (parts.length < 2) {
			return false;
		}
		boolean ret = false;
		for (int i = 1; i < parts.length; i++) {
			if (goodPart2sSet.contains(parts[i])) {
				ret = true;
				break;
			}
		}
		return ret;
	}

	static void makeLinkedEntities() throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("entToWiki.txt")));
		String line;
		br.readLine();
		HashSet<String> ents = new HashSet<String>();
		while ((line = br.readLine()) != null) {
			try {
				String[] parts = line.split("::");
				ents.add(parts[2]);
			} catch (Exception e) {

			}

		}

		PrintStream op = new PrintStream(new File("allLinkedEnts.txt"));
		for (String e : ents) {
			op.println(e);
		}
		op.close();
		br.close();
	}

	static void extractTypesStanNER(String[] args) throws FileNotFoundException, IOException, ParseException {
		String fileName = "wikiEnts.txt";
		if (args.length > 0) {
			fileName = args[0];
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
		// BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line;
		while ((line = br.readLine()) != null) {
			String wikiEnt = null;
			try {
				wikiEnt = line.split("::")[0];
				wikiEnt = wikiEnt.replace("_", " ");
				String stanType = getSimpleNERType(wikiEnt);

				System.out.println(wikiEnt + "::" + stanType);
				// for (Object element : elements) {
				// // System.out.println(JsonPath.read(element,
				// // "$.result.name").toString());
				// System.out.println(element);
				// }
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(wikiEnt + ": EXCEPTION");
			}
		}
		br.close();
	}

	static void extractTypesGooKG(String[] args) throws FileNotFoundException, IOException, ParseException {
		String fileName = "wikiEnts.txt";
		int apiIdx = 2;
		if (args.length > 0) {
			fileName = args[0];
			apiIdx = Integer.parseInt(args[1]);
		}

		Scanner sc = new Scanner(new File("kgsearch.properties"));
		ArrayList<String> apis = new ArrayList<String>();
		while (sc.hasNext()) {
			apis.add(sc.nextLine());
		}
		sc.close();

		HttpTransport httpTransport = new NetHttpTransport();
		HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
		JSONParser parser = new JSONParser();

		// BufferedReader br = new BufferedReader(new InputStreamReader(
		// new FileInputStream(fileName)));
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line;
		int lineNumber = 0;
		while ((line = br.readLine()) != null) {
			// if (lineNumber == 20) {
			// break;
			// }
			lineNumber++;
			String wikiEnt = null;
			try {
				wikiEnt = line.split("::")[0];
				wikiEnt = wikiEnt.replace("_", " ");
				GenericUrl url = new GenericUrl("https://kgsearch.googleapis.com/v1/entities:search");
				String query = wikiEnt;
				url.put("query", query);
				url.put("limit", "10");
				url.put("indent", "true");
				url.put("key", apis.get(apiIdx));
				HttpRequest request = requestFactory.buildGetRequest(url);
				HttpResponse httpResponse = request.execute();
				JSONObject response = (JSONObject) parser.parse(httpResponse.parseAsString());
				JSONArray elements = (JSONArray) response.get("itemListElement");
				response.put("wikiEnt", wikiEnt);
				System.out.println(response);
				// for (Object element : elements) {
				// // System.out.println(JsonPath.read(element,
				// // "$.result.name").toString());
				// System.out.println(element);
				// }
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println(wikiEnt + ": EXCEPTION");
			}
		}
		br.close();
	}

	// converts google KG json to simple txt
	static void simpleTypeRead(String[] args) throws IOException {
		if (args.length == 0) {
			args = new String[] { "we0.txt" };
		}
		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		String line = br.readLine();
		JsonParser jparser = new JsonParser();
		while ((line = br.readLine()) != null) {
			if (line.endsWith(": EXCEPTION")) {
				System.out.println(line);
				continue;
			}
			try {
				JsonObject lobj = jparser.parse(line).getAsJsonObject();
				String s = lobj.get("wikiEnt").getAsString();
				JsonArray tarr = lobj.get("itemListElement").getAsJsonArray();
				if (tarr.size() == 0) {
					System.out.println(s + "::None");
					continue;
				}
				JsonObject tobj = tarr.get(0).getAsJsonObject().get("result").getAsJsonObject();
				// System.out.println(tobj);
				String type = tobj.get("@type") + "";
				System.out.println(s + "::" + type);
			} catch (Exception e) {
				System.err.println(line);
				continue;
			}

		}
		br.close();
	}

	static void writeAllNodeCounts() throws FileNotFoundException {
		File folder = new File("typedEntGrDir");
		File[] files = folder.listFiles();
		for (File f : files) {
			if (!f.getAbsolutePath().endsWith("_sim.txt")) {
				continue;
			}
			Scanner sc = new Scanner(f);
			String l = sc.nextLine();

			int ii = l.indexOf(':');
			int ii2 = l.indexOf(',');
			String types = l.substring(ii + 2, ii2);

			ii = l.lastIndexOf(':');
			int c = Integer.parseInt(l.substring(ii + 2));
			System.out.println(c + "\t" + types);
		}
	}

	public static void countURLDomains(String[] args) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		String line;
		HashMap<String, Integer> urlCounts = new HashMap<>();
		while ((line = br.readLine()) != null) {
			try {
				String domain = getDomainName(line);
				if (!urlCounts.containsKey(domain)) {
					urlCounts.put(domain, 0);
				}
				urlCounts.put(domain, urlCounts.get(domain) + 1);
			} catch (Exception e) {
				continue;
			}
		}
		ArrayList<SimpleSpot> elements = new ArrayList<>();
		for (String s : urlCounts.keySet()) {
			elements.add(new SimpleSpot(s, urlCounts.get(s)));
		}
		Collections.sort(elements, Collections.reverseOrder());
		for (SimpleSpot ss : elements) {
			System.out.println(ss.spot + "\t" + ss.count);
		}
	}

	public static String getDomainName(String url) throws URISyntaxException {
		URI uri = new URI(url);
		String domain = uri.getHost();

		return domain.startsWith("www.") ? domain.substring(4) : domain;
	}

	public static void countArgs(String[] args) throws JsonSyntaxException, IOException {
		String fName = args[0];
		HashMap<String, Integer> argCount = new HashMap<>();
		HashMap<String, Integer> relCount = new HashMap<>();
		BufferedReader br = new BufferedReader(new FileReader(fName));

		int lineNumbers = 0;
		JsonParser jsonParser = new JsonParser();

		// long t0;
		// long sharedTime = 0;

		String line;
		while ((line = br.readLine()) != null) {

			if (line.startsWith("exception for") || line.contains("nlp.pipeline")) {
				continue;
			}
			JsonObject jObj = jsonParser.parse(line).getAsJsonObject();

			JsonArray jar = jObj.get("rels").getAsJsonArray();
			// t0 = 0;

			for (int i = 0; i < jar.size(); i++) {
				// if (lineNumbers%100==0){
				// t0 = System.currentTimeMillis();
				// }
				JsonObject relObj = jar.get(i).getAsJsonObject();
				String relStr = relObj.get("r").getAsString();
				relStr = relStr.substring(1, relStr.length() - 1);
				String[] parts = relStr.split("::");
				String pred = parts[0];
				if (++lineNumbers % 10000 == 0) {
					System.err.println(lineNumbers);
				}

				if (!Util.acceptablePredFormat(pred, true)) {
					continue;
				}
				pred = Util.getPredicateLemma(pred, true)[0];
				if (!relCount.containsKey(pred)) {
					relCount.put(pred, 1);
				} else {
					relCount.put(pred, relCount.get(pred) + 1);
				}

				// We also remove "-" here, because sometimes, we have the type
				// without "-". But we didn't remove
				// "-" when we're looking in g kg, because it might help!

				for (int j = 1; j < 3; j++) {
					parts[j] = simpleNormalize(parts[j]);

					if (!argCount.containsKey(parts[j])) {
						argCount.put(parts[j], 1);
					} else {
						argCount.put(parts[j], argCount.get(parts[j]) + 1);
					}

				}
			}
		}

		System.out.println("rel counts:");

		ArrayList<SimpleSpot> sspots = new ArrayList<>();
		for (String s : relCount.keySet()) {
			sspots.add(new SimpleSpot(s, relCount.get(s)));
		}

		Collections.sort(sspots, Collections.reverseOrder());

		for (SimpleSpot ss : sspots) {
			System.out.println(ss.spot + " " + ss.count);
		}

		System.out.println("arg counts:");

		sspots = new ArrayList<>();
		for (String s : argCount.keySet()) {
			sspots.add(new SimpleSpot(s, argCount.get(s)));
		}

		Collections.sort(sspots, Collections.reverseOrder());

		for (SimpleSpot ss : sspots) {
			System.out.println(ss.spot + " " + ss.count);
		}
		br.close();
	}

	// Obama: [Barack_Obama,Person]
	// morning: [morning,time_...]
	// stanType is already converted to Figer type
	public static String linkAndType(String arg, boolean isEntity, boolean backup,
			Map<String, String> tokenToStanType) {

		String mainArg = arg;
		String type;

		// way!
		// no linking required for generics!
		if (isEntity) {
			arg = simpleNormalize(arg);

			// String wiki = entToWiki.get(arg);//Changed on 20 Dec
			// if (wiki == null && EntailGraphFactoryAggregator.figerTypes) {
			// wiki = arg;
			// }

			// Dec 20, 2017: decided not to lookup ent=>wiki. Instead use stan NER for
			// person, loc, etc
			String wiki = arg;

			if (wiki != null) {
				wiki = simpleNormalize(wiki);
				type = getType(wiki, true);
			} else {
				type = "thing";
			}

			if (type.equals("thing")) {
				String[] ss = wiki.split(" ");
				for (String s : ss) {
					String typeCand = tokenToStanType.get(s);
					if (typeCand != null && !typeCand.equals("thing")) {
						type = typeCand;
						break;
					}
				}

			}

			///
			if (backup && type.equals("thing")) {
				// We should first backup to NER
				String argNER = wiki != null ? wiki : arg;
				String NERType = getSimpleNERType(argNER).toLowerCase();
				if (!NERType.equals("none")) {
					type = NERType;
					System.out.println("backing up to NER type: " + arg + " " + NERType);
				} else {
					// Now, we have to back up to genTypes
					arg = getLemma(arg);
					String genType = getType(arg, false);
					System.out.println("backing up to genTypes: " + mainArg + " " + arg + ": " + genType);
					type = genType;
					if (genType.equals("thing")) {
						System.out.println("no type found for: " + arg);
					}
				}
			}
		} else {
			if (EntailGraphFactoryAggregator.figerTypes) {
				arg = simpleNormalize(arg);
			}
			type = getType(arg, false);
			if (type.equals("thing")) {
				arg = simpleNormalize(arg);
				type = getType(getLemma(arg), false);
				System.out.println("try lemma for: " + arg + " " + getLemma(arg) + " " + type);
			}

			// if it's still thing, we should back off to NEs!
			if (backup && type.equals("thing")) {
				String NEarg = simpleNormalize(arg);

				// String wiki = entToWiki.get(NEarg);
				String wiki = NEarg;

				if (wiki != null) {
					wiki = simpleNormalize(wiki);
					type = getType(wiki, true);
				} else {
					type = "thing";
				}
				System.out.println("backing up to entTypes for " + mainArg + ": " + type);
			}
		}

		mainArg = mainArg.replace(" ", "_");

		return mainArg + "::" + type;
	}

	public static String simpleNormalize(String s) {
		s = s.replace("_", " ");
		s = s.replace("-", " ");

		// Why would I do the below? :)
		// The thing is Siva's code inserts -, and we're
		// not
		// sure which - is legitimate!

		s = s.toLowerCase().trim();

		return s;
	}

	public static HashSet<String> loadAllDSPreds(String path) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(path));
		HashSet<String> ret = new HashSet<>();
		String line = null;
		while ((line = br.readLine()) != null) {

			String[] parts = line.split("\t");
			String[] ss = parts[0].split(" ");
			String rel1 = ss[0];
			ss = parts[1].split(" ");
			String rel2 = ss[0];
			ret.add(rel1);
			ret.add(rel2);
		}

		br.close();
		return ret;
	}

	// static void convertOIEToOurFormat() {
	// BufferedReader br = new BufferedReader(new InputStreamReader(new
	// FileInputStream(args[0]), "UTF-8"));
	// String line;
	// while ((line=br.readLine())!=null){
	// if (line.equals("")){
	// continue;
	// }
	//
	// }
	// }

	public static boolean isCoordination(String pred) {
		try {
			pred = pred.substring(1, pred.length() - 1);
			String[] parts = pred.split(",");
			return parts[0].equals(parts[1]);
		} catch (Exception e) {
			return false;
		}
	}

	static void testNERStan() throws FileNotFoundException {

		Scanner sc2 = new Scanner(new File("freebase_types/types.map"));
		Set<String> figers = new HashSet<>();
		while (sc2.hasNext()) {
			String line = sc2.nextLine();
			figers.add(line.split("\t")[1].split("/")[1]);
		}
		for (String s : figers) {
			System.out.println(s);
		}

		Scanner sc = new Scanner(new File("data/ent/all.txt"));
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			System.out.println(line);
			getSimpleNERTypeSent(line.split("\t")[0]);
			getSimpleNERTypeSent(line.split("\t")[1]);
			System.out.println();
		}
	}

	public static void main(String[] args) throws ParseException, IOException {

		getSimpleNERType("kansas jayhawks won the game.");
		getSimpleNERType("prime minister stephen harper");
		
		// testNERStan();

		// String[] ss = "2013-02-07".split("$");
		// System.out.println(ss.length);
		// System.out.println("here");
		// System.out.println(getLemma("You'll like it!"));
		// HashMap<String, String> allPOSTags = getAllPOSTags("You will like Los
		// Angeles");
		// for (String s : allPOSTags.keySet()) {
		// System.out.println(s + ": " + allPOSTags.get(s));
		// }
		// System.out.println(isGeneric("los angeles", allPOSTags));
		//
		// convertToPArgFormat(args);

		// System.out.println(normalizeArg("The two books"));
		// findFrequentSentences(args);

		// System.out.println(1d);
		// readJSONSimple();
		// convertReleaseToRawJson();
		// convertPredArgsToJson(args);
		// countArgs(args);
		// System.out.println(removeHtmlTags(""));

		// lemmatizePredsJson(args[0]);
		// writeAllNodeCounts();
		// countURLDomains(args);
		// convertToPArgFormat(args[0]);
		// System.out.println(Util.getLemma("encouraged"));
		// System.out.println(Util.getPredicateSimple(""));
		// loadEntToWiki(0);
		// makeLinkedEntities();
		// long t0 = System.currentTimeMillis();
		// extractTypes(args);
		// System.err.println("time: " + (System.currentTimeMillis() - t0));
		// simpleTypeRead(args);
		// extractTypesStanNER(args);

		// countArgs("news_gen3.txt");
		// System.out.println(getPredicateLemma("(is.with.2,is.with.2)"));
	}

}
