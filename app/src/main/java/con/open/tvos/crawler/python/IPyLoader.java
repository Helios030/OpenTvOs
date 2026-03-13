package con.open.tvos.crawler.python;

import con.open.tvos.crawler.Spider;

import java.util.Map;

public interface IPyLoader {
    void clear();
    void setConfig(String jsonStr);
    void setRecentPyKey(String pyApi);
    Spider getSpider(String key, String cls, String ext);
    Object[] proxyInvoke(Map<String, String> params);
}