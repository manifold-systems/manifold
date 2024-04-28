package manifold.sql.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CombinationUtil {

    private CombinationUtil(){
        // hide utility class constructor
    }

    /**
     * Create all possible combinations with the provided elements.
     * A combinations follows the following rules:
     * <ul>
     *     <li>It doesn't need to use all provided elements. Even an empty list is a valid case.</li>
     *     <li>Only its elements are important, not the order of the elements. [1, 2] is the same as [2, 1] and will only be included once.</li>
     * </ul>
     *
     * <p>
     * Example:
     * <ul>
     *     <li>input: {1, 2, 3}</li>
     *     <li>output: [], [1], [2], [3], [1, 2], [1, 3], [2, 3], [1, 2, 3]</li>
     * </ul>
     * @param elements the input elements
     * @return a list of all possible combinations
     * @param <T> the type of the elements
     */
    public static <T> List<List<T>> createAllCombinations(T[] elements) {
        List<List<T>> results = new ArrayList<>();
        results.add(Collections.emptyList());
        createAllCombinations(elements, results);
        return results;
    }

    /**
     * Helper class to create the combinations. See <a href="https://github.com/hmkcode/Java/tree/master/java-combinations">here</a>
     * @param elements the elements
     * @param results the result list.
     * @param <T>  the type of the elements
     */
    private static <T> void createAllCombinations(T[] elements, List<List<T>> results) {
        int n = elements.length;
        for (int k = 1; k <= elements.length; k++) {
            // init combination index array
            int[] combination = new int[k];
            int r = 0; // index for combination array
            int i = 0; // index for elements array
            while (r >= 0) {
                // forward step if i < (N + (r-K))
                if (i <= (n + (r - k))) {
                    combination[r] = i;
                    // if combination array is full print and increment i;
                    if (r == k - 1) {
                        results.add(combinationAsList(combination, elements));
                        i++;
                    } else {
                        // if combination is not full yet, select next element
                        i = combination[r] + 1;
                        r++;
                    }
                }
                // backward step
                else {
                    r--;
                    if (r >= 0) {
                        i = combination[r] + 1;
                    }
                }
            }
        }
    }

    private static <T> List<T> combinationAsList(int[] combination, T[] elements) {
        return Arrays.stream(combination).mapToObj(i -> elements[i]).collect(Collectors.toList());
    }

}
