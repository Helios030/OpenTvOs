package con.open.tvos.bean

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamConverter
import com.thoughtworks.xstream.annotations.XStreamImplicit
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter
import java.io.Serializable

/**
 * @author pj567
 * @date :2020/12/18
 * @description:
 */
@XStreamAlias("class")
class MovieSort : Serializable {
    @XStreamImplicit(itemFieldName = "ty")
    var sortList: List<SortData>? = null

    @XStreamAlias("ty")
    @XStreamConverter(value = ToAttributedValueConverter::class, strings = ["name"])
    class SortData : Serializable, Comparable<SortData> {
        @XStreamAsAttribute
        var id: String? = null
        var name: String? = null
        var sort: Int = -1
        var select: Boolean = false
        var filters: ArrayList<SortFilter> = ArrayList()
        var filterSelect: HashMap<String, String> = HashMap()
        var flag: String? = null

        constructor()
        constructor(id: String?, name: String?) {
            this.id = id
            this.name = name
        }

        fun filterSelectCount(): Int {
            if (filterSelect == null) {
                return 0
            }
            var count = 0
            for (filter in filterSelect.values) {
                if (filter != null && filter.isNotEmpty()) {
                    count++
                }
            }
            return count
        }

        override fun compareTo(other: SortData): Int {
            return this.sort - other.sort
        }

        override fun toString(): String {
            return "SortData{id='$id', name='$name', sort=$sort, select=$select, filters=$filters, filterSelect=$filterSelect, flag='$flag'}"
        }
    }

    class SortFilter {
        var key: String? = null
        var name: String? = null
        var values: LinkedHashMap<String, String>? = null

        override fun toString(): String {
            return "SortFilter{key='$key', name='$name', values=$values}"
        }
    }
}
