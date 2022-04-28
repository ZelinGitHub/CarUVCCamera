package com.wt.carcamera.ui.activity;




import static com.wt.carcamera.util.AppUtils.hideStatusBar;
import static com.wt.carcamera.util.Constant.ARG_IMG_PATH;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.wt.carcamera.R;
import com.wt.carcamera.util.OnNoDoubleClickListener;

public class ImgDetailActivity extends AppCompatActivity {

    ImageView iv_back;
    ImageView iv_img;
    String mPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img_detail);
        mPath = getIntent().getStringExtra(ARG_IMG_PATH);
        iv_back = findViewById(R.id.iv_back);
        iv_img = findViewById(R.id.iv_img);


        iv_back.setOnClickListener(new OnNoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                finish();
            }
        });
//        //设置返回按钮的颜色
//        VectorDrawable drawable2 = (VectorDrawable) getDrawable(R.drawable.ic_arrow_ios_left);
//        drawable2.mutate();
//        drawable2.setTint(getResources().getColor(R.color.wt_system_white_700_color));
//        drawable2.setBounds(0, 0, drawable2.getMinimumWidth(), drawable2.getMinimumHeight());
//        iv_back.setImageDrawable(drawable2);
        if (null != mPath) {
            Glide.with(ImgDetailActivity.this).load(mPath)
                    .into(iv_img);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideStatusBar(getWindow().getDecorView());
    }
}
