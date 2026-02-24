public class HeapSort extends Sort{

    int heaplenght;

    @Override
    public void sort(int[] array) {
        heaplenght = array.length;
        BuildMaxHeap(array);
        int end = array.length-1;
        while (heaplenght != 0 && end >= 0){
            swap(array, 0, end);
            interchanges++;
            end--;
            heaplenght--;
            MaxHeapify(0, array);
        }
    }

    private void BuildMaxHeap(int[] array){
        for (int i = array.length/2; i >= 0; i--) {
            MaxHeapify(i, array);
        }
    }

    private void MaxHeapify(int i, int[] array){
        int l = left(i);
        int r = right(i);
        int largest = i;
        if(r < heaplenght && array[r] > array[i]){
            largest = r;
        }
        if(l < heaplenght && array[l] > array[largest]){largest = l;}
        comparisons+=2;
        if(largest != i){
            swap(array, i, largest);
            interchanges++;
            MaxHeapify(largest, array);
        }
    }

    private int left(int i){return (2*i)+1;}

    private int right(int i){return (2*i)+2;}
}
