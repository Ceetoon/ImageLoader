package libcore.io;

import java.io.FileDescriptor;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
/**
 * Created by ceetoon on 2016/5/6.
 */
public class ImageResizer {
	public Bitmap compressSampleBitmapFromResource(Resources re,int resId,int reqWidth,int reqHeight){
		Options options=new Options();
		options.inJustDecodeBounds=true;
		BitmapFactory.decodeResource(re,resId, options);
		options.inSampleSize=cacluSampleSize(options,reqWidth,reqHeight);
		options.inJustDecodeBounds=false;
		return BitmapFactory.decodeResource(re,resId,options);
	}
	public Bitmap compressSampleBitmapFromFileDesc(FileDescriptor fd,int reqWidth,int reqHeight){
		Options options=new Options();
		options.inJustDecodeBounds=true;
		BitmapFactory.decodeFileDescriptor(fd, null,options);
		options.inSampleSize=cacluSampleSize(options,reqWidth,reqHeight);
		options.inJustDecodeBounds=false;
		return BitmapFactory.decodeFileDescriptor(fd, null,options);
	}

	private int cacluSampleSize(Options options, int reqWidth, int reqHeight) {
		if(reqWidth==0 || reqHeight==0){
			return 1;
		}
		final int width=options.outWidth;
		final int height=options.outHeight;
		int sampleSize=1;
		if(width>reqWidth || height>reqHeight){
			while(width/sampleSize>reqWidth && height/sampleSize>reqHeight){
				sampleSize*=2;
			}
		}
		return sampleSize;
	}
	
	
}
