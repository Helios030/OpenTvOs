package con.open.tvos.ui.adapter;

import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import con.open.tvos.R;
import con.open.tvos.api.ApiConfig;
import con.open.tvos.bean.SourceBean;
import con.open.tvos.cache.VodCollect;
import con.open.tvos.picasso.RoundTransformation;
import con.open.tvos.util.DefaultConfig;
import con.open.tvos.util.HawkConfig;
import con.open.tvos.util.MD5;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class CollectAdapter extends BaseQuickAdapter<VodCollect, BaseViewHolder> {
    public CollectAdapter() {
        super(R.layout.item_grid, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, VodCollect item) {
    	// takagen99: Add Delete Mode
        FrameLayout tvDel = helper.getView(R.id.delFrameLayout);
        if (HawkConfig.hotVodDelete) {
            tvDel.setVisibility(View.VISIBLE);
        } else {
            tvDel.setVisibility(View.GONE);
        }
        
        helper.setVisible(R.id.tvLang, false);
        helper.setVisible(R.id.tvArea, false);
        helper.setVisible(R.id.tvNote, false);
        helper.setText(R.id.tvName, item.name);
        TextView tvYear = helper.getView(R.id.tvYear);
        SourceBean source = ApiConfig.get().getSource(item.sourceKey);
        tvYear.setText(source!=null?source.getName():"");
        
        ImageView ivThumb = helper.getView(R.id.ivThumb);
        //由于部分电视机使用glide报错
        if (!TextUtils.isEmpty(item.pic)) {
            Picasso.get()
                    .load(DefaultConfig.checkReplaceProxy(item.pic))
                    .transform(new RoundTransformation(MD5.string2MD5(item.pic))
                            .centerCorp(true)
                            .override(AutoSizeUtils.mm2px(mContext, 240), AutoSizeUtils.mm2px(mContext, 336))
                            .roundRadius(AutoSizeUtils.mm2px(mContext, 10), RoundTransformation.RoundType.ALL))
                    .placeholder(R.drawable.img_loading_placeholder)
                    .noFade()
                    .error(R.drawable.img_loading_placeholder)
                    .into(ivThumb);
        } else {
            ivThumb.setImageResource(R.drawable.img_loading_placeholder);
        }
    }
}