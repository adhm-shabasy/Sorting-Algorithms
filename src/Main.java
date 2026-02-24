void main() throws IOException {

    Random random = new Random();
    Generator generator = new Generator();
    InputReader reader = new InputReader();

    String sampleFile = "sample_ints.txt";
    int size = random.nextInt(10, 101);
    int op = 0;//random.nextInt(0,3);
    int bound = 1000;

    //int[] array = generator.generate(size, op, bound);
    List<Integer> list = reader.readIntegerFile(sampleFile);
    int[] array = list.stream().mapToInt(Integer::intValue).toArray();

    System.out.println(size);
    for (int i = 0; i < array.length; i++) {
        System.out.print(array[i]);
        if (i + 1 != array.length) {
            System.out.print(", ");
        }
    }
    System.out.println();
    System.out.println();

    //SelectionSort sort = new SelectionSort();
    //InsertionSort sort = new InsertionSort();
    //BubbleSort sort = new BubbleSort();
    //MergeSort sort = new MergeSort();
    //QuickSort sort = new QuickSort();
    HeapSort sort = new HeapSort();

    long start = System.nanoTime();
    sort.sort(array);
    long end = System.nanoTime();
    double time = ((end - start)/1000000.0);
    sort.printarray(array);
    System.out.println();
    System.out.println(time);
    System.out.println(sort.comparisons);
    System.out.println(sort.interchanges);
}

