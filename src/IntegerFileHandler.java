import java.io.*;
import java.util.*;

    public static void generateIntegerFile(String filename, int count) throws IOException {
        Random random = new Random();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int i = 0; i < count; i++) {
                int number = random.nextInt(1001);
                writer.write(String.valueOf(number));
                if (i < count - 1) {
                    writer.write(", ");
                }
            }
            writer.newLine();
        }
    }

    public static void main(String[] args) {
        InputReader reader = new InputReader();
        if (args.length == 0) {
            // Demo: generate a sample file and read it
            String sampleFile = "sample_ints.txt";
            try {
                System.out.println("No input files given. Generating a sample file: " + sampleFile);
                generateIntegerFile(sampleFile, 20);
                System.out.println("Generated " + sampleFile + "\n");
                args = new String[]{sampleFile};
            } catch (IOException e) {
                System.err.println("Failed to generate sample file: " + e.getMessage());
                return;
            }
        }

        // Process each file provided as argument
        for (String filename : args) {
            try {
                List<Integer> numbers = reader.readIntegerFile(filename);
                System.out.println("File: " + filename);
                System.out.println("Integers (" + numbers.size() + " total): " + numbers);
            } catch (FileNotFoundException e) {
                System.err.println("File not found: " + filename);
            } catch (IOException e) {
                System.err.println("Error reading file " + filename + ": " + e.getMessage());
            } catch (NumberFormatException e) {
                System.err.println("Invalid number format in file " + filename + ": " + e.getMessage());
            }
            System.out.println();
        }
    }
