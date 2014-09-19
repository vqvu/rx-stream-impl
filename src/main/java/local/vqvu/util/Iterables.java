package local.vqvu.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Iterables {
    private Iterables() {}

    public static <T> List<T> asList(Iterable<? extends T> iterable) {
        return asList(iterable.iterator());
    }

    public static <T> List<T> asList(Iterator<? extends T> iterator) {
        List<T> ret = new ArrayList<>();
        while (iterator.hasNext()) {
            ret.add(iterator.next());
        }
        return ret;
    }
}
