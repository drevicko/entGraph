This is the implementation for the following paper:

Learning Typed Entailment Graphs with Global Soft Constraints, Mohammad Javad Hosseini, Nathanael Chambers, Siva Reddy, Xavier Holt, Shay B. Cohen, Mark Johnson, and Mark Steedman, Transactions of the Association for Computational Linguistics (TACL 2018).

### Setting up the code

After cloning the entGraph project, you need to put these folders inside the entGraph folder:

https://worksheets.codalab.org/bundles/0x21ba95ba21834dee8940c7cb69baa3cb/
https://worksheets.codalab.org/bundles/0x91c4fc781be342d19c2acb4d59058d83/


You should also create a folder called freebase_types inside the entGraph folder and copy this file to it:

https://worksheets.codalab.org/bundles/0x9060f43f11e444d8b8ee22d7956e03f7/


A) Extract binary relations from the input json file:

1) Split the input json file line by line:

run Util.convertReleaseToRawJson(inputJsonAddress) 1> rawJsonAddress by changing its main function.

for example, java -cp lib/*:bin convertReleaseToRawJson("data/release/crawlbatched") 1>newsC_raw.json

2) run this bash script: /disk/data/darkstar2/s1583634/java/graph-parser/prArgs2.sh

Change the input and output address as necessary

example:

fName=news_rawC.json
oName1=predArgsC_gen.txt (binary relations with at least one Named Entity argument).
oName2=predArgsC_NE.txt (binary relations with two NE arguments)

I usually use oName1 output, but you might want to use oName2.

4) Run entailment.Util.java (function convertPredArgsToJsonUnsorted) with these arguments: predArgsC_gen.txt true true 120000000 aida/newsC_linked.json 1>newsC_gen.json

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
