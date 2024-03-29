package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import entailment.PredicateArgumentExtractor;
import entailment.Util;

public class GBooksToCCG implements Runnable{
	String path;
	String opath;
	int numThreads = 16;
	ThreadPoolExecutor threadPool;
	String root = "gbooks_dir/";

	public GBooksToCCG() {

	}

	public GBooksToCCG(String path, String opath) {
		this.path = path;
		this.opath = opath;
	}
	
	void extractRelationsCCGFromGBooks() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(root+path));
		PrintStream op = new PrintStream(new File(root+opath));
		String line = null;
		PredicateArgumentExtractor prExt = new PredicateArgumentExtractor(null);
		while ((line=br.readLine())!=null) {
			line = StringUtils.capitalize(line);
			String[] ss = line.split("\t");
			String sent = ss[0]+" "+ss[2]+" "+ss[1]+".";
//			System.out.println("sent: "+sent);
			String rel1 = null;
			try {
				rel1 = prExt.extractPredArgsStrsForceFinding(sent, ss[0], ss[1],false);
				if (rel1.equals("")) {
					continue;
				}
				String[] rel1ss = rel1.split(" ");
				
				String[] lemmas = Util.getPredicateLemma(rel1ss[0], true);
				rel1ss[0] = lemmas[0];
				
				if (lemmas[1].equals("false")) {
					rel1 = rel1ss[0] + " " + rel1ss[1] + " " + rel1ss[2]+" "+rel1ss[3]+" "+rel1ss[4];
				} else {
//					System.out.println("pred inverse:");
					rel1 = rel1ss[0] + " " + rel1ss[2] + " " + rel1ss[1]+" "+rel1ss[3]+" "+rel1ss[4];
				}
				
//				System.out.println(line+"\t"+rel1);
				op.println(line+"\t"+rel1);
			} catch (Exception e) {
//				System.err.println(rel1);
//				e.printStackTrace();
			}
		}
		br.close();
	}

	void normalizeOIERels() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(root+path));
		PrintStream op = new PrintStream(new File(root+opath));
		String line;
		while ((line = br.readLine()) != null) {
			String[] ss = line.split("\t");
			ss[0] = Util.normalizeArg(ss[0]);
			ss[1] = Util.normalizeArg(ss[1]);
			
			ss[2] = Util.getPredicateLemma(ss[2], false)[0];

			String normalized = ss[0] + "\t" + ss[1] + "\t" + ss[2] + "\t" + ss[3];
			op.println(normalized);
		}
		br.close();
		op.close();
	}

	void renewThreadPool() {
		final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(numThreads);
		threadPool = new ThreadPoolExecutor(numThreads, numThreads, 600, TimeUnit.SECONDS, queue);
		// to silently discard rejected tasks. :add new
		// ThreadPoolExecutor.DiscardPolicy()

		threadPool.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				// this will block if the queue is full
				try {
					executor.getQueue().put(r);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	void runAll(){
		for (int i = 0; i < numThreads; i++) {
			String path;
			String opath;
			if (i < 10) {
				path = "x0" + i;
			}
			else{
				path = "x" + i;
			}
			opath = "op"+i;

			Runnable normalizer = new GBooksToCCG(path,opath);
			threadPool.execute(normalizer);
			// entGrFact.run();
		}

		threadPool.shutdown();
		// Wait hopefully all threads are finished. If not, forget about it!
		try {
			threadPool.awaitTermination(200, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		GBooksToCCG s = new GBooksToCCG();
		s.renewThreadPool();
		s.runAll();
	}

	public void run() {
		try {
			extractRelationsCCGFromGBooks();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
