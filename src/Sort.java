public abstract class Sort {
    int[] array;
    public long interchanges;
    public long comparisons;

    public abstract void sort(int[] array);

    public void reset() {
        interchanges = 0;
        comparisons = 0;
    }

    public void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    public void printarray(int[] array) {
        for (int i = 0; i < array.length; i++) {
            System.out.print(array[i]);
            if (i + 1 != array.length) {
                System.out.print(", ");
            }
        }
    }
}
