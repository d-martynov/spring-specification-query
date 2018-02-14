package ru.dmartynov.kotlin.specification

import org.slf4j.LoggerFactory
import org.springframework.data.jpa.domain.Specification
import ru.dmartynov.kotlin.specification.Filter.Companion.PATH_DELIMITER
import ru.dmartynov.kotlin.specification.Logic.Companion.AND
import ru.dmartynov.kotlin.specification.Logic.Companion.OR
import ru.dmartynov.kotlin.specification.Operator.Companion.CONTAINS
import ru.dmartynov.kotlin.specification.Operator.Companion.END_WITH
import ru.dmartynov.kotlin.specification.Operator.Companion.EQUAL
import ru.dmartynov.kotlin.specification.Operator.Companion.GREATER_THAN
import ru.dmartynov.kotlin.specification.Operator.Companion.GREATER_THAN_OR_EQUAL
import ru.dmartynov.kotlin.specification.Operator.Companion.IN
import ru.dmartynov.kotlin.specification.Operator.Companion.IS_EMPTY
import ru.dmartynov.kotlin.specification.Operator.Companion.IS_NOT_EMPTY
import ru.dmartynov.kotlin.specification.Operator.Companion.IS_NOT_NULL
import ru.dmartynov.kotlin.specification.Operator.Companion.IS_NULL
import ru.dmartynov.kotlin.specification.Operator.Companion.LESS_THAN
import ru.dmartynov.kotlin.specification.Operator.Companion.LESS_THAN_OR_EQUAL
import ru.dmartynov.kotlin.specification.Operator.Companion.NOT_CONTAINS
import ru.dmartynov.kotlin.specification.Operator.Companion.NOT_EQUAL
import ru.dmartynov.kotlin.specification.Operator.Companion.START_WITH
import ru.dmartynov.kotlin.specification.exception.SpecificationException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.persistence.criteria.*


class WhereSpecification(private val filter: Filter) : Specification<Any> {
    private var dateFormat: SimpleDateFormat? = null

