package ru.dmartynov.kotlin.specification

class Operator {
    companion object {
        const val EQUAL = "eq"
        const val NOT_EQUAL = "neq"
        const val IS_NULL = "isnull"
        const val IS_NOT_NULL = "isnotnull"
        const val IS_EMPTY = "isempty"
        const val IS_NOT_EMPTY = "isnotempty"
        const val CONTAINS = "contains"
        const val NOT_CONTAINS = "doesnotcontain"
        const val START_WITH = "startswith"
        const val END_WITH = "endswith"
        const val GREATER_THAN = "gt"
        const val LESS_THAN = "lt"
        const val GREATER_THAN_OR_EQUAL = "gte"
        const val LESS_THAN_OR_EQUAL = "lte"
        const val IN = "in"
    }
}