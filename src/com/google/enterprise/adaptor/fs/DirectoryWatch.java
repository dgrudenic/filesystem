package com.google.enterprise.adaptor.fs;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DirectoryWatch implements Runnable {

  private static final Logger log = Logger.getLogger(DirectoryWatch.class.getName());

  public static Map<String, EgnayteAclAttributes> egnyteAcl = new HashMap<>();
  private static final String [] FILE_HEADER_MAPPING = {"path", "assigned_groups", "assigned_users"};

  private final Path dir;
  private final WatchService watcher;
  private final WatchKey key;

  public DirectoryWatch(Path dir) throws IOException {
    this.dir = dir;
    this.watcher = FileSystems.getDefault().newWatchService();
    this.key = dir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
  }

  private static void readCsvFile(String fileName) {
    FileReader fileReader = null;

    CSVParser csvFileParser = null;

    //Create the CSVFormat object with the header mapping
    CSVFormat csvFileFormat = CSVFormat.TDF.withHeader(FILE_HEADER_MAPPING);

    try {
      //initialize FileReader object
      fileReader = new FileReader(fileName);

      //initialize CSVParser object
      csvFileParser = new CSVParser(fileReader, csvFileFormat);

      //Get a list of CSV file records
      List csvRecords = csvFileParser.getRecords();

      //Read the CSV file records starting from the second record to skip the header
      for (int i = 1; i < csvRecords.size(); i++) {
        CSVRecord record = (CSVRecord) csvRecords.get(i);
        String path = record.get("path");
        log.log(Level.FINE, "CSV path: {0}", path);
        String assignedGroups = record.get("assigned_groups");
        String assignedUsers = record.get("assigned_users");

        if(assignedGroups.equals("")) {
          assignedGroups = null;
        }

        if(assignedUsers.equals("")) {
          assignedUsers = null;
        }

        egnyteAcl.put(path, new EgnayteAclAttributes(path, assignedGroups, assignedUsers));
      }


    }
    catch (Exception e) {
      log.log(Level.SEVERE, "Error in CsvFileReader {0}", e);
    } finally {
      try {
        fileReader.close();
        csvFileParser.close();
      } catch (IOException e) {
        log.log(Level.SEVERE, "Error while closing fileReader/csvFileParser  {0}", e);
      }
    }

  }

  @SuppressWarnings("unchecked")
  static <T> WatchEvent<T> cast(WatchEvent<?> event) {
    return (WatchEvent<T>) event;
  }

  @Override
  public void run() {
    try {
      for (;;) {
        // wait for key to be signalled
        WatchKey key = watcher.take();

        if (this.key != key) {
          log.log(Level.SEVERE, "WatchKey not recognized!");
          continue;
        }

        for (WatchEvent<?> event : key.pollEvents()) {
          WatchEvent<Path> ev = cast(event);
          log.log(Level.FINE, "WatchEvent kind: {0}", ev.kind());
          log.log(Level.FINE, "WatchEvent context: {0}", dir.resolve(ev.context()));
          readCsvFile(dir.resolve(ev.context()).toString());

        }

        // reset key
        if (!key.reset()) {
          break;
        }
      }
    } catch (InterruptedException x) {
      return;
    }

  }
}
