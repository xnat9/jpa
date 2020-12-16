package cn.xnatural.jpa;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页数据结构
 * @param <E>
 */
public class Page<E> {
    // 当前页码: 从1开始
    private Integer       page;
    // 一页数据大小
    private Integer       pageSize;
    // 总条数
    private Long          totalRow;
    // 总页数
    private Integer       totalPage;
    // 当前页数据
    private Collection<E> list;


    public static Page empty() {
        return new Page().setPage(1).setPageSize(10).setTotalRow(0L).setList(Collections.emptyList());
    }


    /**
     * 转换
     * @param fn
     * @return
     */
    public static <T, E> Page<T> of(Page<E> p1, Function<E, T> fn) {
        return new Page<T>().setPage(p1.page).setPageSize(p1.pageSize).setTotalRow(p1.totalRow).setList(p1.list.stream().map(e -> fn.apply(e)).collect(Collectors.toList()));
    }


    public Page setTotalRow(Long totalRow) {
        this.totalRow = totalRow;
        if (totalRow != null) {
            this.totalPage = (int) (Math.ceil(totalRow / Double.valueOf(this.pageSize)));
        }
        return this;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public Page<E> setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public Long getTotalRow() {
        return totalRow;
    }

    public Integer getTotalPage() {
        return totalPage;
    }

    public Page<E> setTotalPage(Integer totalPage) {
        this.totalPage = totalPage;
        return this;
    }

    public Collection<E> getList() {
        return list;
    }

    public Page<E> setList(Collection<E> list) {
        this.list = list;
        return this;
    }

    public Integer getPage() {
        return page;
    }

    public Page<E> setPage(Integer page) {
        this.page = page;
        return this;
    }

    @Override
    public String toString() {
        return "Page@" + Integer.toHexString(hashCode()) + "{page=" + page + ", pageSize=" + pageSize + ", totalRow=" + totalRow + ", totalPage=" + totalPage + ", list=" + list + '}';
    }
}
