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

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.*


/**
 * @author Jon (Zhongjun Tian)
 */
class SpecificationBuilder<T> {

    private var repository: JpaSpecificationExecutor<T>? = null
    private var specification: SpecificationImpl? = null

    private fun distinct(): SpecificationBuilder<*> {
        specification!!.add(Specification { _, query, _ -> query?.distinct(true)?.restriction })
        return this
    }

    fun where(queryString: String): SpecificationBuilder<T> {
        val filter = Filter.parse(queryString)
        return where(filter!!)
    }

    fun where(filter: Filter): SpecificationBuilder<T> {
        if (this.repository == null) {
            throw IllegalStateException("Did not specify which repository, please use from() before where()")
        }
        specification!!.add(WhereSpecification(filter))
        return this
    }

    fun where(field: String, operator: String, value: Any): SpecificationBuilder<T> {
        val filter = Filter(field, operator, value)
        return where(filter)
    }

    fun leftJoin(vararg tables: String): SpecificationBuilder<T> {
        specification!!.add(JoinSpecification().setLeftJoinFetchTables(Arrays.asList(*tables)))
        return this
    }

    fun innerJoin(vararg tables: String): SpecificationBuilder<T> {
        specification!!.add(JoinSpecification().setInnerJoinFetchTables(Arrays.asList(*tables)))
        return this
    }

    fun rightJoin(vararg tables: String): SpecificationBuilder<T> {
        specification!!.add(JoinSpecification().setRightJoinFetchTables(Arrays.asList(*tables)))
        return this
    }

    fun findAll(): List<T> {
        return repository!!.findAll(specification as Specification<T>)
    }

    fun findPage(page: Pageable): Page<T> {
        return repository!!.findAll(specification as Specification<T>, page)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SpecificationBuilder::class.java)

        fun selectFrom(repository: JpaSpecificationExecutor<Any>): SpecificationBuilder<Any> {
            val builder = SpecificationBuilder<Any>()
            builder.repository = repository
            builder.specification = SpecificationImpl()
            return builder
        }

        fun selectDistinctFrom(repository: JpaSpecificationExecutor<Any>): SpecificationBuilder<Any> {
            val builder = selectFrom(repository)
            builder.distinct()
            return builder
        }
    }
}
