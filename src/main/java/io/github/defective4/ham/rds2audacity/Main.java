package io.github.defective4.ham.rds2audacity;

import com.google.gson.Gson;

import java.io.*;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

import static java.lang.System.err;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            err.println("Usage: " + Main.class.getProtectionDomain()
                                              .getCodeSource()
                                              .getLocation()
                                              .getFile() + " <input> <output> [keywords...]");
            return;
        }

        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        File rdsFile = args[0].equals("-") ? null : new File(args[0]);
        File targetFile = args[1].equals("-") ? null : new File(args[1]);

        if (rdsFile != null && !rdsFile.isFile()) {
            err.println("File " + args[1] + " does not exist");
            return;
        }

        String[] keywords = Arrays.copyOfRange(args, 2, args.length);

        Gson gson = new Gson();
        List<RDSEntry> entries = new ArrayList<>();
        List<String> ps = new ArrayList<>();
        long absoluteStart = Long.MAX_VALUE;
        long absoluteEnd = Long.MIN_VALUE;
        long transmitterStart = Long.MAX_VALUE;
        long transmitterEnd = Long.MIN_VALUE;
        err.println("Parsing input file...");
        try (BufferedReader reader = rdsFile == null ? new BufferedReader(new InputStreamReader(System.in)) : Files.newBufferedReader(
                rdsFile.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
                RDSEntry entry = gson.fromJson(line, RDSEntry.class);
                if (entry != null) {
                    if (entry.ps != null && !ps.contains(entry.ps)) ps.add(entry.ps);
                    if (entry.clock_time != null) {
                        try {
                            long ttime = format.parse(entry.clock_time).getTime();
                            transmitterStart = Math.min(transmitterStart, ttime);
                            transmitterEnd = Math.max(transmitterEnd, ttime);
                        } catch (Exception ignored) {
                        }
                    }
                    absoluteStart = Math.min(absoluteStart, entry.rx_time);
                    absoluteEnd = Math.max(absoluteEnd, entry.rx_time);
                    if (entry.radiotext != null) {
                        boolean isSong = true;
                        for (String keyword : keywords)
                            if (!entry.radiotext.contains(keyword)) {
                                isSong = false;
                                break;
                            }
                        if (isSong) {
                            entries.add(entry);
                        }
                    }
                }
            }
        }

        Map<String, TimeRange> songTimes = new LinkedHashMap<>();

        err.println("Filtering RDS data...");
        for (RDSEntry entry : entries) {
            String song = entry.radiotext;
            for (String keyword : keywords)
                song = song.replace(keyword, "");
            long time = entry.rx_time - absoluteStart;
            if (!songTimes.containsKey(song)) songTimes.put(song, new TimeRange());
            TimeRange range = songTimes.get(song);
            range.setEnd(Math.max(range.getEnd(), time));
            range.setStart(Math.min(range.getStart(), time));
        }

        err.println("Correcting song timestamps");
        for (Map.Entry<String, TimeRange> song : new ArrayList<>(songTimes.entrySet())) {
            List<TimeRange> conflicts = new ArrayList<>();
            TimeRange range = song.getValue();
            for (Map.Entry<String, TimeRange> subsong : songTimes.entrySet()) {
                if (subsong.getKey().equals(song.getKey())) continue;
                TimeRange subrange = subsong.getValue();
                if (subrange.getStart() >= range.getStart() && subrange.getEnd() <= range.getEnd()) {
                    conflicts.add(subrange);
                }
            }

            if (!conflicts.isEmpty()) {
                conflicts.sort((time1, time2) -> (int) (time1.getStart() - time2.getStart()));
                long oldEnd = range.getEnd();
                range.setEnd(Math.max(range.getStart(), conflicts.get(0).getStart() - 1));
                songTimes.put(song.getKey() + " (2)",
                              new TimeRange(Math.min(conflicts.get(conflicts.size() - 1).getEnd() + 1, oldEnd),
                                            oldEnd));
            }
            break;
        }

        err.println("Writing labels...");
        try (PrintWriter pw = targetFile == null ? new PrintWriter(System.out) : new PrintWriter(Files.newOutputStream(
                targetFile.toPath()))) {
            for (Map.Entry<String, TimeRange> entry : songTimes.entrySet()) {
                TimeRange range = entry.getValue();
                pw.println(String.format("%s\t%s\t%s", range.getStart(), range.getEnd(), entry.getKey()));
            }
        }

        err.println("==== RDS REPORT ====");
        err.println("Station identifiers:");
        ps.forEach(line -> err.println("- " + line));
        err.println();
        err.println("RDS recording time: " + getDuration(absoluteStart * 1000, absoluteEnd * 1000));
        err.println("Transmitter recording time: " + (transmitterStart == Long.MAX_VALUE || transmitterEnd == Long.MIN_VALUE ? "No data received" : getDuration(
                transmitterStart,
                transmitterEnd)));
        err.println("Songs recorded: " + songTimes.size());
        long totalDur = 0;
        for (TimeRange range : songTimes.values())
            totalDur += range.getDuration();
        err.println("Est. total music duration: " + Duration.ofSeconds(totalDur).toString().substring(2));
        double prc = (double) totalDur / (double) (absoluteEnd - absoluteStart);
        prc *= 10000;
        prc = (int) prc;
        prc /= 100;
        err.println("Est. music to ads percentage: " + prc + "%");
        err.println("==== END OF RDS REPORT ====");
    }


    private static String getDuration(long start, long end) {
        return Duration.ofMillis(end - start).toString().substring(2);
    }
}
