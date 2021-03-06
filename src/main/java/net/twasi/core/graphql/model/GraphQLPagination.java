package net.twasi.core.graphql.model;

import net.twasi.core.services.providers.config.ConfigService;

import java.util.List;
import java.util.stream.Stream;

public class GraphQLPagination<T> {

    private static long itemsPerPage = ConfigService.get().getCatalog().webinterface.paginationMax;
    private ICount countFunction;
    private IResolve<T> resolveFunction;

    private boolean counted = false;
    private long amount = 0;

    public GraphQLPagination(ICount countFunction, IResolve<T> resolveFunction) {
        this.countFunction = countFunction;
        this.resolveFunction = resolveFunction;
    }

    public final long getPages() {
        long total = getTotal();
        return total / itemsPerPage + ((total % itemsPerPage) == 0 ? 0 : 1);
    }

    public final long getTotal() {
        // Prevent counting twice
        if (counted) return amount;
        counted = true;
        return amount = countFunction.countFunction();
    }

    public static long getItemsPerPage() {
        return itemsPerPage;
    }

    public final List<T> getContent(int page) {
        return resolveFunction.resolveFunction(page);
    }

    public interface ICount {
        long countFunction();
    }

    public interface IResolve<T> {
        List<T> resolveFunction(int page);
    }

    public static <U> Stream<U> paginateStream(Stream<U> stream, int page) {
        return stream
                .skip(((page > 0 ? page : 1) - 1) * itemsPerPage)
                .limit(itemsPerPage);
    }

}
