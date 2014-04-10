import java.awt.EventQueue;
import java.io.FileNotFoundException;


public class Main {


	public static void main(String[] args) throws InterruptedException{
		EventQueue.invokeLater(new Runnable(){

			@Override
			public void run() {
				try {
					new SynmvFrame();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		});
		
	}

}
