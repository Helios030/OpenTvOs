package con.open.tvos.ui.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import con.open.tvos.R;
import con.open.tvos.bean.Subtitle;

import java.util.ArrayList;

public class SearchSubtitleAdapter extends BaseQuickAdapter<Subtitle, BaseViewHolder> {

    public SearchSubtitleAdapter() {
        super(R.layout.item_search_subtitle_result, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, Subtitle item) {
        helper.setText(R.id.subtitleName, item.getName());
        helper.setText(R.id.subtitleNameInfo, item.getIsZip() ? "压缩包" : "文件");
    }
}
