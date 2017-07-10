package libcore.io;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
/**
 * Created by ceetoon on 2016/5/6.
 */
public class SquareImageView extends ImageView {

	public SquareImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, widthMeasureSpec);
	}

}
