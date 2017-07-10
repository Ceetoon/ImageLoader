package libcore.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import libcore.io.DiskLruCache.Editor;
import libcore.io.DiskLruCache.Snapshot;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.util.LruCache;
import android.widget.ImageView;
/**
 * Created by ceetoon on 2016/5/6.
 */
public class ImageLoader {
	private LruCache<String, Bitmap> mLruCache;
	private ExecutorService THREAD_POOL;
	private static final long MAX_MEMORY_CACHESIZE = 1024 * 1024 * 50;
	private static final int BUFFER_SIZE = 1024 * 1;
	private DiskLruCache mDiskLruCache;
	private static final int LOAD_IMAGE = 0;
	private static final int GET_TASK = 0;
	private LinkedList<MyTask>mTaskQueue=new LinkedList<MyTask>();
	private Handler mLooperHandler;
	private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case LOAD_IMAGE:
				ImageResult result = (ImageResult) msg.obj;
				ImageView iv = result.iv;
				if (result.url.equals(iv.getTag())) {
					iv.setImageBitmap(result.bitmap);
				}
				break;

			default:
				break;
			}

		};

	};
	@SuppressLint("NewApi")
	public ImageLoader(Context context) {
		mLruCache = new LruCache<String, Bitmap>((int) (Runtime.getRuntime()
				.maxMemory() / 1024 / 8)) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getRowBytes() * value.getHeight()/1024;
			}
		};
		THREAD_POOL = Executors.newFixedThreadPool(10);
		File cacheDir = getCacheDir(context, "bitmap");
		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
		if (getUseableSpace(cacheDir) > MAX_MEMORY_CACHESIZE) {
			try {
				mDiskLruCache = DiskLruCache.open(cacheDir, 1, 1,
						MAX_MEMORY_CACHESIZE);
				mIsDiskLruCacheCreated = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		startLooperThread();

	}

	private void startLooperThread() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				mLooperHandler = new Handler(){
					public void handleMessage(android.os.Message msg) {
						MyTask task=getTask();
						if(task!=null){
							THREAD_POOL.submit(task);
						}
					}
				};
				Looper.loop();
			}
		}).start();
	}
	private MyTask getTask() {
		return mTaskQueue.removeLast();
	};
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private long getUseableSpace(File cacheDir) {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD) {
			return cacheDir.getUsableSpace();
		}
		final StatFs statFs = new StatFs(cacheDir.getPath());
		return (long) statFs.getBlockSize() * (long) statFs.getBlockCount();
	}

	private File getCacheDir(Context context, String cacheFileName) {
		String cacheDir = "";
		boolean extraAvaiable = Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);
		if (extraAvaiable) {
			cacheDir = context.getExternalCacheDir().getPath();
		} else {
			cacheDir = context.getCacheDir().getPath();
		}
		return new File(cacheDir + File.separator + cacheFileName);
	}

	public void display(ImageView iv, String url) {
		display(iv, url, 0, 0);
	}

	public void display(ImageView iv, String url, int reqWidth, int reqHeight) {
		
		Bitmap bitmap = loadBimapFromMemCache(url);
		if (bitmap != null) {
			iv.setImageBitmap(bitmap);
			return;
		}
		iv.setTag(url);
		MyTask task=new MyTask(iv, url, reqWidth, reqHeight);
		mTaskQueue.add(task);
		mLooperHandler.obtainMessage(GET_TASK).sendToTarget();
	}

	class MyTask implements Runnable {

		private ImageView iv;
		private String url;
		private int reqWidth;
		private int reqHeight;

		public MyTask(ImageView iv, String url, int reqWidth, int reqHeight) {
			this.iv = iv;
			this.url = url;
			this.reqWidth = reqWidth;
			this.reqHeight = reqHeight;
		}

		@Override
		public void run() {
			Bitmap bitmap = loadImage(iv, url, reqWidth, reqHeight);
			ImageResult result = new ImageResult(url, iv, bitmap);
			mMainHandler.obtainMessage(LOAD_IMAGE, result).sendToTarget();
		}
	}

	static class ImageResult {
		public ImageResult(String url, ImageView iv, Bitmap bitmap) {
			this.url = url;
			this.iv = iv;
			this.bitmap = bitmap;
		}

		String url;
		ImageView iv;
		Bitmap bitmap;
	}

	private Bitmap loadBimapFromMemCache(String url) {
		String key = getMd5FileName(url);
		return mLruCache.get(key);
	}

	public Bitmap loadImage(ImageView iv, String url, int reqWidth,
			int reqHeight) {
		Bitmap bitmap = loadBimapFromMemCache(url);
		if (bitmap != null) {
			return bitmap;
		}
		bitmap = loadBitmapFromDiskLruCache(url, reqWidth, reqHeight);
		if (bitmap != null) {
			return bitmap;
		}
		bitmap = loadBitmapFromHttp(url, reqWidth, reqHeight);

		if (bitmap == null && !mIsDiskLruCacheCreated) {
			bitmap = loadBitmapFromUrl(url);
		}
		return bitmap;
	}

	private Bitmap loadBitmapFromUrl(String url) {

		HttpURLConnection conn = null;
		BufferedInputStream is = null;
		Bitmap bitmap = null;
		try {
			URL u = new URL(url);
			conn = (HttpURLConnection) u.openConnection();
			if (conn.getResponseCode() == 200) {
				is = new BufferedInputStream(conn.getInputStream(), BUFFER_SIZE);
				bitmap = BitmapFactory.decodeStream(is);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
			MyUtils.close(is);
		}

		return bitmap;

	}

	private Bitmap loadBitmapFromHttp(String url, int reqWidth, int reqHeight) {
		if (mDiskLruCache == null)
			return null;
		String key = getMd5FileName(url);
		try {
			Editor edit = mDiskLruCache.edit(key);
			if (edit != null) {
				BufferedOutputStream os = new BufferedOutputStream(
						edit.newOutputStream(0), BUFFER_SIZE);
				if (downloadStreamFromUrl(url, os)) {
					edit.commit();
				} else {
					edit.abort();
				}
				mDiskLruCache.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return loadBitmapFromDiskLruCache(url, reqWidth, reqHeight);
	}

	private Bitmap loadBitmapFromDiskLruCache(String url, int reqWidth,
			int reqHeight) {
		if (mDiskLruCache == null) {
			return null;
		}
		Bitmap bitmap = null;
		try {
			String key = getMd5FileName(url);
			Snapshot snapshot = mDiskLruCache.get(key);
			if (snapshot != null) {
				FileInputStream fis = (FileInputStream) snapshot
						.getInputStream(0);
				bitmap = mImageResizer.compressSampleBitmapFromFileDesc(
						fis.getFD(), reqWidth, reqHeight);
				if (bitmap != null) {
					addBitmapMemCache(key, bitmap);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bitmap;
	}

	private void addBitmapMemCache(String key, Bitmap bitmap) {
		if (mLruCache.get(key) == null) {
			mLruCache.put(key, bitmap);
		}
	}

	private ImageResizer mImageResizer = new ImageResizer();
	private boolean mIsDiskLruCacheCreated;


	private boolean downloadStreamFromUrl(String url, BufferedOutputStream os) {
		HttpURLConnection conn = null;
		BufferedInputStream is = null;
		boolean result = false;
		try {
			URL u = new URL(url);
			conn = (HttpURLConnection)u.openConnection();
			if (conn.getResponseCode() == 200) {
				is = new BufferedInputStream(conn.getInputStream(), BUFFER_SIZE);
				int len = -1;
				while ((len = is.read()) != -1) {
					os.write(len);
				}
				result = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
			MyUtils.close(is);
			MyUtils.close(os);
		}

		return result;
	}


	private String getMd5FileName(String url) {
		String cacheKey;
		try {
			final MessageDigest mDigest = MessageDigest.getInstance("MD5");
			mDigest.update(url.getBytes());
			cacheKey = toHexString(mDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			cacheKey = String.valueOf(url.hashCode());
		}
		return cacheKey;
	}

	private String toHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		final int length = bytes.length;
		for (int i = 0; i < length; i++) {
			String hexString = Integer.toHexString(0xFF & bytes[i]);
			if (hexString.length() == 1) {
				sb.append("0");
			}
			sb.append(hexString);
		}
		return sb.toString();

	}
}
