package ch.uzh.ifi.ce.cabne.bundelgenerator;

import java.util.ArrayList;
import java.util.HashMap;

public class BundleGenerator {

    private final HashMap<Integer, int[]> bundles;

    private ArrayList<ArrayList<Integer>> maxFeasibleAllocations;
    private final int overbiddingKey = 6;

    /**
     * Constructor for the auction setup.
     *
     * @param nrPlayers The number of bidders.
     * @param nrItems The number of items.
     * @param probabilityItems The probability for each item to be of interest for a bidder.
     */
    public BundleGenerator(int nrPlayers, int nrItems, double probabilityItems) {
        this.bundles = generateBundlesEvenly(nrPlayers, nrItems, probabilityItems);
        this.maxFeasibleAllocations = calculateMaxFeasibleAllocations();
        cutNoInterestBidders();
    }

    /**
     * Constructor if the bundles are already known.
     *
     * @param bundles All bundles mapped to their bidder.
     */
    public BundleGenerator(HashMap<Integer, int[]> bundles, ArrayList<ArrayList<Integer>> maxFeas) {
        this.bundles = bundles;
        this.maxFeasibleAllocations = maxFeas;
        cutNoInterestBidders();
    }

    /**
     * Removes all bidders with no interest at all from maxFeasibleAllocs
     */
    private void cutNoInterestBidders() {
        ArrayList<Integer> noInterestBidders = new ArrayList<>();
        bundles.forEach((key, value) -> {
            int valsum = 0;
            for (int val : value) {
                valsum += val;
            }
            if (valsum == 0) {
                noInterestBidders.add(key);
            }
        });
        for (int noInterest : noInterestBidders) {
            for (ArrayList<Integer> alloc : maxFeasibleAllocations) {
                if (alloc.contains(noInterest)) {
                    alloc.remove((Integer) noInterest);
                }
            }
        }
    }

    /**
     * Generates a List of all opponents of a player
     *
     * @param player The player considered
     * @param nrPlayers The number of players in the auction
     * @return A List of all players, except the player considered
     */
    public static ArrayList<Integer> getOpponents(int player, int nrPlayers) {
        ArrayList<Integer> allPlayers = new ArrayList<>();
        for (int i = 0; i < nrPlayers; i++) {
            allPlayers.add(i);
        }
        ArrayList<Integer> opponents_i = new ArrayList<>();

        for (int i = 0, k = 0; i < allPlayers.size(); i++) {
            if (i == player - 1) {
                continue;
            }
            opponents_i.add(allPlayers.get(i));
        }
        return opponents_i;
    }

    /**
     * Generates bundles for all players that contains each item available by a probability.
     *
     * @param nrPlayers The number of players in the auction.
     * @param nrItems The number of items that are available in the auction.
     * @param probabilityItemChosen The probability for each item to be in the bundle of a player.
     */
    public HashMap<Integer, int[]> generateBundlesEvenly(int nrPlayers, int nrItems, double probabilityItemChosen) {
        HashMap<Integer, int[]> bundleDict = new HashMap<>();  // where all bundles are stored

        // Create a bundle for each player
        for (int currentPlayer = 0; currentPlayer < nrPlayers; currentPlayer++) {
            int[] currentBundle = new int[nrItems];
            // Chose each item by the probability probabilityItemChosen
            for (int i = 0; i < nrItems; i++) {
                double toss = Math.random();
                if (toss < probabilityItemChosen) {
                    currentBundle[i] = 1;
                } else {  // item is not chosen
                    currentBundle[i] = 0;
                }
            }
            bundleDict.put(currentPlayer, currentBundle);  // Add the bundle to the player
        }

        return bundleDict;
    }

    /**
     * Generates all distinct combination of all players (2^(n-1) possibilities)
     *
     * @param nrPlayers The number of players in the auction
     * @return All 2^(n-1) distinct combinations of all players
     */
    private static ArrayList<ArrayList<Integer>> getBundleCombinations(int nrPlayers) {
        // Create Array that contains all players (numerated)
        Integer[] arr = new Integer[nrPlayers];
        for (int i = 0; i < nrPlayers; i++) {
            arr[i] = i;
        }

        // Set N as # of all possible distinct combinations of all players
        int N = (int) Math.pow(2d, nrPlayers);
        ArrayList<ArrayList<Integer>> allBundleCombs = new ArrayList<ArrayList<Integer>>();

        // Iterate over all binary codes and for each create an ArrayList
        for (int i = 1; i < N; i++) {
            ArrayList<Integer> currentList = new ArrayList<>();
            String code = Integer.toBinaryString(N | i).substring(1);
            for (int j = 0; j < nrPlayers; j++) {
                if (code.charAt(j) == '1') {
                    currentList.add(arr[j]);
                }
            }
            allBundleCombs.add(currentList);
        }

        return allBundleCombs;
    }

