package ch.uzh.ifi.ce.cabne.bundelgenerator;

import java.util.ArrayList;
import java.util.HashMap;

public class BundleGenerator {

    private final HashMap<Integer, int[]> bundles;

    private final ArrayList<ArrayList<Integer>> maxFeasibleAllocations;
    private final int overbiddingKey = 6;

    public BundleGenerator(int nr_players, int nr_items, double probability_items) {
        this.bundles = generateBundlesEvenly(nr_players, nr_items, probability_items);
        this.maxFeasibleAllocations = calculateMaxFeasibleAllocations();
        cutNoInterestBidders();
    }

    public BundleGenerator(HashMap<Integer, int[]> bundles) {
        this.bundles = bundles;
        this.maxFeasibleAllocations = calculateMaxFeasibleAllocations();
        cutNoInterestBidders();
    }

    // removes all players with no interest from max_feasible_allocs
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

    public HashMap<Integer, int[]> getBundles() {
        return this.bundles;
    }

    public ArrayList<ArrayList<Integer>> getMaxFeasibleAllocations() {
        return this.maxFeasibleAllocations;
    }

    /**
     * Generates a List of all opponents of a player
     *
     * @param player     The player considered
     * @param nr_players The number of players in the auction
     * @return A List of all players, except the player considered
     */
    public static ArrayList<Integer> get_opponents(int player, int nr_players) {
        ArrayList<Integer> all_players = new ArrayList<>();
        for (int i = 0; i < nr_players; i++) {
            all_players.add(i);
        }
        //int[] opponents_i = new int[all_players.size() - 1];
        ArrayList<Integer> opponents_i = new ArrayList<>();

        for (int i = 0, k = 0; i < all_players.size(); i++) {
            if (i == player - 1) {
                continue;
            }
            //opponents_i[k++] = all_players.get(i);
            opponents_i.add(all_players.get(i));
        }
        return opponents_i;
    }


    /**
     * Generates bundles for all players that contains each item available by a probability.
     *
     * @param nr_players              The number of players in the auction
     * @param nr_items                The number of items that are available in the auction
     * @param probability_item_chosen The probability for each item to be in the bundle of a player
     */
    public HashMap<Integer, int[]> generateBundlesEvenly(int nr_players, int nr_items, double probability_item_chosen) {
        HashMap<Integer, int[]> bundle_dict = new HashMap<>();  // where all bundles are stored

        // create bundle for each player
        for (int current_player = 0; current_player < nr_players; current_player++) {
            int[] current_bundle = new int[nr_items];
            // chose each item by the probability probability_item_chosen
            for (int i = 0; i < nr_items; i++) {
                double toss = Math.random();  // toss = random between 0.0 and 1.0
                if (toss < probability_item_chosen) {  // item is chosen
                    current_bundle[i] = 1;
                } else {  // item is not chosen
                    current_bundle[i] = 0;
                }
            }
            bundle_dict.put(current_player, current_bundle);  // Add the bundle to the player
        }

//        bundle_dict.forEach((key, value) -> System.out.println(key + ": " + Arrays.toString(value)));
        return bundle_dict;
    }


    /**
     * Generates all distinct combination of all players (2^n-1 possibilities)
     *
     * @param nr_players The number of players in the auction
     * @return All 2^-1 distinct combinations of all players
     */
    private static ArrayList<ArrayList<Integer>> getBundleCombinations(int nr_players) {
        // Create Array that contains all players (numerated)
        Integer[] arr = new Integer[nr_players];
        for (int i = 0; i < nr_players; i++) {
            arr[i] = i;
        }

        // Set N as # of all possible distinct combinations of all players
        int N = (int) Math.pow(2d, nr_players);
        ArrayList<ArrayList<Integer>> all_bundle_combs = new ArrayList<ArrayList<Integer>>();

        // Iterate over all binary codes and for each create an ArrayList
        for (int i = 1; i < N; i++) {
            ArrayList<Integer> currentList = new ArrayList<>();
            String code = Integer.toBinaryString(N | i).substring(1);
            for (int j = 0; j < nr_players; j++) {
                if (code.charAt(j) == '1') {
                    currentList.add(arr[j]);
                }
            }
            all_bundle_combs.add(currentList);
        }
        return all_bundle_combs;
    }

    public enum Status {
        maximal,
        not_processed,
        processed
    }

    private boolean checkFeasible(ArrayList<Integer> comb) {
        int nr_items = bundles.get(0).length;
        int feasible_items = 0;

        for (int item = 0; item < nr_items; item++) {
            int item_interest_count = 0;
            for (Integer player : comb) {  // if the item is of interest for player, then add it to the count
                item_interest_count += bundles.get(player)[item];  // Get players bundle and add value of current item to the count
            }
            if (item_interest_count > 1) {
                return false;
            } else {
                feasible_items += 1;
            }
        }
        return feasible_items == nr_items;
    }

    private ArrayList<ArrayList<Integer>> calculateMaxFeasibleAllocations() {
        int nr_players = bundles.size();
        ArrayList<ArrayList<Integer>> feasible_allocs = new ArrayList<>();
        ArrayList<ArrayList<Integer>> combs = getBundleCombinations(nr_players);

        // initialize length map - to iterate through
        HashMap<Integer, ArrayList<ArrayList<Integer>>> length_map = new HashMap<>();
        for (int iter = 1; iter <= nr_players; iter++) {
            length_map.put(iter, new ArrayList<>());
        }

        // initialize status_map - setting all status to not_processed
        HashMap<ArrayList<Integer>, Status> status_map = new HashMap<>();
        for (ArrayList<Integer> comb : combs) {
            status_map.put(comb, Status.not_processed);
            length_map.get(comb.size()).add(comb);
        }

        // iterate over all lengths from largest to lowest
        for (int iter = nr_players; iter > 0; iter--) {
            int length_size = length_map.get(iter).size();
            for (ArrayList<Integer> current_comb : length_map.get(iter)) {
                if (status_map.get(current_comb) == Status.not_processed) {
                    if (checkFeasible(current_comb)) {  // test if feasible | if not, set processed/if feasible, set maximal and set all from its lattice to processed
                        feasible_allocs.add(current_comb);
                        status_map.put(current_comb, Status.maximal);
                        processDownwardsLattice(current_comb, status_map);
                    } else {
                        status_map.put(current_comb, Status.processed);
                    }
                } else {
                    length_size -= 1;
                    if (length_size == 0) {
                        return feasible_allocs;
                    }
                }
            }
        }
        return feasible_allocs;
    }

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
    private static void processDownwardsLattice(ArrayList<Integer> comb, HashMap<ArrayList<Integer>, Status> map) {
        int sum = comb.size();

        ArrayList<String> lower_ = new ArrayList<>();
        if (sum > 1) {
            lower_ = generateBins(sum);
        }

        // replace ones with each substring to create children in hypercube
        for (String i : lower_) {
            int incr = 0;
            ArrayList<Integer> current_bundle = new ArrayList<>();
            // iterate over binary code - within: replace 1s with substrings
            for (int pl_in_alloc = 0; pl_in_alloc < sum; pl_in_alloc++) {
                if (Integer.parseInt(String.valueOf(i.charAt(incr))) == 1) {  // iterate 0 to sum-1 - get i.charAt(iterate) -> if 1 then add the iterate-th nr in the comb_code to the current_bundle
                    current_bundle.add(comb.get(incr));
                }
                incr += 1;
            }
            map.put(current_bundle, Status.processed);
        }
    }

    private static ArrayList<String> generateBins(int size) {
        // Set N as # of all possible distinct combinations of all players
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
}
