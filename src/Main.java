import java.awt.EventQueue;


public class Main {


	public static void main(String[] args) throws InterruptedException{
		EventQueue.invokeLater(new Runnable(){

			@Override
			public void run() {
				new SynmvFrame();
			}
			
		});
		
	}

}
