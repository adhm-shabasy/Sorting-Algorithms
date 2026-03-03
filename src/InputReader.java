import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InputReader {
    public List<Integer> readIntegerFile(String filename) throws IOException {
        List<Integer> numbers = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                // Strip bracket/brace/paren characters so [1,2,3] and (1 2 3) also work
                line = line.replaceAll("[\\[\\](){}]", "");
                // Split on commas and/or whitespace so "1,2,3" and "1 2 3" both work
                String[] parts = line.split("[,\\s]+");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        try {
                            numbers.add(Integer.parseInt(trimmed));
                        } catch (NumberFormatException ignored) {
                            // skip non-numeric tokens silently
                        }
                    }
                }
            }
        }
        return numbers;
    }
}