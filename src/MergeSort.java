public class MergeSort extends Sort {
    @Override
    public void sort(int[] array) {
        mergeSort(array);
    }

    private void mergeSort(int[] array) {
        int length = array.length;
        if (length == 1) return;

        int[] rightArray = new int[length / 2];
        int[] leftArray = new int[length - rightArray.length];

        for (int i = 0; i < leftArray.length; i++) leftArray[i] = array[i];
        for (int i = 0; i < rightArray.length; i++) rightArray[i] = array[i + leftArray.length];

        mergeSort(leftArray);
        mergeSort(rightArray);
        merge(array, leftArray, rightArray);
    }

    private void merge(int[] array, int[] leftArray, int[] rightArray) {
        int i = 0, j = 0, k = 0;
        while (i < leftArray.length && j < rightArray.length) {
            comparisons++;
            if (leftArray[i] <= rightArray[j]) {
                array[k++] = leftArray[i++];
            } else {
                array[k++] = rightArray[j++];
            }
            interchanges++;
        }
        while (i < leftArray.length) { array[k++] = leftArray[i++]; interchanges++; }
        while (j < rightArray.length) { array[k++] = rightArray[j++]; interchanges++; }
    }
}