    @Throws(SpecificationException::class)
    override fun toPredicate(root: Root<Any>, cq: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate? {
        return getPredicate(filter, root, cb)
    }

    @Throws(SpecificationException::class)
    private fun getPredicate(filter: Filter, root: Path<Any>, cb: CriteriaBuilder): Predicate? {
        if (isInValidFilter(filter))
            return null
        return if (filter.logic == null) {//one filter
            doGetPredicate(filter, root, cb)
        } else {//logic filters
            when {
                filter.logic == AND -> {
                    val predicates = getPredicateList(filter, root, cb)
                    cb.and(*predicates)
                }
                filter.logic == OR -> {
                    val predicates = getPredicateList(filter, root, cb)
                    cb.or(*predicates)
                }
                else -> throw SpecificationException("Unknown filter logic" + filter.logic!!)
            }
        }
    }

    @Throws(SpecificationException::class)
    private fun getPredicateList(filter: Filter, root: Path<Any>, cb: CriteriaBuilder): Array<Predicate> {
        val predicateList = filter.filters!!.mapNotNullTo(LinkedList()) { getPredicate(it, root, cb) }
        return predicateList.toTypedArray()
    }


    private fun isInValidFilter(filter: Filter?): Boolean {
        return filter == null || filter.field == null && filter.filters == null && filter.logic == null && filter.value == null && filter.operator == null
    }

    @Throws(SpecificationException::class)
    private fun doGetPredicate(filter: Filter, root: Path<Any>, cb: CriteriaBuilder): Predicate? {
        val field = filter.field
        val path: Path<Any>?
        try {
            path = parsePath(root, field!!)
        } catch (e: Exception) {
            throw SpecificationException("Meet problem when parse field path: " + field + ", this path does not exist. " + e.message, e)
        }

        val operator = filter.operator
        val value = filter.value
        try {
            return doGetPredicate(cb, path, operator, value)
        } catch (e: Exception) {
            throw SpecificationException("Unable to filter by: " + filter.toString() + ", value type:" + value!!.javaClass + ", operator: " + operator + ", entity type:" + path.javaType + ", message: " + e.message, e)
        }

    }

    @Throws(SpecificationException::class)
    private fun doGetPredicate(cb: CriteriaBuilder, path: Path<Any>, operator: String?, value: Any?): Predicate? {
        var value = value
        val entityType = path.javaType
        var p: Predicate? = null
        //look at Hibernate Mapping types
        //we only support primitive types and data/time types
        if (value !is Comparable<*>) {
            throw IllegalStateException("This library only support primitive types and date/time types in the list: " +
                    "Integer, Long, Double, Float, Short, BidDecimal, Character, String, Byte, Boolean" +
                    ", Date, Time, TimeStamp, Calendar")
        }
        when (operator) {
        /*
                Operator for Comparable type
             */
            EQUAL -> {
                value = parseValue(path, value)
                p = cb.equal(path, value)
            }
            NOT_EQUAL -> {
                value = parseValue(path, value)
                p = cb.notEqual(path, value)
            }
        /*
                Operator for any type
             */
            IS_NULL -> p = cb.isNull(path)
            IS_NOT_NULL -> p = cb.isNotNull(path)
        /*
                Operator for String type
             */
            IS_EMPTY -> p = cb.equal(path, "")
            IS_NOT_EMPTY -> p = cb.notEqual(path, "")
            CONTAINS -> p = cb.like(path.`as`(String::class.java), "%" + value.toString() + "%")
            NOT_CONTAINS -> p = cb.notLike(path.`as`(String::class.java), "%" + value.toString() + "%")
            START_WITH -> p = cb.like(path.`as`(String::class.java), value.toString() + "%")
            END_WITH -> p = cb.like(path.`as`(String::class.java), "%" + value.toString())
        /*
                Operator for Comparable type;
                does not support Calendar
             */
            GREATER_THAN -> {
                value = parseValue(path, value)
                p = if (value is Date) {
                    cb.greaterThan(path.`as`(Date::class.java), value)
                } else {
                    cb.greaterThan(path.`as`(String::class.java), value.toString())
                }
            }
            GREATER_THAN_OR_EQUAL -> {
                value = parseValue(path, value)
                p = if (value is Date) {
                    cb.greaterThanOrEqualTo(path.`as`(Date::class.java), value)
                } else {
                    cb.greaterThanOrEqualTo(path.`as`(String::class.java), value.toString())
                }
            }
            LESS_THAN -> {
                value = parseValue(path, value)
                p = if (value is Date) {
                    cb.lessThan(path.`as`(Date::class.java), value)
                } else {
                    cb.lessThan(path.`as`(String::class.java), value.toString())
                }
            }
            LESS_THAN_OR_EQUAL -> {
                value = parseValue(path, value)
                if (value is Date) {
                    p = cb.lessThanOrEqualTo(path.`as`(Date::class.java), value)
                } else {
                    p = cb.lessThanOrEqualTo(path.`as`(String::class.java), value.toString())
                }
            }
        /*
                Functionality in experimenting;
             */
            IN -> if (assertCollection(value)) {
                p = path.`in`(value as Collection<*>?)
            }
            else -> {
                logger.error("unknown operator: " + operator!!)
                throw IllegalStateException("unknown operator: " + operator)
            }
        }
        return p
    }

    private fun parseValue(path: Path<Any>, value: Any): Any {
        if (Date::class.java.isAssignableFrom(path.javaType)) {
            try {
                val dateFormat = if (this.dateFormat != null) this.dateFormat else defaultDateFormat
                return dateFormat!!.parse(value.toString())
            } catch (e: ParseException) {
                throw SpecificationException("Illegal date format: " + value + ", required format is " + dateFormat!!.toPattern())
            }

        }
        return value
    }

    private fun assertCollection(value: Any): Boolean {
        if (value is Collection<*>) {
            return true
        }
        throw IllegalStateException("After operator $IN should be a list, not '$value'")
    }

    private fun parsePath(root: Path<out Any>, field: String): Path<Any> {
        if (!field.contains(PATH_DELIMITER)) {
            return root.get(field)
        }
        val i = field.indexOf(PATH_DELIMITER)
        val firstPart = field.substring(0, i)
        val secondPart = field.substring(i + 1, field.length)
        val p = root.get<Any>(firstPart)
        return parsePath(p, secondPart)
    }

    fun setDateFormat(dateFormat: SimpleDateFormat) {
        this.dateFormat = dateFormat
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WhereSpecification::class.java)
        private var defaultDateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")

        /*fun setDefaultDateFormat(defaultDateFormat: SimpleDateFormat) {
            WhereSpecification.defaultDateFormat = defaultDateFormat
        }*/
    }
}
