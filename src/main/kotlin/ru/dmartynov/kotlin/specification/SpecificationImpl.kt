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
import org.springframework.data.jpa.domain.Specification
import java.util.*
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

/**
 * @author Jon (Zhongjun Tian)
 */
class SpecificationImpl : Specification<Any> {
    private val specifications = LinkedList<Specification<Any>>()


    /*
     this method is called by
     SimpleJpaRepository.applySpecificationToCriteria(Specification<T> spec, CriteriaQuery<S> query)
     https://github.com/spring-projects/spring-data-jpa/blob/master/src/main/java/org/springframework/data/jpa/repository/support/SimpleJpaRepository.java
     */
    override fun toPredicate(root: Root<Any>, cq: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate {
        val predicates = specifications.mapNotNull { it.toPredicate(root, cq, cb) }
        return cb.and(*predicates.toTypedArray())
    }

    fun add(specification: Specification<Any>) {
        specifications.add(specification)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SpecificationImpl::class.java)
    }

}

