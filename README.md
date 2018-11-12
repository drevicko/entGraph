This is the implementation for the following paper:

Learning Typed Entailment Graphs with Global Soft Constraints, Mohammad Javad Hosseini, Nathanael Chambers, Siva Reddy, Xavier Holt, Shay B. Cohen, Mark Johnson, and Mark Steedman. Transactions of the Association for Computational Linguistics (TACL 2018).

Please follow the below instructions to create entailment graphs and/or replicate the paper's experiments. You can download all the additional necessary data from from https://worksheets.codalab.org/worksheets/0x8684ad8e95e24c4d80074278bce37ba4/ except specified otherwise.

**Step 1**: Clone the entGraph project

**Step 2**: Download and add lib and lib_data folders inside the entGraph folder.

**Step 3**: Create a folder called freebase_types inside the entGraph folder and copy entity2Types.txt to it.

**Step 4**: You can simply download the linked and parsed NewsSpike corpus (NewsSpike_CCG_parsed.json) to your preferred location and skip to step 5, or do steps 4.1 to 4.5 to parse and link the NewsSpike corpus (or your own corpus) into predicate argument structure using the graph-parser (developped by Siva Reddy) based on CCG parser (easyCCG).

**Step 4.1**: Download the NewsSpike Corpus from https://www.cs.washington.edu/node/9473/ and copy the data folder inside entGraph.
   
**Step 4.2**: Split the input json file line by line: run entailment.Util.convertReleaseToRawJson(inputJsonAddress) 1>rawJsonAddress (by changing Util's main function), where inputJsonAddress should be by default "data/release/crawlbatched". Run the code as "java -cp lib/*:bin entailment.Util "data/release/crawlbatched" 1>news_raw.json"

**Step 4.3**: Extract binary relations from the input json file: Run the bash script: prArgs.sh (This takes about 12 hours on the servers I used with 20 threads.) Change the input and output address as necessary.

The number of threads is a parameter which might need to be changed in constants.ConstantsParsing. Please keep the other parameters unchanged.

example:

fName=news_raw.json
oName1=predArgs_gen.txt (binary relations with at least one Named Entity argument, which is used in our experiments).
oName2=predArgs_NE.txt (binary relations with two NE arguments).

**Step 4.4**: Download news_linked.json and put it in folder aida. This is the output of NE linking (In our experiments, we used AIDALight).

**Step 4.5**: Run entailment.Util (function convertPredArgsToJson) with these arguments: predArgs_gen.txt true true 12000000 aida/news_linked.json 1>news_gen.json

predArgs_gen.txt: output of step 4.3.
aida/news_linked.json: output of step 4.4.
120000000 is an upper bound on the number of lines of the corpus (this might need to be changed for a new corpus). For larger corpora, instead of convertPredArgsToJson, you can use convertPredArgsToJsonUnsorted which will get less memory, but the output isn't sorted (this doesn't change any of the results for this paper).

**Step 5**: Extract the interim outputs:

You might need to set a few parameters in constants.ConstantsAgg:

  1. minArgPairForPred is C_1 in the paper, which is set to 3 by default.

  2. minPredForArgPair is C_2 in the paper, which is set to 3 by default.

  3. relAddress is the output of step 4.

  4. simsFolder is where the final output will be stored.

You need to run the entailment.vector.EntailGraphFactoryAggregator using:

java -Xmx100G -cp lib/*:bin  entailment.vector.EntailGraphFactoryAggregator

**Step 6**: The global learning: Run graph.softConst.TypePropagateMN. A few parameters might need to be set in constants.ConstantsGraphs as follows:

  1. featName is the feature name to be used, which is by default BINC score.
  2. root is the folder address storing the output of step 5.
  3. constants.ConstantsSoftConst:

A few more parameters in constants.ConstantsSoftConst:

  1. numThreads, which I set that to 60 for a machine with 20 cpus, because not all the threads will run together. But you might need to change it.
  2. numIters is the number of iterations. lambda, lambda_2 and tau are set by default for Cross-Graph + Paraphrase-Resolution global soft constraints experiments, but can be tuned for another dataset.

**Step 7**: Please follow the instructions outlined in xxx to test the graphs on the entailment datasets.
