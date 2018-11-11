This is the implementation for the following paper:

Learning Typed Entailment Graphs with Global Soft Constraints, Mohammad Javad Hosseini, Nathanael Chambers, Siva Reddy, Xavier Holt, Shay B. Cohen, Mark Johnson, and Mark Steedman. Transactions of the Association for Computational Linguistics (TACL 2018).

### Setting up the code

Please follow the below instructions to create entailment graphs and/or replicate the paper's experiments. You can download all the additional necessary data from from https://worksheets.codalab.org/worksheets/0x8684ad8e95e24c4d80074278bce37ba4/ except specified otherwise.

Step 1) Clone the entGraph project

Step 2) Download and add lib and lib_data folders inside the entGraph folder.

3. Create a folder called freebase_types inside the entGraph folder and copy entity2Types.txt to it.

4. You can simply download NewsSpike_CCG_parsed.json to your preferred location and skip to step xxx, or do steps xxx to xxx  to parse the NewsSpike corpus (or your own corpus) into predicate argument structure using the graph-parser (developped by Siva Reddy) based on CCG parser (easyCCG).

5. Download the NewsSpike Corpus from https://www.cs.washington.edu/node/9473/ and copy the data folder inside entGraph.
   
6. Split the input json file line by line: run entailment.Util.convertReleaseToRawJson(inputJsonAddress) 1>rawJsonAddress (by changing Util's main function), where inputJsonAddress should be by default "data/release/crawlbatched". Run the code as "java -cp lib/*:bin entailment.Util "data/release/crawlbatched" 1>news_raw.json"

7) Extract binary relations from the input json file: Run the bash script: prArgs.sh (This takes about 12 hours on the servers I used with 20 threads.)
Change the input and output address as necessary

example:

fName=news_raw.json
oName1=predArgs_gen.txt (binary relations with at least one Named Entity argument, which is used in our experiments).
oName2=predArgs_NE.txt (binary relations with two NE arguments).

4)  Run entailment.Util (function convertPredArgsToJsonUnsorted) with these arguments: predArgsC_gen.txt true true 120000000 aida/newsC_linked.json 1>newsC_gen.json

predArgsC_gen.txt: output of 2
aida/news_linked.json: output of NE linking
/disk/data/darkstar2/s1583634/java/graph-parser/aida/newsC_linked.json

Leave the rest as it is.

Forward the output to newsC_gen.json

B) Extract the interim outputs

You need to run the entailment.vector.EntailGraphFactoryAggregator using:

java -Xmx100G -cp lib/*:bin  entailment.vector.EntailGraphFactoryAggregator

I've set the parameters at the top of the file (it was more convenient at the time to have them there :) ). You can see two cutoffs that are set to 40, but of course you can change them. For NewsSpike, I had them 3, for news crawl, it could be more.

relAddress is the output of the A.4 step. You should change that.
simsFolder is where the final output will be stored
