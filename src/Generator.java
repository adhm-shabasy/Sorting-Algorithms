import java.util.Random;

public class Generator {
    Random random = new Random();

    public int[] generate(int size, int op, int bound) {
        int[] array;
        switch (op) {
            case 0 -> array = random.ints(size, 0, bound + 1).toArray();
            case 1 -> {
                array = new int[size];
                for (int i = 0; i < size; i++) array[i] = i;
            }
            case 2 -> {
                array = new int[size];
                for (int i = 0; i < size; i++) array[i] = size - i;
            }
            default -> throw new IllegalStateException("Unexpected value: " + op);
        }
        return array;
    }
}
