public class SelectionSort extends Sort {
    @Override
    public void sort(int[] array) {
        for (int i = 0; i < array.length; i++) {
            int min = array[i];
            int index = i;
            for (int j = i; j < array.length; j++) {
                comparisons++;
                if (array[j] < min) {
                    min = array[j];
                    index = j;
                }
            }
            if (index != i) {
                swap(array, i, index);
                interchanges++;
            }
        }
    }
}
