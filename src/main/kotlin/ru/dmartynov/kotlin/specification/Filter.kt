/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.dmartynov.kotlin.specification


import ru.dmartynov.kotlin.specification.Logic.Companion.AND
import ru.dmartynov.kotlin.specification.Logic.Companion.DELIMITER
import ru.dmartynov.kotlin.specification.Logic.Companion.OR
import ru.dmartynov.kotlin.specification.exception.SpecificationException
import java.util.*
import java.util.stream.Collectors

/**
 * @author Jon (Zhongjun Tian)
 */
class Filter {

    constructor(field: String, operator: String, value: Any) {
        this.field = field
        this.operator = operator
        this.value = value
    }

    constructor(logic: String, vararg filters: Filter) {
        this.logic = logic
        this.filters = Arrays.asList(*filters)
    }

    var field: String? = null
    var operator: String? = null
    var value: Any? = null
    var logic: String? = null
    var filters: List<Filter>? = null

    fun and(filters: Filter): Filter {
        return Filter(AND, filters)
    }

    fun or(filters: Filter): Filter {
        return Filter(OR, filters)
    }

    override fun toString(): String {
        return toSting(false)
    }

    private fun toSting(withBrackets: Boolean): String {
        if (logic == null) {
            //xxx~eq~yyy
            return field + DELIMITER + operator + DELIMITER + value
        } else if (filters != null && !filters!!.isEmpty()) {
            //(xxx~eq~yyy~and~aaa~eq~bbb)
            val join = filters!!.stream().map { f -> f.toSting(true) }.collect(Collectors.toList()).joinToString(DELIMITER + logic + DELIMITER)
            return if (withBrackets)
                LEFT_BRACKET + join + RIGHT_BRACKET
            else
                join
        } else {
            return ""
        }
    }

    companion object {

        //delimiter for crossing table search
        val PATH_DELIMITER = "."
        val LEFT_BRACKET = "("
        val RIGHT_BRACKET = ")"

        fun and(vararg filters: Filter): Filter {
            return Filter(AND, *filters)
        }

        fun or(vararg filters: Filter): Filter {
            return Filter(OR, *filters)
        }

        //(a~eq~a~and~(b~eq~b~or~b~eq~bb))
        fun parse(queryString: String?): Filter? {
            if (queryString == null || queryString.isEmpty())
                return null
            val params = queryString.split(DELIMITER.toRegex()).toTypedArray()
            return parse(params, 0, params.size - 1)
        }

        private fun parse(params: Array<String>, s: Int, e: Int): Filter {
            val n = e - s + 1
            if (n % 4 != 3)
                throw SpecificationException("illegal" + Arrays.asList(*params).subList(s, e + 1).joinToString(DELIMITER))
            if (params[s].startsWith(LEFT_BRACKET) && params[e].endsWith(RIGHT_BRACKET)) {
                params[s] = params[s].substring(1, params[s].length)
                params[e] = params[e].substring(0, params[e].length - 1)
            }
            val filters = LinkedList<Filter>()
            var logic: String? = null
            var i = s
            while (i + 2 <= e) {
                if (params[i].startsWith(LEFT_BRACKET)) {
                    val j = findRightBracket(params, i, e)
                    val filter = parse(params, i, j)
                    filters.add(filter)
                    if (logic == null && j + 1 <= e) {
                        logic = params[j + 1]
                    }
                    i = j + 2
                } else {
                    if (logic == null && i + 3 <= e) {
                        logic = params[i + 3]
                    }
                    if (i + 3 <= e)
                        if (params[i + 3] != logic || params[i + 3] != AND && params[i + 3] != OR)
                            throw SpecificationException("Illegal logic or mixed logic in one level bracket")
                    val filter = Filter(params[i], params[i + 1], params[i + 2])
                    filters.add(filter)
                    i += 4
                }
            }
            return when (logic) {
                null -> filters[0]
                OR -> or(*filters.toTypedArray())
                else -> and(*filters.toTypedArray())
            }
        }

        private fun findRightBracket(params: Array<String>, i: Int, e: Int): Int {
            var countLeft = 0
            var countRight = 0
            var j = i
            while (j < e) {
                if (params[j].startsWith(LEFT_BRACKET)) {
                    (0 until params[j].length)
                            .takeWhile { params[j].substring(it, it + 1) == LEFT_BRACKET }
                            .forEach { countLeft++ }
                }
                if (params[j + 2].endsWith(RIGHT_BRACKET)) {
                    (params[j + 2].length - 1 downTo 0)
                            .map { params[j + 2].substring(it, it + 1) }
                            .takeWhile { it == RIGHT_BRACKET }
                            .forEach { countRight++ }
                }
                if (countLeft == countRight) {
                    return j + 2
                }
                j += 4
            }
            throw SpecificationException("")//TODO
        }
    }
}
