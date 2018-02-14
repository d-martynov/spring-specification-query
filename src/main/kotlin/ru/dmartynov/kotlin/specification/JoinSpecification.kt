package ru.dmartynov.kotlin.specification

import org.springframework.data.jpa.domain.Specification

import javax.persistence.criteria.*

/**
 * Created by zhongjun on 6/18/17.
 */
class JoinSpecification : Specification<Any> {
    internal var leftJoinFetchTables: List<String>? = null
    internal var innnerJoinFetchTables: List<String>? = null
    internal var rightJoinFetchTables: List<String>? = null

    override fun toPredicate(root: Root<Any>, cq: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate? {
        //because this piece of code may be run twice for pagination,
        //first time 'count' , second time 'select',
        //So, if this is called by 'count', don't join fetch tables.
        if (isCountCriteriaQuery(cq))
            return null

        join(root, leftJoinFetchTables, JoinType.LEFT)
        join(root, innnerJoinFetchTables, JoinType.INNER)
        join(root, rightJoinFetchTables, JoinType.RIGHT)
        (cq as CriteriaQuery<Any>).select(root)
        return null
    }

    /*
        For Issue:
        when run repository.findAll(specs,page)
        The method toPredicate(...) upon will return a Predicate for Count(TableName) number of rows.
        In hibernate query, we cannot do "select count(table_1) from table_1 left fetch join table_2 where ..."
        Resolution:
        In this scenario, CriteriaQuery<?> is CriteriaQuery<Long>, because return type is Long.
        we don't fetch other tables where generating query for "count";
     */
    private fun isCountCriteriaQuery(cq: CriteriaQuery<*>): Boolean {
        return cq.resultType.toString().contains("java.lang.Long")
    }


    private fun join(root: Root<Any>, joinFetchTables: List<String>?, type: JoinType) {
        if (joinFetchTables != null && joinFetchTables.isNotEmpty()) {
            joinFetchTables.forEach { root.fetch<Any, Any>(it, type) }
        }
    }

    fun setLeftJoinFetchTables(leftJoinFetchTables: List<String>): JoinSpecification {
        this.leftJoinFetchTables = leftJoinFetchTables
        return this
    }

    fun setInnerJoinFetchTables(innerJoinFetchTables: List<String>): JoinSpecification {
        this.innnerJoinFetchTables = innerJoinFetchTables
        return this
    }

    fun setRightJoinFetchTables(rightJoinFetchTables: List<String>): JoinSpecification {
        this.rightJoinFetchTables = rightJoinFetchTables
        return this
    }
}
