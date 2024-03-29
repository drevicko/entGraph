package constants;

import entailment.vector.EntailGraphFactoryAggregator;

public class ConstantsAgg {

	// ####################################################################################################
	// Extract sims
	// All parameters:

	 public static boolean onlyDSPreds = false;
	 public static boolean rawExtractions = false;// gbooks original (or gbooks in general?)
	 public static boolean GBooksCCG = false;
	 public static boolean useTimeEx = false;
	 public static boolean isCCG = true;
	 public static boolean isTyped = true;
	 public static final boolean normalizePredicate = true;// if rawExtraction, wouldn't matter.
	 public static final boolean backupToStanNER = false;// You can make this true, but it will take some good time to run!
	 public static boolean removeEventEventModifiers = true;
	 public static boolean removeStopPreds = false;
	 public static boolean cutoffBasedonNSGraphs = true;//use NSpike-based cutoffs
	 public static String NSSizesAddress = "NS_sizes.txt";
	
	 // cutoffs
	 public static int minArgPairForPred = 4;// 100;
	 public static int minPredForArgPair = 4;// 20;// min num of unique predicates for argpair
	//when NS based num aps, we allow x aps for each pred, even if not in NS
	 public static int numArgPairsNSBasedAlwaysAllowed = 10; 
	 public static int maxPredsTotal = -1;// 35000;
	 public static final int minPredForArg = -1;// min num of unique predicates for
	 public static final int numThreads = 20;
	
	 // embedding parameters
	 public static boolean embBasedScores = false;// use sigmoid(transE score) instead of counts
	 public static double sigmoidLocParameter = 10;
	 public static double sigmoidScaleParameter = .25;
	 public static int dsPredNumThreads = 15;
	 public static EntailGraphFactoryAggregator.ProbModel probModel = EntailGraphFactoryAggregator.ProbModel.PEL;
	 public static EntailGraphFactoryAggregator.LinkPredModel linkPredModel =
	 EntailGraphFactoryAggregator.LinkPredModel.ConvE;
	
//	 public static String relAddress = "news_gen8_p.json";
	 public static String relAddress = "news_genC.json";
	 
	 public static String simsFolder =
	 "typedEntGrDirC_aida_typed_NSSize_predBasedEx_"+minArgPairForPred+"_"+minPredForArgPair;
	 
	 public static boolean computeProbELSims = false;
	 public static boolean linkPredBasedRandWalk = false;

	// ####################################################################################################
	// ConvE untyped
	// All parameters:

//	public static boolean onlyDSPreds = false;
//	public static boolean rawExtractions = false;// gbooks original (or gbooks in general?)
//	public static boolean GBooksCCG = false;
//	public static boolean useTimeEx = false;
//	public static boolean isCCG = true;
//	public static boolean isTyped = false;
//	public static final boolean normalizePredicate = true;// if rawExtraction, wouldn't matter.
//	public static final boolean backupToStanNER = false;// You can make this true, but it will take some good time to
//	// run!
//	public static boolean removeEventEventModifiers = false;
//	public static boolean removeStopPreds = false;
//
//	public static int numThreads = 20;
//	// cutoffs
//	public static int minArgPairForPred = 20;// 100;
//	public static int minPredForArgPair = 20;// 20;// min num of unique predicates for argpair
//	public static int maxPredsTotal = -1;// 35000;
//	public static final int minPredForArg = -1;// min num of unique predicates for
//
//	// embedding parameters
//	public static boolean embBasedScores = false;// use sigmoid(transE score) instead of counts. Must be false
//	public static double sigmoidLocParameter = 10;
//	public static double sigmoidScaleParameter = .25;
//
//	public static boolean computeProbELSims = true;
//	public static int dsPredNumThreads = 15;
//
//	public static EntailGraphFactoryAggregator.ProbModel probModel = EntailGraphFactoryAggregator.ProbModel.RandWalk;
//	public static EntailGraphFactoryAggregator.LinkPredModel linkPredModel = EntailGraphFactoryAggregator.LinkPredModel.ConvE;
//	// only used for agg main func, not randwalk itself, false: count-based
//	public static boolean linkPredBasedRandWalk = false;
//	public static String relAddress = "news_gen8_p.json";
//	public static String simsFolder = "typedEntGrDir_aida_untyped_20_20_convE_ol_NS_rwC_pmi";
			//+ ConstantsRWalk.convEArgPairNeighs;

	// relAddress = "news_gen8_aida.json";
	// relAddress = "news_genC_aida.json";
	// relAddress = "gbooks_norm.txt";

}