    /**
     * Status for the processing of the lattice.
     */
    public enum Status {
        maximal,
        notProcessed,
        processed
    }

    /**
     * Checks if in the allocation comb no item is allocated more than once.
     *
     * @param comb The combination of winning bidders.
     * @return True if every item is not allocated more than once.
     */
    private boolean checkFeasible(ArrayList<Integer> comb) {
        int nrItems = bundles.get(0).length;
        int feasibleItems = 0;

        for (int item = 0; item < nrItems; item++) {
            int itemInterestCount = 0;
            for (Integer player : comb) {  // if the item is of interest for player, then add it to the count
                itemInterestCount += bundles.get(player)[item];  // Get players bundle and add value of current item to the count
            }
            if (itemInterestCount > 1) {
                return false;
            } else {
                feasibleItems += 1;
            }
        }
        return feasibleItems == nrItems;
    }

    /**
     * Calculates all maximal feasible allocation.
     *
     * @return All maximal feasible allocations.
     */
    private ArrayList<ArrayList<Integer>> calculateMaxFeasibleAllocations() {
        int nrPlayers = bundles.size();
        ArrayList<ArrayList<Integer>> feasibleAllocs = new ArrayList<>();
        ArrayList<ArrayList<Integer>> combs = getBundleCombinations(nrPlayers);

        // initialize lengthMap to iterate through later
        HashMap<Integer, ArrayList<ArrayList<Integer>>> lengthMap = new HashMap<>();
        for (int iter = 1; iter <= nrPlayers; iter++) {
            lengthMap.put(iter, new ArrayList<>());
        }

        // initialize statusMap by setting all status to notProcessed
        HashMap<ArrayList<Integer>, Status> statusMap = new HashMap<>();
        for (ArrayList<Integer> comb : combs) {
            statusMap.put(comb, Status.notProcessed);
            lengthMap.get(comb.size()).add(comb);
        }

        // iterate over all lengths from largest to lowest
        for (int iter = nrPlayers; iter > 0; iter--) {
            int lengthSize = lengthMap.get(iter).size();
            for (ArrayList<Integer> currentComb : lengthMap.get(iter)) {
                if (statusMap.get(currentComb) == Status.notProcessed) {
                    if (checkFeasible(currentComb)) {  // test if feasible | if not, set processed/if feasible, set maximal and set all from its lattice to processed
                        feasibleAllocs.add(currentComb);
                        statusMap.put(currentComb, Status.maximal);
                        processDownwardsLattice(currentComb, statusMap);
                    } else {
                        statusMap.put(currentComb, Status.processed);
                    }
                } else {
                    lengthSize -= 1;
                    if (lengthSize == 0) {
                        return feasibleAllocs;
                    }
                }
            }
        }
        return feasibleAllocs;
    }

    /**
     * Calculates maximal feasible allocations that are added to the current maximal feasible allocations
     * if the overbidding bundle is added to the auction.
     *
     * @param oBidder The current overbidding Bidder.
     * @param oBundle The bundle the bidder potentially places a bid on.
     * @return A list of maximal feasible allocations additionally generated by the overbidding bundle.
     */
    // checks for each maxfeasalloc if overbidder is in current alloc, if yes -> make new array, remove overbidder, add overbidderkey (for bundlehashmap), then check if this is feasible, if yes add it to overbiddignmaxfeasallocs
    // if none is feasible add plain overbidderkey to overbiddingmaxfeasibleallocs
    public ArrayList<ArrayList<Integer>> calculateOverbiddingMaxFeasibleAllocs(int oBidder, int[] oBundle) {
        bundles.put(overbiddingKey, oBundle);
        int index;
        ArrayList<ArrayList<Integer>> overbiddingMaxFeasibleAllocations = new ArrayList<>();
        for (ArrayList<Integer> alloc : maxFeasibleAllocations) {
            if (alloc.contains(oBidder)) {
                ArrayList<Integer> currentOverbiddingAlloc = new ArrayList<>(alloc);
                index = currentOverbiddingAlloc.indexOf(oBidder);
                currentOverbiddingAlloc.remove(index);
                currentOverbiddingAlloc.add(overbiddingKey);
                boolean isFeasible = checkFeasible(currentOverbiddingAlloc);
                if (isFeasible) {
                    overbiddingMaxFeasibleAllocations.add(currentOverbiddingAlloc);
                }
            }
        }
        if (overbiddingMaxFeasibleAllocations.isEmpty()) {
            overbiddingMaxFeasibleAllocations.add(new ArrayList<Integer>() {{add(overbiddingKey);}});
        }
        return overbiddingMaxFeasibleAllocations;
    }

