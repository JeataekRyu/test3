import java.util.Timer;
import java.util.TimerTask;

public class HelloWorld {
	public static void main(String[] args) {
		Timer t = new Timer("TimerThread");
		t.schedule(new TimerTask() {
			public void run() {
				System.out.println("This is later");
				System.exit(0);
			}
		}, 1 * 1000);
		System.out.println("Exiting main()");
	}

}
