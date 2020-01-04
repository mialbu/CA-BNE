package ch.uzh.ifi.ce.cabne.algorithm;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

import ch.uzh.ifi.ce.cabne.BR.BRCalculator;
import ch.uzh.ifi.ce.cabne.strategy.Strategy;
import ch.uzh.ifi.ce.cabne.verification.Verifier;

public class BNEAlgorithm<Value, Bid> {
	public static class Result<Value, Bid> {
		public double epsilon;
		public List<Strategy<Value, Bid>> equilibriumStrategies;
		
		public Result(double epsilon, List<Strategy<Value, Bid>> equilibriumStrategies) {
			this.epsilon = epsilon;
			this.equilibriumStrategies = equilibriumStrategies;
		}
	}

	public enum IterationType {INNER, OUTER, VERIFICATION};
	
	private int nBidders;
	private ArrayList<Integer> playingBidders;
	private boolean overbidding;
	private BNESolverContext<Value, Bid> context;

	Map<Integer, Strategy<Value, Bid>> initialStrategies = new HashMap<>();
	
	int[] canonicalBidders;
	boolean[] updateBidder;
	

	private BNEAlgorithmCallback<Value, Bid> callback; 
	
	public BNEAlgorithm(int nBidders, BNESolverContext<Value, Bid> context) {
		this.nBidders = nBidders;
		this.playingBidders = new ArrayList<>();
		for (int i = 0; i < nBidders; i++) {
			playingBidders.add(i);
		}
		overbidding = false;
		canonicalBidders = new int[nBidders];
		updateBidder = new boolean[nBidders];
		for (int i=0; i<nBidders; i++) {
			canonicalBidders[i] = i;
			updateBidder[i] = true;
		}
		this.context = context;
	}

	public BNEAlgorithm(int nBidders, ArrayList<Integer> playingBidders, BNESolverContext<Value, Bid> context) {
		this.nBidders = nBidders;
		this.playingBidders = playingBidders;
		overbidding = true;
		canonicalBidders = new int[nBidders];
		updateBidder = new boolean[nBidders];
		for (int i=0; i<nBidders; i++) {
			canonicalBidders[i] = i;
			updateBidder[i] = true;
		}
		this.context = context;
	}

	public void setContext(BNESolverContext<Value, Bid> context) {
		this.context = context;
	}
	
	public void setCallback(BNEAlgorithmCallback<Value, Bid> callback) {
		this.callback = callback;
	}
	
	public void setInitialStrategy(int bidder, Strategy<Value, Bid> initialStrategy) {
		initialStrategies.put(bidder, initialStrategy);
	}
	
	public void makeBidderNonUpdating(int bidder) {
		updateBidder[bidder] = false;
	}
	
	public void makeBidderSymmetric(int bidder, int canonicalBidder) {
		if (canonicalBidders[canonicalBidder] != canonicalBidder) {
			throw new RuntimeException("Tried to add symmetric bidder but canonical bidder is not a primary bidder");
		}
		canonicalBidders[bidder] = canonicalBidder;
	}
	
	private void callbackAfterIteration(int iteration, IterationType type, List<Strategy<Value, Bid>> strategies, double epsilon) {
		if (callback != null) {
			callback.afterIteration(iteration, type, strategies, epsilon);
		}	
	}

	private double calculateBROfPlayingBidders(List<Strategy<Value, Bid>> strategies, BRCalculator<Value, Bid> brc, ArrayList<Integer> playingBidders) throws IOException {
		double highestEpsilon = 0.0;
		int gridsize = context.getIntParameter("gridsize");

		// compute best responses for players where this is needed
		Map<Integer, Strategy<Value, Bid>> bestResponseMap = new HashMap<>();
		for (int i : playingBidders) {
			if (canonicalBidders[i] == i && updateBidder[i]) {
				// this is a canonical bidder whose strategy should be updated
				BRCalculator.Result<Value, Bid> result = brc.computeBR(i, strategies);
				Strategy<Value, Bid> s = result.br;
				highestEpsilon = Math.max(highestEpsilon, result.epsilonAbs); // mb: if highest epsilon  is 0.000 -> all epsilons have to be 0.000 (since no negative number possible...)
//				System.out.println(result.epsilonAbs);
				bestResponseMap.put(i, s);
			}
		}
//		System.out.println();

		// update strategies in place
		for (int i : playingBidders) {
			if (updateBidder[i]) {
				strategies.set(i, bestResponseMap.get(canonicalBidders[i]));
			}
		}
		return highestEpsilon;
	}
	
