public class BubbleSort extends Sort {
    @Override
    public void sort(int[] array) {
        for (int i = 0; i + 1 < array.length; i++) {
            for (int j = 0; j + 1 < array.length; j++) {
                comparisons++;
                if (array[j] > array[j + 1]) {
                    swap(array, j, j + 1);
                    interchanges++;
                }
            }
        }
    }
}
