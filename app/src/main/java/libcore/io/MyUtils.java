package libcore.io;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
/**
 * Created by ceetoon on 2016/5/6.
 */
public class MyUtils {

	public static void close(InputStream is){
		if(is!=null){
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				is=null;
			}
		}
	}

	public static void close(BufferedOutputStream os) {
		if(os!=null){
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				os=null;
			}
		}
		
	}
}
