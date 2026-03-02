public class HeapSort extends Sort {
    int heapLength;

    @Override
    public void sort(int[] array) {
        heapLength = array.length;
        buildMaxHeap(array);
        int end = array.length - 1;
        while (heapLength != 0 && end >= 0) {
            swap(array, 0, end);
            interchanges++;
            end--;
            heapLength--;
            maxHeapify(0, array);
        }
    }

    private void buildMaxHeap(int[] array) {
        for (int i = array.length / 2; i >= 0; i--) {
            maxHeapify(i, array);
        }
    }

    private void maxHeapify(int i, int[] array) {
        int l = (2 * i) + 1;
        int r = (2 * i) + 2;
        int largest = i;
        if (r < heapLength && array[r] > array[i]) largest = r;
        if (l < heapLength && array[l] > array[largest]) largest = l;
        comparisons += 2;
        if (largest != i) {
            swap(array, i, largest);
            interchanges++;
            maxHeapify(largest, array);
        }
    }
}
