package getstarted;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.api.Scanner;
import com.scalar.db.config.DatabaseConfig;
import com.scalar.db.io.BigIntValue;
import com.scalar.db.io.Key;
import com.scalar.db.io.TextValue;
import com.scalar.db.service.StorageModule;
import com.scalar.db.service.StorageService;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Iterator;

public class Worklog {
  private static final String SCALARDB_PROPERTIES =
      System.getProperty("user.dir") + File.separator + "scalardb.properties";
  private static final String NAMESPACE = "plenty";
  private static final String TABLENAME = "worklog";
  private final StorageService storage;

  public Worklog() throws IOException {
    DatabaseConfig dbConfig = new DatabaseConfig(new FileInputStream(SCALARDB_PROPERTIES));
    Injector injector = Guice.createInjector(new StorageModule(dbConfig));

    storage = injector.getInstance(StorageService.class);
    storage.with(NAMESPACE, TABLENAME);
  }

  public void log(String log) throws Exception {
    Key pKey =
        new Key(new TextValue("date", DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now())));
    Key cKey = new Key(new BigIntValue("timestamp", System.currentTimeMillis() / 1000));
    Put put = new Put(pKey, cKey).withValue(new TextValue("log", log));

    storage.put(put);
  }

  public void today() throws Exception {
    Key pKey =
        new Key(new TextValue("date", DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now())));
    Scan scan = new Scan(pKey);
    Scanner scanner = storage.scan(scan);
    Iterator<Result> iterator = scanner.iterator();

    while (iterator.hasNext()) {
      Result result = iterator.next();
      BigIntValue timestampValue = (BigIntValue) result.getValue("timestamp").get();
      TextValue logValue = (TextValue) result.getValue("log").get();
      Date d = new Date(timestampValue.get() * 1000);
      System.out.println(d.toString() + ": " + logValue.getString().get());
    }
  }

  public void close() {
    storage.close();
  }
}
