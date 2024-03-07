import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.Map;


public class PlotThroughput {
  static String plotPath = "ThroughputPlot.csv";
  static String outputPath = "ProcessedThroughputPlot.csv";
  


  public static void main(String[] args) throws IOException {
//    plotThroughput();

    // deal with csv
    dealWithData();
  }


  private static void dealWithData() {
    Map<Long, String> newEntries = new LinkedHashMap<>(); // Preserve insertion order
    long startTime = 0;
    long prev = 0;

    try (BufferedReader reader = new BufferedReader(new FileReader(plotPath))) {
      String line;
      reader.readLine(); // Skip header

      int i = 1;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.trim().split("\\s+"); // Assumes whitespace separation
        if (parts.length >= 2) {
          long currentTime = Long.parseLong(parts[0]);
          if (i == 1) {
            startTime = currentTime;
            prev = currentTime;
            String newLine = String.format("%-75f %-25f%n", 0.0, 0.0 );
            newEntries.put(currentTime, newLine);
          }
          if (i % 100 == 0 && i != 1) {
            double wallTime = ((currentTime - prev) / 1000.0);
            String newLine = String.format("%-75f %-25f%n", (float) (currentTime - startTime), 100 / wallTime);
            newEntries.put(currentTime, newLine); // This will overwrite duplicates, keeping the last
            prev = currentTime;
          }

        }
        i++;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Write processed data back to a file
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
      writer.write(String.format("%-75s %-25s%n", "Current Time", "Throughput"));
      for (String entry : newEntries.values()) {
        writer.write(entry + "\n");
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  private static void plotThroughput() throws IOException{
    List<PostMetric> flattenedMetrics = SkiersClient.metrics.stream()
        .flatMap(List::stream)
        .collect(Collectors.toList());

    List<PostMetric> sortedMetrics = sortByEndTime(flattenedMetrics);


    try (BufferedWriter writer = new BufferedWriter(new FileWriter(plotPath))) {
      writer.append(String.format("%-75s %-25s%n", "Current Time", "Throughput"));

      sortedMetrics.stream()
          .map(metric -> {
            long curTime = metric.getEnd();
            double throughput = (curTime - SkiersClient.startTime == 0) ? 0
                : (sortedMetrics.indexOf(metric) + 1) / ((curTime - SkiersClient.startTime) / 1000.0);
            return String.format("%-75s %-25f%n", curTime, throughput);
          })
          .forEach(line -> {
            try {
              writer.write(line);
            } catch (IOException e) {
              e.printStackTrace();
            }
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }
    private static List<PostMetric> sortByEndTime(List<PostMetric> metrics) {
      List<PostMetric> sortedMetrics = new ArrayList<>(metrics);
      Comparator<PostMetric> latencyComparator = new Comparator<PostMetric>() {
        @Override
        public int compare(PostMetric metric1, PostMetric metric2) {
          // Compare based on endtime
          return Double.compare(metric1.getEnd(), metric2.getEnd());
        }
      };
      Collections.sort(sortedMetrics, latencyComparator);
      return  sortedMetrics;
    }


}
