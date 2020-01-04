package ch.uzh.ifi.ce.cabne.BR;

import ch.uzh.ifi.ce.cabne.algorithm.BNESolverContext;
import ch.uzh.ifi.ce.cabne.helpers.UtilityHelpers;
import ch.uzh.ifi.ce.cabne.pointwiseBR.Optimizer;
import ch.uzh.ifi.ce.cabne.strategy.Strategy;
import ch.uzh.ifi.ce.cabne.strategy.UnivariatePWLOverbiddingStrategy;
import ch.uzh.ifi.ce.cabne.strategy.UnivariatePWLStrategy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;

public class PWLOverbiddingBRCalculator implements BRCalculator<Double, Double[]> {
	private String outputFile;
	BNESolverContext<Double, Double[]> context;


	public PWLOverbiddingBRCalculator(BNESolverContext<Double, Double[]> context, String outputFile) {
		this.context = context;
		this.outputFile = outputFile;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public Result<Double, Double[]> computeBR(int i, List<Strategy<Double, Double[]>> s) throws IOException {
		int nPoints = 5;
//		int nPoints = Integer.parseInt(context.config.get("gridsize"));
		
		TreeMap<Double, Double[]> pointwiseBRs = new TreeMap<>();
		TreeMap<Double, Double> pointwiseUtility = new TreeMap<>();
		double epsilonAbs = 0.0;
		double epsilonRel = 0.0;

		double maxValue = s.get(i).getMaxValue();
		
		for (int j = 0; j<=nPoints; j++) {
			double v = maxValue * ((double) j) / (nPoints);
			Double[] oldbid = s.get(i).getBid(v);
			Optimizer.Result<Double[]> result = context.optimizer.findBR(i, v, oldbid, s);
			epsilonAbs = Math.max(epsilonAbs, UtilityHelpers.absoluteLoss(result.oldutility, result.utility));  // utility in string builder und dann ausschreiben
			epsilonRel = Math.max(epsilonRel, UtilityHelpers.relativeLoss(result.oldutility, result.utility));

			Double[] newbids = result.bid;
			pointwiseBRs.put(v, newbids);
			pointwiseUtility.put(v, result.utility);
		}

//		System.out.println("value utility bid1 bid2");

		System.out.println("debug");

		File brStratFile = new File(outputFile);
		FileWriter fileWriter = new FileWriter(brStratFile, true);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

		try {
			bufferedWriter.write(String.valueOf(i));
			bufferedWriter.newLine();
			bufferedWriter.write("overbidding bundleSet: "); // todo: write bundle
			bufferedWriter.newLine();
			pointwiseBRs.forEach((key, value) -> {
				try {  // 'value utility bid1 bid2  value utility bid1 bid2'
					bufferedWriter.write(key + " " + pointwiseUtility.get(key) + " " + value[0] + " " + value[1] + "  ");
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		bufferedWriter.close();
		fileWriter.close();
		
		return new Result<>(new UnivariatePWLOverbiddingStrategy(true, pointwiseBRs), epsilonAbs, epsilonRel);
	}
}
