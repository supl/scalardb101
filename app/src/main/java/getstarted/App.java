package getstarted;

public class App {
  public static void main(String[] args) throws Exception {
    Worklog worklog = new Worklog();

    if (args.length > 0) {
      String log = args[0];
      worklog.log(log);
    } else {
      worklog.today();
    }

    worklog.close();
  }
}
