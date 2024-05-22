package io.github.defective4.ham.rds2audacity;

import com.google.gson.Gson;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            System.err.println("Usage: " + Main.class.getProtectionDomain()
                                                     .getCodeSource()
                                                     .getLocation()
                                                     .getFile() + " <input> <output> [keywords...]");
            return;
        }

        File rdsFile = args[0].equals("-") ? null : new File(args[0]);
        File targetFile = args[1].equals("-") ? null : new File(args[1]);

        if (rdsFile != null && !rdsFile.isFile()) {
            System.err.println("File " + args[1] + " does not exist");
            return;
        }

        String[] keywords = Arrays.copyOfRange(args, 2, args.length);

        Gson gson = new Gson();
        List<RDSEntry> entries = new ArrayList<>();
        long absoluteStart = Long.MAX_VALUE;
        System.err.println("Parsing input file...");
        try (BufferedReader reader = rdsFile == null ? new BufferedReader(new InputStreamReader(System.in)) : Files.newBufferedReader(
                rdsFile.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
                RDSEntry entry = gson.fromJson(line, RDSEntry.class);
                if (entry != null) {
                    absoluteStart = Math.min(absoluteStart, entry.rx_time);
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

        System.err.println("Filtering RDS data...");
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

        System.err.println("Writing labels...");
        try (PrintWriter pw = targetFile == null ? new PrintWriter(System.out) : new PrintWriter(Files.newOutputStream(
                targetFile.toPath()))) {
            for (Map.Entry<String, TimeRange> entry : songTimes.entrySet()) {
                TimeRange range = entry.getValue();
                pw.println(String.format("%s\t%s\t%s", range.getStart(), range.getEnd(), entry.getKey()));
            }
        }
    }


}
