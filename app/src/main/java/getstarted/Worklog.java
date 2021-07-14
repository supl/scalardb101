package getstarted;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.api.Scan;
import com.scalar.db.config.DatabaseConfig;
import com.scalar.db.exception.transaction.CommitException;
import com.scalar.db.exception.transaction.CrudException;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.exception.transaction.UnknownTransactionStatusException;
import com.scalar.db.io.BigIntValue;
import com.scalar.db.io.IntValue;
import com.scalar.db.io.Key;
import com.scalar.db.io.TextValue;
import com.scalar.db.service.StorageModule;
import com.scalar.db.service.TransactionModule;
import com.scalar.db.service.TransactionService;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

public class Worklog {
  private static final String SCALARDB_PROPERTIES =
      System.getProperty("user.dir") + File.separator + "scalardb.properties";
  private final TransactionService transaction;

  public Worklog() throws IOException {
    DatabaseConfig dbConfig = new DatabaseConfig(new FileInputStream(SCALARDB_PROPERTIES));
    Injector injector = Guice.createInjector(new TransactionModule(dbConfig));
    transaction = injector.getInstance(TransactionService.class);

    injector = Guice.createInjector(new StorageModule(dbConfig));
  }

  public void log(String log) throws Exception {
    for (int retry = 3; retry > 0; retry--) {
      try {
        DistributedTransaction tx = transaction.start();

        Key pKey = new Key(
            new TextValue("date", DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now())));
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

        try {
          tx.put(put1);
          tx.put(put2);
          tx.commit();
        } catch (CrudException | CommitException | UnknownTransactionStatusException e) {
          tx.abort();
          continue;
        }
      } catch (TransactionException e) {
        continue;
      }

      break;
    }
  }

  public void today() throws Exception {
    Key pKey =
        new Key(new TextValue("date", DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now())));
    Scan scan = new Scan(pKey).forNamespace("plenty").forTable("worklog");
    Get get = new Get(pKey).forNamespace("plenty").forTable("workcount");

    for (int retry = 3; retry > 0; retry--) {
      try {
        DistributedTransaction tx = transaction.start();

        try {
          tx.scan(scan).forEach(result -> {
            BigIntValue timestampValue = (BigIntValue) result.getValue("timestamp").get();
            TextValue logValue = (TextValue) result.getValue("log").get();
            Date d = new Date(timestampValue.get() * 1000);
            System.out.println(d + ": " + logValue.getString().get());
          });

          int count = 0;
          Optional<Result> result = tx.get(get);
          if (result.isPresent()) {
            count = ((IntValue) result.get().getValue("count").get()).get();
          }
          System.out.println("TOTAL WORK: " + count);

          tx.commit();
        } catch (CrudException | CommitException | UnknownTransactionStatusException e) {
          tx.abort();
          continue;
        }
      } catch (TransactionException e) {
        continue;
      }


      break;
    }
  }

  public void close() {
    transaction.close();
  }
}
