package getstarted;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.api.Scanner;
import com.scalar.db.config.DatabaseConfig;
import com.scalar.db.io.BigIntValue;
import com.scalar.db.io.IntValue;
import com.scalar.db.io.Key;
import com.scalar.db.io.TextValue;
import com.scalar.db.service.StorageModule;
import com.scalar.db.service.StorageService;
import com.scalar.db.service.TransactionModule;
import com.scalar.db.service.TransactionService;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Iterator;
import java.util.Optional;

public class Worklog {
  private static final String SCALARDB_PROPERTIES =
      System.getProperty("user.dir") + File.separator + "scalardb.properties";
  private final TransactionService transaction;
  private final StorageService storage;

  public Worklog() throws IOException {
    DatabaseConfig dbConfig = new DatabaseConfig(new FileInputStream(SCALARDB_PROPERTIES));
    Injector injector = Guice.createInjector(new TransactionModule(dbConfig));
    transaction = injector.getInstance(TransactionService.class);

    injector = Guice.createInjector(new StorageModule(dbConfig));
    storage = injector.getInstance(StorageService.class);
  }

  public void log(String log) throws Exception {
    DistributedTransaction tx = transaction.start();

    Key pKey =
        new Key(new TextValue("date", DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now())));
    Key cKey = new Key(new BigIntValue("timestamp", System.currentTimeMillis() / 1000));

    Put put1 = new Put(pKey, cKey).withValue(new TextValue("log", log)).forNamespace("plenty")
        .forTable("worklog");

    int count = 0;
    Get get = new Get(pKey).forNamespace("plenty").forTable("workcount");
    Optional<Result> result = tx.get(get);

    if (result.isPresent()) {
      count = ((IntValue) result.get().getValue("count").get()).get();
    }
    count += 1;
    Put put2 = new Put(pKey).withValue(new IntValue("count", count)).forNamespace("plenty")
        .forTable("workcount");

    tx.put(put1);
    tx.put(put2);

    tx.commit();
  }

  public void today() throws Exception {
    Key pKey =
        new Key(new TextValue("date", DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now())));
    Scan scan = new Scan(pKey).forNamespace("plenty").forTable("worklog");
    Scanner scanner = storage.scan(scan);

    scanner.forEach(result -> {
      BigIntValue timestampValue = (BigIntValue) result.getValue("timestamp").get();
      TextValue logValue = (TextValue) result.getValue("log").get();
      Date d = new Date(timestampValue.get() * 1000);
      System.out.println(d + ": " + logValue.getString().get());
    });

    int count = 0;
    Get get = new Get(pKey).forNamespace("plenty").forTable("workcount");
    Optional<Result> result = storage.get(get);

    if (result.isPresent()) {
      count = ((IntValue) result.get().getValue("count").get()).get();
    }
    System.out.println("TOTAL WORK: " + count);
  }

  public void close() {
    storage.close();
    transaction.close();
  }
}
