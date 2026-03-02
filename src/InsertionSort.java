public class InsertionSort extends Sort {
    @Override
    public void sort(int[] array) {
        for (int i = 1; i < array.length; i++) {
            int temp = i;
            for (int j = temp - 1; j >= 0; j--) {
                comparisons++;
                if (array[j] > array[temp]) {
                    swap(array, j, temp);
                    interchanges++;
                    temp--;
                }
            }
        }
    }
}
