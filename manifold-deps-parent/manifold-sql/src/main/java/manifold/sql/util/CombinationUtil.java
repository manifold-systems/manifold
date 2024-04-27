package manifold.sql.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// See https://github.com/hmkcode/Java/tree/master/java-combinations
public class CombinationUtil {

    public static <T> List<List<T>> createAllCombinations(T[] elements) {
        List<List<T>> results = new ArrayList<>();
        results.add(Collections.emptyList());
        createAllCombinations(elements, results);
        return results;
    }

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
