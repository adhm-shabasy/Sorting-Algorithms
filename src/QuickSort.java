import java.util.Random;

public class QuickSort extends Sort {
    Random random = new Random();

    @Override
    public void sort(int[] array) {
        partition(array, 0, array.length - 1);
    }

    private void partition(int[] array, int l, int h) {
        if (l >= h) return;
        int pivotIndex = random.nextInt(l, h + 1);
        int p = array[pivotIndex];
        swap(array, pivotIndex, l);
        int i = l, j = l + 1;
        while (j <= h) {
            comparisons++;
            if (array[j] >= p) { j++; }
            else { i++; swap(array, i, j); j++; interchanges++; }
        }
        if (i != l) { swap(array, i, l); interchanges++; }
        partition(array, l, i - 1);
        partition(array, i + 1, h);
    }
}