	private double playOneRound(List<Strategy<Value, Bid>> strategies, BRCalculator<Value, Bid> brc, ArrayList<Integer> playingBidders) throws IOException {
		double highestEpsilon = 0.0;
		
		// compute best responses for players where this is needed
		Map<Integer, Strategy<Value, Bid>> bestResponseMap = new HashMap<>();
		for (int i : playingBidders) {
			if (canonicalBidders[i] == i && updateBidder[i]) {
				// this is a canonical bidder whose strategy should be updated
				BRCalculator.Result<Value, Bid> result = brc.computeBR(i, strategies);
				Strategy<Value, Bid> s = result.br;
				highestEpsilon = Math.max(highestEpsilon, result.epsilonAbs); // mb: if highest epsilon  is 0.000 -> all epsilons have to be 0.000 (since no negative number possible...)
//				System.out.println(result.epsilonAbs);
				bestResponseMap.put(i, s);
			}
		}
//		System.out.println();

		// update strategies in place
		for (int i : playingBidders) {
			if (updateBidder[i]) {
				strategies.set(i, bestResponseMap.get(canonicalBidders[i]));
			}
		}
		
		return highestEpsilon;
	}	
	
	private Result<Value, Bid> verify(List<Strategy<Value, Bid>> strategies, Verifier<Value, Bid> verifier) {
		double highestEpsilon = 0.0;
		int gridsize = context.getIntParameter("gridsize");
		
		// Convert strategies if needed (e.g. to PWC).
		// Note that if some player is non-updating (i.e. truthful), their strategy doesn't need to be converted.
		List<Strategy<Value, Bid>> sConverted = new ArrayList<>();
		for (int i=0; i<nBidders; i++) {
			if (updateBidder[i]) {
				sConverted.add(verifier.convertStrategy(gridsize, strategies.get(i)));
			} else {
				sConverted.add(strategies.get(i));
			}
		}
		
		// compute best responses for players where this is needed
		for (int i=0; i<nBidders; i++) {
			if (canonicalBidders[i] == i && updateBidder[i]) {
				// this is a canonical bidder whose epsilon should be computed
				double epsilon = verifier.computeEpsilon(gridsize, i, strategies.get(i), sConverted);
				highestEpsilon = Math.max(highestEpsilon, epsilon);
			}
		}
		
		return new Result<>(highestEpsilon, sConverted);
	}

	public Result<Value, Bid> run() throws IOException {

		int maxIters = context.getIntParameter("maxiters");
		double targetEpsilon = context.getDoubleParameter("epsilon");
		double highestEpsilon = Double.POSITIVE_INFINITY;
		BRCalculator<Value, Bid> brc;
		
		// create list of strategies
		List<Strategy<Value, Bid>> strategies = new ArrayList<>();
		for (int i=0; i<nBidders; i++) {
			strategies.add(initialStrategies.get(canonicalBidders[i]));
		}

		context.activateConfig("innerloop"); // this allows the callback to assume some config is always active.

//		if (overbidding) {
//			context.activateConfig("verification");
//			callbackAfterIteration(0, IterationType.VERIFICATION, strategies, highestEpsilon);
//
//			Result<Value, Bid> result = verify(strategies, context.verifier, playingBidders);
//			callbackAfterIteration(0, IterationType.VERIFICATION, result.equilibriumStrategies, result.epsilon);
//			return result;
//		}

		callbackAfterIteration(0, IterationType.INNER, strategies, highestEpsilon);

		int iteration = 1;
		int lastOuterIteration = 1;
		
		while (iteration <= maxIters) {
			// This is the outer loop. First thing we do is go into the inner loop
			while (iteration <= maxIters) {
				// This is the inner loop.				
				context.activateConfig("innerloop");  // innerloop configs activated
				brc = context.brc;
				
				// Note that playOneRound updates the strategies in place.
				if (overbidding) {
					break;
				}
				highestEpsilon = playOneRound(strategies, brc, playingBidders);
				context.advanceRngs();
				
				callbackAfterIteration(iteration, IterationType.INNER, strategies, highestEpsilon);

				iteration++;
				if (highestEpsilon <= 0.8*targetEpsilon && iteration >= lastOuterIteration + 3) {
					break;
				}
			}
			
			if (iteration > maxIters) {
				return new Result<>(Double.POSITIVE_INFINITY, strategies);
			}
						
			lastOuterIteration = iteration;
			
			context.activateConfig("outerloop");  // outerloop configs activated
			brc = context.outerBRC;
			if (brc == null) {
				return new Result<>(Double.POSITIVE_INFINITY, strategies);
			}

			if (overbidding) {
				context.advanceRngs();
				break;
			}
			highestEpsilon = playOneRound(strategies, brc, playingBidders);
			context.advanceRngs();
			
			callbackAfterIteration(iteration, IterationType.OUTER, strategies, highestEpsilon);

			iteration++;
			if (highestEpsilon <= targetEpsilon) {
				break;
			}
		}


		context.activateConfig("verificationstep");  // verification configs activated
		
		if (overbidding) {
			brc = context.outerBRC;
			highestEpsilon = calculateBROfPlayingBidders(strategies, brc, playingBidders);  // no callback here - only after verification step
		}

		boolean outerloopConverged = highestEpsilon <= targetEpsilon;
		if ((context.verifier == null || !outerloopConverged)) {
			return new Result<>(Double.POSITIVE_INFINITY, strategies);
		}
		Result<Value, Bid> result = verify(strategies, context.verifier);
		callbackAfterIteration(iteration, IterationType.VERIFICATION, result.equilibriumStrategies, result.epsilon);
		return result;
    }
}