    // precondition - all allocations are ordered from lowest to highest number (e.g. [4,2] not possible -> must be [2,4])

    /**
     * Checks from top to bottom all allocations for feasibility and maximality. As soon as a feasible allocation is found
     * all its subsets are illegal, since they cannot be maximal. Terminates as soon as there is no allocation that has
     * not been processed.
     *
     * @param comb
     * @param map The map containing all allocations with their initial status notProcessed
     */
    private static void processDownwardsLattice(ArrayList<Integer> comb, HashMap<ArrayList<Integer>, Status> map) {
        int sum = comb.size();

        ArrayList<String> lower = new ArrayList<>();
        if (sum > 1) {
            lower = generateBins(sum);
        }

        // replace ones with each substring to create children in hypercube
        for (String i : lower) {
            int incr = 0;
            ArrayList<Integer> currentBundle = new ArrayList<>();
            // iterate over binary code - within: replace 1s with substrings
            for (int playersInAlloc = 0; playersInAlloc < sum; playersInAlloc++) {
                if (Integer.parseInt(String.valueOf(i.charAt(incr))) == 1) {  // iterate 0 to sum-1 - get i.charAt(iterate) -> if 1 then add the iterate-th nr in the comb_code to the currentBundle
                    currentBundle.add(comb.get(incr));
                }
                incr += 1;
            }
            map.put(currentBundle, Status.processed);
        }
    }

    /**
     * Generates all distinct binary strings of length size.
     *
     * @param size The length of the binary strings
     * @return A list of binary strings
     */
    private static ArrayList<String> generateBins(int size) {
        // Set N as number of all possible distinct combinations of all players
        int N = (int) Math.pow(2d, size);

        // Iterate over all binary codes and for each create an ArrayList
        ArrayList<String> res = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            String code = Integer.toBinaryString(N | i).substring(1);
            res.add(code);
        }
        res.remove(N - 1);
        res.remove(0);
        return res;
    }

    /**
     * Generates all bundles for which the parameter bundle is a subset of.
     *
     * @param bundle The base bundle
     * @return A HashMap containing all supersets of bundle.
     */
    public HashMap<Integer, int[]> generateParentBundles(int[] bundle) {
        HashMap<Integer, int[]> parentBundles = new HashMap<>();
        int nrItems = bundle.length;
        int nrZeros = 0;
        for (int i : bundle) {
            if (i == 0) {
                nrZeros++;
            }
        }
        ArrayList<String> bins = generateBins(nrZeros);
        int nrBundle = 0;
        for (String bin : bins) {
            int incr = 0;
            int[] currentBundle = new int[nrItems];
            for (int index=0; index<bundle.length; index++) {
                if (bundle[index] == 1) {
                    currentBundle[index] = 1;
                } else {
                    currentBundle[index] = Integer.parseInt(String.valueOf(bin.charAt(incr)));
                    incr++;
                }
            }
            parentBundles.put(nrBundle, currentBundle);
            nrBundle++;
        }
        int[] fullBundle = new int[nrItems];
        for (int i=0; i<nrItems; i++) {
            fullBundle[i] = 1;
        }
        parentBundles.put(nrBundle, fullBundle);
        return parentBundles;
    }

    public HashMap<Integer, int[]> getBundles() {
        return this.bundles;
    }

    public ArrayList<ArrayList<Integer>> getMaxFeasibleAllocations() {
        return this.maxFeasibleAllocations;
    }
}
